package com.dolgantsev.ifboredthanthis

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.dolgantsev.ifboredthanthis.data.ActivityDao
import com.dolgantsev.ifboredthanthis.data.ActivityEntity
import com.dolgantsev.ifboredthanthis.data.AppDatabase
import com.dolgantsev.ifboredthanthis.model.ActivityResponse
import com.dolgantsev.ifboredthanthis.network.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// ViewModel для управления состоянием приложения
class MainViewModel(application: Application) : AndroidViewModel(application) {
    // Состояние выбранных категорий
    private val _selectedCategories = mutableStateListOf<Int>()
    val selectedCategories: List<Int> = _selectedCategories

    // Состояние фильтров
    private val _participants = mutableStateOf<Int?>(null)
    val participants: State<Int?> = _participants
    private val _price = mutableStateOf("any")
    val price: State<String> = _price
    private val _accessibility = mutableFloatStateOf(0f)
    val accessibility: State<Float> = _accessibility
    private val _minMaxPrice = mutableStateOf(0f to 1f)
    val minMaxPrice: State<Pair<Float, Float>> = _minMaxPrice
    private val _minMaxAccessibility = mutableStateOf(0f to 1f)
    val minMaxAccessibility: State<Pair<Float, Float>> = _minMaxAccessibility

    // Состояние сгенерированной активности
    private val _activity = mutableStateOf<ActivityResponse?>(null)
    val activity: State<ActivityResponse?> = _activity

    // Состояние ошибки API
    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage

    // Состояние предупреждения для избранного
    private val _favoritesWarning = mutableStateOf("")
    val favoritesWarning: State<String> = _favoritesWarning

    // Состояние сортировки
    private val _historySortCategory = mutableStateOf("")
    val historySortCategory: State<String> = _historySortCategory
    private val _favoritesSortCategory = mutableStateOf("")
    val favoritesSortCategory: State<String> = _favoritesSortCategory

    // DAO для работы с базой данных
    private val activityDao: ActivityDao

    init {
        // Инициализация базы данных
        val database = Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "app_database"
        ).build()
        activityDao = database.activityDao()
    }

    // Маппинг ID строк категорий на типы активности
    private val categoryToType = mapOf(
        R.string.mood_random to null,
        R.string.mood_education to "education",
        R.string.mood_recreational to "recreational",
        R.string.mood_social to "social",
        R.string.mood_diy to "diy",
        R.string.mood_charity to "charity",
        R.string.mood_cooking to "cooking",
        R.string.mood_relaxation to "relaxation",
        R.string.mood_music to "music",
        R.string.mood_busywork to "busywork"
    )

    // Маппинг ID строк на типы для сортировки
    private val stringToType = mapOf(
        R.string.mood_education to "education",
        R.string.mood_recreational to "recreational",
        R.string.mood_social to "social",
        R.string.mood_diy to "diy",
        R.string.mood_charity to "charity",
        R.string.mood_cooking to "cooking",
        R.string.mood_relaxation to "relaxation",
        R.string.mood_music to "music",
        R.string.mood_busywork to "busywork"
    )

    // Загрузка активности с фильтрами
    fun loadActivity(errorLabel: String) {
        viewModelScope.launch {
            try {
                val types = if (_selectedCategories.contains(R.string.mood_random)) {
                    categoryToType.values.filterNotNull()
                } else {
                    _selectedCategories.mapNotNull { categoryToType[it] }
                }
                val type = types.randomOrNull()
                val (minPrice, maxPrice) = if (_price.value == "free") 0f to 0f else _minMaxPrice.value
                val (minAccessibility, maxAccessibility) = if (_accessibility.floatValue == 0f) 0f to 1f else _minMaxAccessibility.value
                val response = RetrofitClient.apiService.getActivityWithFilters(
                    type = type,
                    participants = _participants.value,
                    price = if (_price.value == "any") null else if (_price.value == "free") 0f else 1f,
                    minPrice = if (_price.value == "any") minPrice else null,
                    maxPrice = if (_price.value == "any") maxPrice else null,
                    accessibility = if (_accessibility.floatValue == 0f) null else _accessibility.floatValue,
                    minAccessibility = if (_accessibility.floatValue == 0f) null else minAccessibility,
                    maxAccessibility = if (_accessibility.floatValue == 0f) null else maxAccessibility
                )
                _activity.value = response
                _errorMessage.value = ""
                // Сохранить в историю
                saveToHistory(response)
            } catch (e: Exception) {
                val fallbackError = getApplication<Application>().getString(R.string.error_unknown)
                _errorMessage.value = "$errorLabel: ${e.message ?: fallbackError}"
            }
        }
    }

    // Сохранение активности в историю
    private suspend fun saveToHistory(activity: ActivityResponse) {
        val entity = ActivityEntity(
            activity = activity.activity,
            type = activity.type,
            participants = activity.participants,
            price = activity.price,
            link = activity.link,
            timestamp = System.currentTimeMillis(),
            isFavorite = false
        )
        activityDao.insertActivity(entity)
        // Проверка лимита истории (50 записей)
        val count = activityDao.getHistoryCount()
        if (count > 50) {
            activityDao.deleteOldestHistory(count - 50)
        }
    }

    // Получение списка истории
    fun getHistory(): Flow<List<ActivityEntity>> {
        return if (_historySortCategory.value.isEmpty()) {
            activityDao.getHistoryActivities()
        } else {
            val type = stringToType.entries.find { getApplication<Application>().getString(it.key) == _historySortCategory.value }?.value
            type?.let { activityDao.getHistoryActivitiesByType(it) } ?: activityDao.getHistoryActivities()
        }
    }

    // Получение списка избранного
    fun getFavorites(): Flow<List<ActivityEntity>> {
        return if (_favoritesSortCategory.value.isEmpty()) {
            activityDao.getFavoriteActivities()
        } else {
            val type = stringToType.entries.find { getApplication<Application>().getString(it.key) == _favoritesSortCategory.value }?.value
            type?.let { activityDao.getFavoriteActivitiesByType(it) } ?: activityDao.getFavoriteActivities()
        }
    }

    // Установить категорию сортировки для истории
    fun setHistorySortCategory(category: String) {
        _historySortCategory.value = category
    }

    // Установить категорию сортировки для избранного
    fun setFavoritesSortCategory(category: String) {
        _favoritesSortCategory.value = category
    }

    // Переключение категории
    fun toggleCategory(categoryResId: Int) {
        if (categoryResId == R.string.mood_random) {
            if (_selectedCategories.contains(R.string.mood_random)) {
                _selectedCategories.clear()
            } else {
                _selectedCategories.clear()
                _selectedCategories.addAll(categoryToType.keys)
            }
        } else {
            if (_selectedCategories.contains(categoryResId)) {
                _selectedCategories.remove(categoryResId)
                _selectedCategories.remove(R.string.mood_random)
            } else {
                _selectedCategories.add(categoryResId)
                if (_selectedCategories.containsAll(categoryToType.keys.filter { it != R.string.mood_random })) {
                    _selectedCategories.clear()
                    _selectedCategories.add(R.string.mood_random)
                }
            }
        }
    }

    // Установить фильтр участников
    fun setParticipants(participants: Int?) {
        _participants.value = participants
    }

    // Установить фильтр цены
    fun setPrice(price: String) {
        _price.value = price
    }

    // Установить фильтр доступности
    fun setAccessibility(accessibility: Float) {
        _accessibility.floatValue = accessibility
    }

    // Установить диапазон цены
    fun setMinMaxPrice(min: Float, max: Float) {
        _minMaxPrice.value = min to max
    }

    // Установить диапазон доступности
    fun setMinMaxAccessibility(min: Float, max: Float) {
        _minMaxAccessibility.value = min to max
    }

    // Переключение статуса избранного
    fun toggleFavorite(activity: ActivityResponse) {
        viewModelScope.launch {
            val existing = activityDao.findActivity(
                activity.activity,
                activity.type,
                activity.participants,
                activity.price,
                activity.link
            )
            if (existing != null) {
                activityDao.updateFavoriteStatus(existing.id, !existing.isFavorite)
            } else {
                val entity = ActivityEntity(
                    activity = activity.activity,
                    type = activity.type,
                    participants = activity.participants,
                    price = activity.price,
                    link = activity.link,
                    timestamp = System.currentTimeMillis(),
                    isFavorite = true
                )
                activityDao.insertActivity(entity)
            }
            // Проверка количества избранных
            val count = activityDao.getFavoritesCount()
            if (count > 100) {
                _favoritesWarning.value = getApplication<Application>().getString(R.string.favorites_limit, count)
            } else {
                _favoritesWarning.value = ""
            }
        }
    }

    // Проверка, является ли активность избранной
    suspend fun isFavorite(activity: ActivityResponse): Boolean {
        val existing = activityDao.findActivity(
            activity.activity,
            activity.type,
            activity.participants,
            activity.price,
            activity.link
        )
        return existing?.isFavorite ?: false
    }

    // Очистка предупреждения
    fun clearFavoritesWarning() {
        _favoritesWarning.value = ""
    }

    // Удаление выбранных активностей из истории
    fun deleteHistoryActivities(ids: List<Long>) {
        viewModelScope.launch {
            activityDao.deleteByIds(ids)
        }
    }

    // Удаление всех активностей из истории
    fun deleteAllHistory() {
        viewModelScope.launch {
            activityDao.deleteAllHistory()
        }
    }

    // Удаление выбранных активностей из избранного
    fun deleteFavoriteActivities(ids: List<Long>) {
        viewModelScope.launch {
            activityDao.deleteByIds(ids)
            // Проверка количества избранных после удаления
            val count = activityDao.getFavoritesCount()
            if (count > 100) {
                _favoritesWarning.value = getApplication<Application>().getString(R.string.favorites_limit, count)
            } else {
                _favoritesWarning.value = ""
            }
        }
    }

    // Удаление всех активностей из избранного
    fun deleteAllFavorites() {
        viewModelScope.launch {
            activityDao.deleteAllFavorites()
            _favoritesWarning.value = ""
        }
    }
}
package com.dolgantsev.ifboredthanthis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dolgantsev.ifboredthanthis.model.ActivityResponse
import com.dolgantsev.ifboredthanthis.network.RetrofitClient
import com.dolgantsev.ifboredthanthis.ui.theme.IfBoredThanThisTheme
import kotlinx.coroutines.launch
import java.util.Calendar

// Основной Activity, запускается при старте приложения
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Установка контента через Jetpack Compose
        setContent {
            IfBoredThanThisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppScreen() // Запуск главного интерфейса приложения
                }
            }
        }
    }
}

// Главный экран приложения с навигацией и управлением содержимым
@Composable
fun AppScreen() {
    // Управление текущим экраном (навигация)
    var currentScreen by remember { mutableStateOf("moodSelection") }

    // Состояние выбранной категории активности
    var selectedCategory by remember { mutableStateOf("") }

    // Состояние сгенерированной активности
    var activity by remember { mutableStateOf<ActivityResponse?>(null) }

    // Ошибка при получении данных с API
    var errorMessage by remember { mutableStateOf("") }

    // Коррутина для асинхронных вызовов
    val coroutineScope = rememberCoroutineScope()

    // Локализованные названия для навигации
    val navHome = stringResource(R.string.nav_home)
    val navHistory = stringResource(R.string.nav_history)
    val navFavorites = stringResource(R.string.nav_favorites)
    val errorLabel = stringResource(R.string.error_label)

    // Маппинг категорий на типы активности (null для случайной)
    val categoryToType = mapOf(
        stringResource(R.string.mood_random) to null,
        stringResource(R.string.mood_education) to "education",
        stringResource(R.string.mood_recreational) to "recreational",
        stringResource(R.string.mood_social) to "social",
        stringResource(R.string.mood_diy) to "diy",
        stringResource(R.string.mood_charity) to "charity",
        stringResource(R.string.mood_cooking) to "cooking",
        stringResource(R.string.mood_relaxation) to "relaxation",
        stringResource(R.string.mood_music) to "music",
        stringResource(R.string.mood_busywork) to "busywork"
    )

    // Основная разметка с навигацией снизу
    Scaffold(
        bottomBar = {
            NavigationBar {
                // Кнопка: выбор категории
                NavigationBarItem(
                    selected = currentScreen == "moodSelection",
                    onClick = { currentScreen = "moodSelection" },
                    icon = { Icon(Icons.Default.Home, contentDescription = navHome) },
                    label = { Text(navHome) }
                )
                // Кнопка: история
                NavigationBarItem(
                    selected = currentScreen == "history",
                    onClick = { currentScreen = "history" },
                    icon = { Icon(Icons.Default.History, contentDescription = navHistory) },
                    label = { Text(navHistory) }
                )
                // Кнопка: избранное
                NavigationBarItem(
                    selected = currentScreen == "favorites",
                    onClick = { currentScreen = "favorites" },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = navFavorites) },
                    label = { Text(navFavorites) }
                )
            }
        }
    ) { paddingValues ->
        // Управление отображаемым контентом по текущему экрану
        when (currentScreen) {
            "moodSelection" -> MoodSelectionScreen(
                modifier = Modifier.padding(paddingValues),
                onCategorySelected = { category ->
                    selectedCategory = category
                    coroutineScope.launch {
                        try {
                            val type = categoryToType[category]
                            val response = if (type == null) {
                                RetrofitClient.apiService.getRandomActivity()
                            } else {
                                RetrofitClient.apiService.getActivityByType(type)
                            }
                            activity = response
                            errorMessage = ""
                            currentScreen = "recommendation"
                        } catch (e: Exception) {
                            errorMessage = "$errorLabel: ${e.message ?: R.string.error_unknown}"
                        }
                    }
                }
            )
            "recommendation" -> RecommendationScreen(
                modifier = Modifier.padding(paddingValues),
                activity = activity,
                errorMessage = errorMessage,
                onBack = { currentScreen = "moodSelection" },
                onNewRecommendation = {
                    coroutineScope.launch {
                        try {
                            val type = categoryToType[selectedCategory]
                            val response = if (type == null) {
                                RetrofitClient.apiService.getRandomActivity()
                            } else {
                                RetrofitClient.apiService.getActivityByType(type)
                            }
                            activity = response
                            errorMessage = ""
                        } catch (e: Exception) {
                            errorMessage = "$errorLabel: ${e.message ?: R.string.error_unknown}"
                        }
                    }
                }
            )
            "history" -> PlaceholderScreen(
                modifier = Modifier.padding(paddingValues),
                text = stringResource(R.string.history_placeholder)
            )
            "favorites" -> PlaceholderScreen(
                modifier = Modifier.padding(paddingValues),
                text = stringResource(R.string.favorites_placeholder)
            )
        }
    }
}

// Экран выбора категории активности
@Composable
fun MoodSelectionScreen(
    modifier: Modifier = Modifier,
    onCategorySelected: (String) -> Unit
) {
    // Определение текущего времени
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)

    // Логика выбора приветствия по времени суток
    val greetingId = when (hour) {
        in 0..4 -> R.string.greeting_night
        in 5..11 -> R.string.greeting_morning
        in 12..16 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }

    // Получение строк из ресурсов
    val greeting = stringResource(greetingId)
    val moodQuestion = stringResource(R.string.mood_question)

    // Список категорий для кнопок
    val categories = listOf(
        stringResource(R.string.mood_random),
        stringResource(R.string.mood_education),
        stringResource(R.string.mood_recreational),
        stringResource(R.string.mood_social),
        stringResource(R.string.mood_diy),
        stringResource(R.string.mood_charity),
        stringResource(R.string.mood_cooking),
        stringResource(R.string.mood_relaxation),
        stringResource(R.string.mood_music),
        stringResource(R.string.mood_busywork)
    )

    // Разметка экрана
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = moodQuestion,
            style = MaterialTheme.typography.headlineMedium
        )

        // Сетка с кнопками категорий
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories.size) { index ->
                val category = categories[index]
                // Кнопка категории
                Button(
                    onClick = { onCategorySelected(category) },
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    // Содержимое кнопки: изображение и текст
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(R.drawable.cat),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(80.dp)
                                .padding(bottom = 8.dp)
                        )
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Экран отображения рекомендации
@Composable
fun RecommendationScreen(
    modifier: Modifier = Modifier,
    activity: ActivityResponse?,
    errorMessage: String,
    onBack: () -> Unit,
    onNewRecommendation: () -> Unit
) {
    // Разметка экрана
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.recommendation_title),
            style = MaterialTheme.typography.headlineMedium
        )

        // Отображение ошибки или активности
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        } else if (activity != null) {
            // Карточка с описанием активности
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.activity_label, activity.activity),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.type_label, activity.type),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.participants_label, activity.participants),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (activity.price == 0f) stringResource(R.string.price_free)
                        else stringResource(R.string.price_paid, activity.price),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (activity.link.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.link_label, activity.link),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            CircularProgressIndicator()
        }

        // Кнопки навигации
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onNewRecommendation) {
                Text(stringResource(R.string.new_recommendation))
            }
            Button(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
        }
    }
}

// Заглушка для экранов истории и избранного
@Composable
fun PlaceholderScreen(
    modifier: Modifier = Modifier,
    text: String
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

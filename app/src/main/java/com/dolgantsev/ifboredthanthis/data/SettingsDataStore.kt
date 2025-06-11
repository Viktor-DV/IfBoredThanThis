package com.dolgantsev.ifboredthanthis.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Создаём расширение для доступа к DataStore в контексте
private val Context.dataStore by preferencesDataStore(name = "user_settings")

// Объект, управляющий сохранением и загрузкой пользовательских настроек
object SettingsDataStore {
    // Ключи для каждой настройки
    private val HISTORY_LIMIT = intPreferencesKey("history_limit")
    private val THEME_DARK = booleanPreferencesKey("theme_dark")
    private val WARNINGS_ENABLED = booleanPreferencesKey("warnings_enabled")

    // Чтение настроек как Flow, которое можно подписать в Compose
    fun readSettings(context: Context): Flow<UserSettings> {
        return context.dataStore.data.map { prefs ->
            UserSettings(
                historyLimit = prefs[HISTORY_LIMIT] ?: 50,        // по умолчанию 50 записей
                isDarkTheme = prefs[THEME_DARK] ?: true,          // по умолчанию тёмная тема
                showWarnings = prefs[WARNINGS_ENABLED] ?: true    // по умолчанию включены
            )
        }
    }

    // Сохранение новых настроек в DataStore
    suspend fun saveSettings(
        context: Context,
        historyLimit: Int,
        isDarkTheme: Boolean,
        showWarnings: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[HISTORY_LIMIT] = historyLimit
            prefs[THEME_DARK] = isDarkTheme
            prefs[WARNINGS_ENABLED] = showWarnings
        }
    }

    // Модель для настроек
    data class UserSettings(
        val historyLimit: Int,
        val isDarkTheme: Boolean,
        val showWarnings: Boolean
    )
}

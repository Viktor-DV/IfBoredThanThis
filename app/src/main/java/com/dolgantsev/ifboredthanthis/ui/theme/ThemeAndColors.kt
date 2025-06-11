package com.dolgantsev.ifboredthanthis.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Цвета для тёмной темы
val DarkPrimary = Color(0xFF6C6C6C)
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnBackground = Color(0xFFE0E0E0)
val DarkOnSurface = Color(0xFFE0E0E0)
val DarkSecondary = Color(0xFFAFADB4)

// Цвета для светлой темы
val LightPrimary = Color(0xFF6200EE)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightBackground = Color(0xFFF5F5F5)
val LightSurface = Color(0xFFFFFFFF)
val LightOnBackground = Color(0xFF000000)
val LightOnSurface = Color(0xFF000000)
val LightSecondary = Color(0xFF03DAC6)

// Определение цветовых схем
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    secondary = DarkSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    secondary = LightSecondary
)

// Общая тема приложения с выбором между светлой и тёмной темой
@Composable
fun IfBoredThanThisTheme(
    useDarkTheme: Boolean = true, // По умолчанию тёмная
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}

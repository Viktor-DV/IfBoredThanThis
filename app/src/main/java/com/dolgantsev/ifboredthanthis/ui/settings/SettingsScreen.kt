package com.dolgantsev.ifboredthanthis.ui.settings

import androidx.compose.runtime.mutableIntStateOf
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dolgantsev.ifboredthanthis.R
import com.dolgantsev.ifboredthanthis.data.SettingsDataStore
import kotlinx.coroutines.launch

// Экран настроек приложения
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Подписываемся на сохранённые значения из DataStore
    val settingsFlow = remember { SettingsDataStore.readSettings(context) }
    val settings by settingsFlow.collectAsState(
        initial = SettingsDataStore.UserSettings(
            historyLimit = 50,
            isDarkTheme = true,
            showWarnings = true
        )
    )

    // Локальные состояния для управления UI
    var selectedLimit by remember { mutableIntStateOf(settings.historyLimit) }
    var isDarkTheme by remember { mutableStateOf(settings.isDarkTheme) }
    var warningsEnabled by remember { mutableStateOf(settings.showWarnings) }

    // Интерфейс экрана
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Выбор лимита истории
            Text(
                text = stringResource(R.string.history_limit_label),
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(50, 100, 200).forEach {
                    FilterChip(
                        selected = selectedLimit == it,
                        onClick = { selectedLimit = it },
                        label = { Text("$it") }
                    )
                }
            }

            // Переключатель темы
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.dark_theme_label),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { isDarkTheme = it }
                )
            }

            // Переключатель показа предупреждений
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.show_warnings_label),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = warningsEnabled,
                    onCheckedChange = { warningsEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Кнопка сохранения
            Button(
                onClick = {
                    scope.launch {
                        SettingsDataStore.saveSettings(
                            context = context,
                            historyLimit = selectedLimit,
                            isDarkTheme = isDarkTheme,
                            showWarnings = warningsEnabled
                        )
                        Toast.makeText(context, R.string.save_settings_success, Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                }
            ) {
                Text(stringResource(R.string.save_button))
            }
        }
    }
}
package com.dolgantsev.ifboredthanthis

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.dolgantsev.ifboredthanthis.data.ActivityEntity
import com.dolgantsev.ifboredthanthis.model.ActivityResponse
import com.dolgantsev.ifboredthanthis.ui.theme.IfBoredThanThisTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.Calendar

// Основной Activity приложения, запускается при старте
class MainActivity : ComponentActivity() {
    // Инициализация ViewModel для управления данными
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Установка контента с использованием Jetpack Compose
        setContent {
            IfBoredThanThisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppScreen(viewModel)
                }
            }
        }
    }
}

// Главный экран с навигацией и управлением содержимым
@Composable
fun AppScreen(viewModel: MainViewModel) {
    // Инициализация контроллера навигации
    val navController = rememberNavController()
    // Локализованные названия для элементов навигации
    val navHome: String = stringResource(R.string.nav_home)
    val navHistory: String = stringResource(R.string.nav_history)
    val navFavorites: String = stringResource(R.string.nav_favorites)
    val errorLabel = stringResource(R.string.error_label)

    // Отображение предупреждений для избранного
    val favoritesWarning by viewModel.favoritesWarning
    val context = LocalContext.current
    LaunchedEffect(favoritesWarning) {
        if (favoritesWarning.isNotEmpty()) {
            Toast.makeText(context, favoritesWarning, Toast.LENGTH_LONG).show()
            viewModel.clearFavoritesWarning()
        }
    }

    // Сборка интерфейса с нижней навигационной панелью
    Scaffold(
        bottomBar = {
            NavigationBar {
                // Элемент навигации: Главная
                NavigationBarItem(
                    selected = navController.currentBackStackEntry?.destination?.route == "moodSelection",
                    onClick = { navController.navigate("moodSelection") { popUpTo(navController.graph.startDestinationId) } },
                    icon = { Icon(Icons.Default.Home, contentDescription = navHome) },
                    label = { Text(navHome) }
                )
                // Элемент навигации: История
                NavigationBarItem(
                    selected = navController.currentBackStackEntry?.destination?.route == "history",
                    onClick = { navController.navigate("history") },
                    icon = { Icon(Icons.Default.History, contentDescription = navHistory) },
                    label = { Text(navHistory) }
                )
                // Элемент навигации: Избранное
                NavigationBarItem(
                    selected = navController.currentBackStackEntry?.destination?.route == "favorites",
                    onClick = { navController.navigate("favorites") },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = navFavorites) },
                    label = { Text(navFavorites) }
                )
            }
        }
    ) { paddingValues ->
        // Настройка навигации между экранами
        NavHost(
            navController = navController,
            startDestination = "moodSelection",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("moodSelection") {
                MoodSelectionScreen(viewModel = viewModel, onStart = {
                    viewModel.loadActivity(errorLabel)
                    navController.navigate("recommendation")
                })
            }
            composable("recommendation") {
                RecommendationScreen(
                    activity = viewModel.activity.value,
                    errorMessage = viewModel.errorMessage.value,
                    onBack = { navController.popBackStack() },
                    onNewRecommendation = { viewModel.loadActivity(errorLabel) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    isFavorite = { viewModel.isFavorite(it) }
                )
            }
            composable("history") {
                HistoryScreen(history = viewModel.getHistory(), viewModel = viewModel)
            }
            composable("favorites") {
                FavoritesScreen(favorites = viewModel.getFavorites(), viewModel = viewModel)
            }
        }
    }
}

// Экран выбора категории активности
@Composable
fun MoodSelectionScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    onStart: () -> Unit
) {
    // Получаем текущее время для выбора приветствия
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greetingId = when (hour) {
        in 0..4 -> R.string.greeting_night
        in 5..11 -> R.string.greeting_morning
        in 12..16 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
    val greeting = stringResource(greetingId)

    // Локализованные строки для подписей
    val participantsLabel = stringResource(R.string.filter_participants)
    val priceLabel = stringResource(R.string.filter_price)
    val accessibilityLabel = stringResource(R.string.filter_accessibility)
    val minMaxPriceLabel = stringResource(R.string.filter_min_max_price)
    val minMaxAccessibilityLabel = stringResource(R.string.filter_min_max_accessibility)
    val selectCategories = stringResource(R.string.select_categories)
    val accessibilityInfo = stringResource(R.string.accessibility_info)
    val context = LocalContext.current

    // Категории настроения — ресурсы строк
    val categoryResIds = listOf(
        R.string.mood_random, R.string.mood_education, R.string.mood_recreational,
        R.string.mood_social, R.string.mood_diy, R.string.mood_charity,
        R.string.mood_cooking, R.string.mood_relaxation, R.string.mood_music,
        R.string.mood_busywork
    )

    // Состояния из ViewModel
    val participants by viewModel.participants
    var participantsExpanded by remember { mutableStateOf(false) }
    val price by viewModel.price
    val accessibility by viewModel.accessibility
    val minMaxPrice by viewModel.minMaxPrice
    val minMaxAccessibility by viewModel.minMaxAccessibility
    val selectedCategories by remember { derivedStateOf { viewModel.selectedCategories } }

    // LazyColumn — основной контейнер с прокруткой для всей страницы
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Приветствие
        item {
            Text(text = greeting, style = MaterialTheme.typography.headlineLarge)
        }
        // Блок выбора участников
        item {
            Text(text = participantsLabel, style = MaterialTheme.typography.headlineSmall)
            Box {
                OutlinedTextField(
                    value = participants?.toString() ?: stringResource(R.string.any_participants),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { participantsExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                    }
                )
                DropdownMenu(
                    expanded = participantsExpanded,
                    onDismissRequest = { participantsExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.any_participants)) },
                        onClick = {
                            viewModel.setParticipants(null)
                            participantsExpanded = false
                        }
                    )
                    (1..5).forEach { num ->
                        DropdownMenuItem(
                            text = { Text(num.toString()) },
                            onClick = {
                                viewModel.setParticipants(num)
                                participantsExpanded = false
                            }
                        )
                    }
                }
            }
        }
        // Блок выбора цены (фильтр по цене)
        item {
            Text(text = priceLabel, style = MaterialTheme.typography.headlineSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = price == "any",
                    onClick = { viewModel.setPrice("any") },
                    label = { Text(stringResource(R.string.any)) }
                )
                FilterChip(
                    selected = price == "free",
                    onClick = { viewModel.setPrice("free") },
                    label = { Text(stringResource(R.string.price_free)) }
                )
                FilterChip(
                    selected = price == "paid",
                    onClick = { viewModel.setPrice("paid") },
                    label = { Text(stringResource(R.string.price_paid)) }
                )
            }
        }
        // Слайдер доступности
        item {
            Text(
                text = accessibilityLabel,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.clickable {
                    Toast.makeText(context, accessibilityInfo, Toast.LENGTH_LONG).show()
                }
            )
            Slider(
                value = accessibility,
                onValueChange = { viewModel.setAccessibility(it) },
                valueRange = 0f..1f,
                steps = 10,
                modifier = Modifier.fillMaxWidth()
            )
        }
        // Если выбрана не бесплатная цена, показываем диапазон цен
        if (price != "free") {
            item {
                Text(text = minMaxPriceLabel, style = MaterialTheme.typography.headlineSmall)
                RangeSlider(
                    value = minMaxPrice.first..minMaxPrice.second,
                    onValueChange = { range ->
                        viewModel.setMinMaxPrice(range.start, range.endInclusive)
                    },
                    valueRange = 0f..1f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // Если доступность больше 0, показываем диапазон доступности
        if (accessibility > 0f) {
            item {
                Text(
                    text = minMaxAccessibilityLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.clickable {
                        Toast.makeText(context, accessibilityInfo, Toast.LENGTH_LONG).show()
                    }
                )
                RangeSlider(
                    value = minMaxAccessibility.first..minMaxAccessibility.second,
                    onValueChange = { range ->
                        viewModel.setMinMaxAccessibility(range.start, range.endInclusive)
                    },
                    valueRange = 0f..1f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // Заголовок для выбора категорий
        item {
            Text(text = selectCategories, style = MaterialTheme.typography.headlineSmall)
        }
        // Кнопки категорий: разбиваем на строки по 2 элемента и выводим в Row
        val chunkedCategories = categoryResIds.chunked(2)
        items(chunkedCategories.size) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunkedCategories[rowIndex].forEach { categoryResId ->
                    val isSelected = selectedCategories.contains(categoryResId)
                    Button(
                        onClick = { viewModel.toggleCategory(categoryResId) },
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    ) {
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
                                text = stringResource(categoryResId),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                // Если в ряду меньше 2 кнопок, добавляем пустой Spacer для выравнивания
                if (chunkedCategories[rowIndex].size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        // Кнопка начала
        item {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.start_button))
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
    onNewRecommendation: () -> Unit,
    onToggleFavorite: (ActivityResponse) -> Unit,
    isFavorite: suspend (ActivityResponse) -> Boolean
) {
    // Основная колонка для отображения рекомендации с прокруткой
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.recommendation_title),
            style = MaterialTheme.typography.headlineMedium
        )
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        } else if (activity != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            val favoriteStatus by produceState(initialValue = false, activity) {
                value = isFavorite(activity)
            }
            IconButton(onClick = { onToggleFavorite(activity) }) {
                Icon(
                    imageVector = if (favoriteStatus) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.nav_favorites)
                )
            }
        } else {
            CircularProgressIndicator()
        }
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

// Экран истории
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    history: Flow<List<ActivityEntity>>,
    viewModel: MainViewModel
) {
    // Состояние для списка активностей
    val activities by history.collectAsState(initial = emptyList())
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showSelectDialog by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    val sortCategory by viewModel.historySortCategory
    val context = LocalContext.current
    val categories = listOf(
        stringResource(R.string.all_categories), stringResource(R.string.mood_education),
        stringResource(R.string.mood_recreational), stringResource(R.string.mood_social),
        stringResource(R.string.mood_diy), stringResource(R.string.mood_charity),
        stringResource(R.string.mood_cooking), stringResource(R.string.mood_relaxation),
        stringResource(R.string.mood_music), stringResource(R.string.mood_busywork)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_history)) },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = sortCategory.ifEmpty { stringResource(R.string.all_categories) },
                            onValueChange = {},
                            label = { Text(stringResource(R.string.sort_by_category)) },
                            readOnly = true,
                            modifier = Modifier
                                .width(200.dp)
                                .padding(end = 8.dp),
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        viewModel.setHistorySortCategory(category)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                showMenu = false
                                showSelectDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_all)) },
                            onClick = {
                                showMenu = false
                                showDeleteAllDialog = true
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // Колонка с прокруткой для списка истории
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (activities.isEmpty()) {
                Text(
                    text = stringResource(R.string.history_placeholder),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activities.size) { index ->
                        ActivityCard(activity = activities[index])
                    }
                }
            }
        }
        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text(stringResource(R.string.confirm_delete_all)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteAllHistory()
                        showDeleteAllDialog = false
                        Toast.makeText(context, R.string.delete_success, Toast.LENGTH_SHORT).show()
                    }) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text(stringResource(R.string.back))
                    }
                }
            )
        }
        if (showSelectDialog) {
            AlertDialog(
                onDismissRequest = { showSelectDialog = false },
                title = { Text(stringResource(R.string.select_activities)) },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(activities.size) { index ->
                            val activity = activities[index]
                            val isChecked = selectedIds.contains(activity.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedIds.remove(activity.id)
                                        else selectedIds.add(activity.id)
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (it) selectedIds.add(activity.id)
                                        else selectedIds.remove(activity.id)
                                    }
                                )
                                Text(text = activity.activity, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteHistoryActivities(selectedIds.toList())
                            selectedIds.clear()
                            showSelectDialog = false
                            Toast.makeText(context, R.string.delete_success, Toast.LENGTH_SHORT).show()
                        },
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.delete_selected))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSelectDialog = false }) {
                        Text(stringResource(R.string.back))
                    }
                }
            )
        }
    }
}

// Экран избранного
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    favorites: Flow<List<ActivityEntity>>,
    viewModel: MainViewModel
) {
    // Состояние для списка избранных активностей
    val activities by favorites.collectAsState(initial = emptyList())
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showSelectDialog by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    val sortCategory by viewModel.favoritesSortCategory
    val context = LocalContext.current
    val categories = listOf(
        stringResource(R.string.all_categories), stringResource(R.string.mood_education),
        stringResource(R.string.mood_recreational), stringResource(R.string.mood_social),
        stringResource(R.string.mood_diy), stringResource(R.string.mood_charity),
        stringResource(R.string.mood_cooking), stringResource(R.string.mood_relaxation),
        stringResource(R.string.mood_music), stringResource(R.string.mood_busywork)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_favorites)) },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = sortCategory.ifEmpty { stringResource(R.string.all_categories) },
                            onValueChange = {},
                            label = { Text(stringResource(R.string.sort_by_category)) },
                            readOnly = true,
                            modifier = Modifier
                                .width(200.dp)
                                .padding(end = 8.dp),
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        viewModel.setFavoritesSortCategory(category)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                showMenu = false
                                showSelectDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_all)) },
                            onClick = {
                                showMenu = false
                                showDeleteAllDialog = true
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // Колонка с прокруткой для списка избранного
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (activities.isEmpty()) {
                Text(
                    text = stringResource(R.string.favorites_placeholder),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activities.size) { index ->
                        ActivityCard(activity = activities[index])
                    }
                }
            }
        }
        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text(stringResource(R.string.confirm_delete_all)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteAllFavorites()
                        showDeleteAllDialog = false
                        Toast.makeText(context, R.string.delete_success, Toast.LENGTH_SHORT).show()
                    }) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text(stringResource(R.string.back))
                    }
                }
            )
        }
        if (showSelectDialog) {
            AlertDialog(
                onDismissRequest = { showSelectDialog = false },
                title = { Text(stringResource(R.string.select_activities)) },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(activities.size) { index ->
                            val activity = activities[index]
                            val isChecked = selectedIds.contains(activity.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedIds.remove(activity.id)
                                        else selectedIds.add(activity.id)
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (it) selectedIds.add(activity.id)
                                        else selectedIds.remove(activity.id)
                                    }
                                )
                                Text(text = activity.activity, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteFavoriteActivities(selectedIds.toList())
                            selectedIds.clear()
                            showSelectDialog = false
                            Toast.makeText(context, R.string.delete_success, Toast.LENGTH_SHORT).show()
                        },
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.delete_selected))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSelectDialog = false }) {
                        Text(stringResource(R.string.back))
                    }
                }
            )
        }
    }
}

// Компонент карточки активности
@Composable
fun ActivityCard(activity: ActivityEntity) {
    // Карточка с информацией об активности
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
}

// Предпросмотры
//@Preview(showBackground = true)
//@Composable
//fun MoodSelectionScreenPreview() {
//    IfBoredThanThisTheme {
//        MoodSelectionScreen(viewModel = MainViewModel(ApplicationProvider.getApplicationContext()), onStart = {})
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun RecommendationScreenPreview() {
//    IfBoredThanThisTheme {
//        RecommendationScreen(
//            activity = ActivityResponse("Test Activity", "recreational", 1, 0f, "", "test"),
//            errorMessage = "",
//            onBack = {},
//            onNewRecommendation = {},
//            onToggleFavorite = {},
//            isFavorite = { false }
//        )
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun HistoryScreenPreview() {
//    IfBoredThanThisTheme {
//        HistoryScreen(
//            history = flowOf(listOf(ActivityEntity("Test", "recreational", 1, 0f, "", System.currentTimeMillis(), false))),
//            viewModel = MainViewModel(ApplicationProvider.getApplicationContext())
//        )
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun FavoritesScreenPreview() {
//    IfBoredThanThisTheme {
//        FavoritesScreen(
//            favorites = flowOf(listOf(ActivityEntity("Test", "recreational", 1, 0f, "", System.currentTimeMillis(), true))),
//            viewModel = MainViewModel(ApplicationProvider.getApplicationContext())
//        )
//    }
//}
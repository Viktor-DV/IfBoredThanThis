package com.dolgantsev.ifboredthanthis

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.dolgantsev.ifboredthanthis.data.ActivityEntity
import com.dolgantsev.ifboredthanthis.model.ActivityResponse
import com.dolgantsev.ifboredthanthis.ui.theme.IfBoredThanThisTheme
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

// Компонент экрана загрузки с анимацией
@Composable
fun MySplashScreen(onAnimationEnd: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("start_screen_loading.json"))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = true,
        speed = 1f
    )

    LaunchedEffect(progress) {
        if (progress >= 1f) {
            onAnimationEnd()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(200.dp)
        )
    }
}

// Основной Activity приложения, запускается при старте
class MainActivity : ComponentActivity() {
    // Инициализация ViewModel для управления данными
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var showSplash by mutableStateOf(true)
        // Установка контента
        setContent {
            IfBoredThanThisTheme {
                if (showSplash) {
                    MySplashScreen(onAnimationEnd = { showSplash = false })
                } else {
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
}

// Главный экран с навигацией и управлением содержимым
@Composable
fun AppScreen(viewModel: MainViewModel) {
    // Инициализация контроллера навигации
    val navController = rememberNavController()
    // Локализованные названия для элементов навигации
    val navHome = stringResource(R.string.nav_home)
    val navHistory = stringResource(R.string.nav_history)
    val navFavorites = stringResource(R.string.nav_favorites)
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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
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

// Экран выбора категории активности с анимацией и раскрывающимися фильтрами
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

    // Локализованные строки
    val participantsLabel = stringResource(R.string.filter_participants)
    val priceLabel = stringResource(R.string.filter_price)
    val accessibilityLabel = stringResource(R.string.filter_accessibility)
    val minMaxPriceLabel = stringResource(R.string.filter_min_max_price)
    val minMaxAccessibilityLabel = stringResource(R.string.filter_min_max_accessibility)
    val selectCategories = stringResource(R.string.select_categories)
    val accessibilityInfo = stringResource(R.string.accessibility_info)
    val filtersTitle = stringResource(R.string.filters)
    val context = LocalContext.current

    // Состояния из ViewModel
    val participants by viewModel.participants
    var participantsExpanded by remember { mutableStateOf(false) }
    val price by viewModel.price
    val accessibility by viewModel.accessibility
    val minMaxPrice by viewModel.minMaxPrice
    val minMaxAccessibility by viewModel.minMaxAccessibility

    val categoryResIds = listOf(
        R.string.mood_random, R.string.mood_education, R.string.mood_recreational,
        R.string.mood_social, R.string.mood_diy, R.string.mood_charity,
        R.string.mood_cooking, R.string.mood_relaxation, R.string.mood_music,
        R.string.mood_busywork
    )

    var filtersExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Приветствие
        item {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Заголовок категорий
        item {
            Text(
                text = selectCategories,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Кнопки категорий с иконками
        item {
            CategoryGrid(
                selected = viewModel.selectedCategories,
                onSelect = { viewModel.toggleCategory(it) }
            )
        }

        // Заголовок "Нюансы" с кнопкой раскрытия
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { filtersExpanded = !filtersExpanded },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = filtersTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = if (filtersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Анимация фильтров
        item {
            AnimatedVisibility(
                visible = filtersExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Выбор количества участников
                    Text(text = participantsLabel, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    Box {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { participantsExpanded = true }
                        ) {
                            OutlinedTextField(
                                value = participants?.toString() ?: stringResource(R.string.any_participants),
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.secondary
                                ),
                                trailingIcon = {
                                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                }
                            )
                        }
                        DropdownMenu(
                            expanded = participantsExpanded,
                            onDismissRequest = { participantsExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.any_participants), color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    viewModel.setParticipants(null)
                                    participantsExpanded = false
                                }
                            )
                            (1..5).forEach { num ->
                                DropdownMenuItem(
                                    text = { Text(num.toString(), color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        viewModel.setParticipants(num)
                                        participantsExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Выбор цены
                    Text(text = priceLabel, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = price == "any",
                            onClick = { viewModel.setPrice("any") },
                            label = { Text(stringResource(R.string.any)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                selectedContainerColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        FilterChip(
                            selected = price == "free",
                            onClick = { viewModel.setPrice("free") },
                            label = { Text(stringResource(R.string.price_free)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                selectedContainerColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        FilterChip(
                            selected = price == "paid",
                            onClick = { viewModel.setPrice("paid") },
                            label = { Text(stringResource(R.string.price_paid_label)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                selectedContainerColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Доступность
                    Text(
                        text = accessibilityLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.clickable {
                            Toast.makeText(context, accessibilityInfo, Toast.LENGTH_LONG).show()
                        }
                    )
                    Slider(
                        value = accessibility,
                        onValueChange = { viewModel.setAccessibility(it) },
                        valueRange = 0f..1f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Диапазон цены, если выбрано "платно"
                    if (price != "free") {
                        Text(text = minMaxPriceLabel, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                        RangeSlider(
                            value = minMaxPrice.first..minMaxPrice.second,
                            onValueChange = { range ->
                                viewModel.setMinMaxPrice(range.start, range.endInclusive)
                            },
                            valueRange = 0f..1f,
                            steps = 10,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Диапазон доступности
                    if (accessibility > 0f) {
                        Text(
                            text = minMaxAccessibilityLabel,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
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
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Кнопка "Начать"
        item {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.start_button), color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

// Компонент сетки категорий
@Composable
fun CategoryGrid(
    selected: List<Int>,
    onSelect: (Int) -> Unit
) {
    val categories = listOf(
        Triple(R.string.mood_random, Icons.Default.Casino, "Случайная активность"),
        Triple(R.string.mood_education, Icons.Default.School, "Обучение"),
        Triple(R.string.mood_recreational, Icons.Default.SportsEsports, "Развлечения"),
        Triple(R.string.mood_social, Icons.Default.Group, "Общение"),
        Triple(R.string.mood_diy, Icons.Default.Build, "Сделай сам"),
        Triple(R.string.mood_charity, Icons.Default.VolunteerActivism, "Благотворительность"),
        Triple(R.string.mood_cooking, Icons.Default.RestaurantMenu, "Кулинария"),
        Triple(R.string.mood_relaxation, Icons.Default.SelfImprovement, "Расслабление"),
        Triple(R.string.mood_music, Icons.Default.MusicNote, "Музыка"),
        Triple(R.string.mood_busywork, Icons.Default.WorkOutline, "Рутина")
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { (resId, icon, label) ->
                val isSelected = resId in selected
                val backgroundColor = if (isSelected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.surfaceVariant

                Surface(
                    onClick = { onSelect(resId) },
                    shape = RoundedCornerShape(16.dp),
                    color = backgroundColor,
                    tonalElevation = if (isSelected) 4.dp else 1.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(resId),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            maxLines = 2,
                            softWrap = true,
                            modifier = Modifier
                                .weight(1.5f)
                                .padding(end = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// Экран отображения рекомендации с анимацией загрузки
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
    val context = LocalContext.current
    // Загрузка анимации для состояния загрузки
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("loading.json"))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    // Локальное состояние для отслеживания статуса избранного
    var favoriteStatus by remember { mutableStateOf(false) }

    // Обновляем статус избранного при изменении активности
    LaunchedEffect(activity) {
        favoriteStatus = if (activity != null) {
            isFavorite(activity)
        } else {
            false
        }
    }

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
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.activity_label, activity.activity),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.type_label, activity.type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.participants_label, activity.participants ?: 0),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (activity.price == 0f) stringResource(R.string.price_free)
                        else stringResource(R.string.price_paid, activity.price ?: 0f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (activity.link.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.link_label, activity.link),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            // Кнопка для переключения статуса избранного
            IconButton(onClick = {
                onToggleFavorite(activity)
                favoriteStatus = !favoriteStatus // Оптимистическое обновление UI
            }) {
                Icon(
                    imageVector = if (favoriteStatus) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.nav_favorites),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            // Отображение анимации загрузки
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(150.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    onNewRecommendation()
                    favoriteStatus = false // Сброс статуса при новой рекомендации
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.new_recommendation), color = MaterialTheme.colorScheme.onPrimary)
            }
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

// Экран истории с анимацией карточек
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    history: Flow<List<ActivityEntity>>,
    viewModel: MainViewModel
) {
    // Собираем данные из Flow
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
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_history), color = MaterialTheme.colorScheme.onSurface) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                actions = {
                    // Кнопка выбора категории
                    var categoryExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        OutlinedButton(
                            onClick = { categoryExpanded = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(sortCategory.ifEmpty { stringResource(R.string.all_categories) })
                        }
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        viewModel.setHistorySortCategory(category)
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    // Кнопка удаления
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                showMenu = false
                                showSelectDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_all), color = MaterialTheme.colorScheme.onSurface) },
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
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Сообщение при пустом списке
            if (activities.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.history_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                // Список активностей
                items(activities.size) { index ->
                    val activity = activities[index]
                    ActivityCardAnimated(activity = activity)
                }
            }
        }

        // Диалог подтверждения удаления всех записей
        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(stringResource(R.string.confirm_delete_all), color = MaterialTheme.colorScheme.onSurface) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteAllHistory()
                            showDeleteAllDialog = false
                            Toast.makeText(context, R.string.delete_success, Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }

        // Диалог выбора записей для удаления
        if (showSelectDialog) {
            AlertDialog(
                onDismissRequest = { showSelectDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(stringResource(R.string.select_activities), color = MaterialTheme.colorScheme.onSurface) },
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
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(text = activity.activity, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp))
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
                        Text(stringResource(R.string.delete_selected), color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSelectDialog = false }) {
                        Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    }
}

// Экран избранного с анимацией карточек
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    favorites: Flow<List<ActivityEntity>>,
    viewModel: MainViewModel
) {
    // Собираем данные из Flow
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
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_favorites), color = MaterialTheme.colorScheme.onSurface) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                actions = {
                    // Кнопка выбора категории
                    var categoryExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        OutlinedButton(
                            onClick = { categoryExpanded = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(sortCategory.ifEmpty { stringResource(R.string.all_categories) })
                        }
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        viewModel.setFavoritesSortCategory(category)
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    // Кнопка удаления
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                showMenu = false
                                showSelectDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_all), color = MaterialTheme.colorScheme.onSurface) },
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
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Сообщение при пустом списке
            if (activities.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.favorites_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                // Список активностей
                items(activities.size) { index ->
                    val activity = activities[index]
                    ActivityCardAnimated(activity = activity)
                }
            }
        }

        // Диалог подтверждения удаления всех записей
        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(stringResource(R.string.confirm_delete_all), color = MaterialTheme.colorScheme.onSurface) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteAllFavorites()
                            showDeleteAllDialog = false
                            Toast.makeText(context, R.string.delete_success, Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }

        // Диалог выбора записей для удаления
        if (showSelectDialog) {
            AlertDialog(
                onDismissRequest = { showSelectDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(stringResource(R.string.select_activities), color = MaterialTheme.colorScheme.onSurface) },
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
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(text = activity.activity, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp))
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
                        Text(stringResource(R.string.delete_selected), color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSelectDialog = false }) {
                        Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    }
}

// Компонент анимированной карточки активности
@Composable
fun ActivityCardAnimated(activity: ActivityEntity) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.activity_label, activity.activity),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.type_label, activity.type),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.participants_label, activity.participants),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (activity.price == 0f) stringResource(R.string.price_free)
                    else stringResource(R.string.price_paid, activity.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (activity.link.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.link_label, activity.link),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
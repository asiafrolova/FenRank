package com.example.fencing_project.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fencing_project.R
import com.example.fencing_project.data.model.Bout
import com.example.fencing_project.data.model.Opponent
import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.viewmodel.HomeViewModel
import java.util.Calendar

private data class StatisticsData(
    val totalBouts: Int = 0,
    val victories: Int = 0,
    val defeats: Int = 0,
    val draws: Int = 0,
    val userScore: Int = 0,
    val opponentScore: Int = 0,
    val byWeaponHand: Map<String, StatsByCategory> = emptyMap(),
    val byWeaponType: Map<String, StatsByCategory> = emptyMap(),
    val byOpponent: Map<String, OpponentStats> = emptyMap()
)

private data class StatsByCategory(
    val bouts: Int = 0,
    val victories: Int = 0,
    val defeats: Int = 0,
    val draws: Int = 0,
    val userScore: Int = 0,
    val opponentScore: Int = 0
)

private data class OpponentStats(
    val opponentId: String,
    val opponentName: String,
    val bouts: Int = 0,
    val victories: Int = 0,
    val defeats: Int = 0,
    val draws: Int = 0,
    val userScore: Int = 0,
    val opponentScore: Int = 0
)

// Функция для расчета статистики (вынесена из Composable)
private fun calculateStatistics(
    boutsState: UIState<List<Bout>>,
    opponentsState: UIState<List<Opponent>>,
    selectedYear: Int,
    selectedMonth: Int,
    selectedOpponentId: String?
): StatisticsData {
    return when (boutsState) {
        is UIState.Success -> {
            val bouts = (boutsState as UIState.Success<List<Bout>>).data
            val opponents = when (opponentsState) {
                is UIState.Success -> (opponentsState as UIState.Success<List<Opponent>>).data
                else -> emptyList()
            }

            // Фильтруем бои
            val filteredBouts = bouts.filter { bout ->
                // Фильтр по сопернику
                val matchesOpponent = selectedOpponentId?.let { id ->
                    bout.opponentId == id
                } ?: true

                // Фильтр по дате
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = bout.date

                val matchesYear = if (selectedYear > 0) {
                    calendar.get(Calendar.YEAR) == selectedYear
                } else true

                val matchesMonth = if (selectedMonth > 0) {
                    (calendar.get(Calendar.MONTH) + 1) == selectedMonth
                } else true

                matchesOpponent && matchesYear && matchesMonth
            }

            // Рассчитываем статистику
            var totalBouts = 0
            var victories = 0
            var defeats = 0
            var draws = 0
            var userScore = 0
            var opponentScore = 0

            val byWeaponHand = mutableMapOf<String, StatsByCategory>()
            val byWeaponType = mutableMapOf<String, StatsByCategory>()
            val byOpponent = mutableMapOf<String, OpponentStats>()

            filteredBouts.forEach { bout ->
                val opponent = opponents.find { it.id == bout.opponentId }

                // Общая статистика
                totalBouts++
                userScore += bout.userScore
                opponentScore += bout.opponentScore

                when {
                    bout.userScore > bout.opponentScore -> victories++
                    bout.userScore < bout.opponentScore -> defeats++
                    else -> draws++
                }

                // Статистика по руке
                opponent?.weaponHand?.let { hand ->
                    val stats = byWeaponHand.getOrPut(hand) { StatsByCategory() }
                    byWeaponHand[hand] = stats.copy(
                        bouts = stats.bouts + 1,
                        victories = stats.victories + if (bout.userScore > bout.opponentScore) 1 else 0,
                        defeats = stats.defeats + if (bout.userScore < bout.opponentScore) 1 else 0,
                        draws = stats.draws + if (bout.userScore == bout.opponentScore) 1 else 0,
                        userScore = stats.userScore + bout.userScore,
                        opponentScore = stats.opponentScore + bout.opponentScore
                    )
                }

                // Статистика по оружию
                opponent?.weaponType?.let { type ->
                    val stats = byWeaponType.getOrPut(type) { StatsByCategory() }
                    byWeaponType[type] = stats.copy(
                        bouts = stats.bouts + 1,
                        victories = stats.victories + if (bout.userScore > bout.opponentScore) 1 else 0,
                        defeats = stats.defeats + if (bout.userScore < bout.opponentScore) 1 else 0,
                        draws = stats.draws + if (bout.userScore == bout.opponentScore) 1 else 0,
                        userScore = stats.userScore + bout.userScore,
                        opponentScore = stats.opponentScore + bout.opponentScore
                    )
                }

                // Статистика по сопернику
                opponent?.let { opp ->
                    val stats = byOpponent.getOrPut(opp.id) {
                        OpponentStats(opponentId = opp.id, opponentName = opp.name)
                    }
                    byOpponent[opp.id] = stats.copy(
                        bouts = stats.bouts + 1,
                        victories = stats.victories + if (bout.userScore > bout.opponentScore) 1 else 0,
                        defeats = stats.defeats + if (bout.userScore < bout.opponentScore) 1 else 0,
                        draws = stats.draws + if (bout.userScore == bout.opponentScore) 1 else 0,
                        userScore = stats.userScore + bout.userScore,
                        opponentScore = stats.opponentScore + bout.opponentScore
                    )
                }
            }

            StatisticsData(
                totalBouts = totalBouts,
                victories = victories,
                defeats = defeats,
                draws = draws,
                userScore = userScore,
                opponentScore = opponentScore,
                byWeaponHand = byWeaponHand,
                byWeaponType = byWeaponType,
                byOpponent = byOpponent
            )
        }
        else -> StatisticsData()
    }
}

@Composable
private fun StatisticsFilterSection(
    title: String,
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                FilterChip(
                    selected = selectedItem == item,
                    onClick = { onItemSelected(item) },
                    label = {
                        Text(
                            text = item,
                            color = if (selectedItem == item) Color.White else Color(200, 200, 200)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = if (selectedItem == item)
                            Color(139, 0, 0)
                        else Color(61, 61, 70),
                        selectedContainerColor = Color(139, 0, 0),
                        labelColor = Color.White
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    navController: NavController,
    pref: SharedPrefsManager,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val userId = pref.getUserId()

    // Фильтры по дате
    var selectedYear by remember { mutableIntStateOf(-1) } // -1 = все года
    var selectedMonth by remember { mutableIntStateOf(-1) } // -1 = все месяцы, 0 = не выбрано
    var selectedOpponentId by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    // Данные
    val boutsState by homeViewModel.bouts.collectAsState()
    val opponentsState by homeViewModel.opponents.collectAsState()

    LaunchedEffect(key1 = userId) {
        if (userId != null) {
            homeViewModel.loadUserData(userId)
        }
    }

    // Вычисляем статистику
    val statistics = remember(boutsState, opponentsState, selectedYear, selectedMonth, selectedOpponentId) {
        calculateStatistics(
            boutsState = boutsState,
            opponentsState = opponentsState,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            selectedOpponentId = selectedOpponentId
        )
    }

    // Доступные года для фильтрации
    val availableYears = remember(boutsState) {
        when (boutsState) {
            is UIState.Success -> {
                val bouts = (boutsState as UIState.Success<List<Bout>>).data
                bouts.map { bout ->
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = bout.date
                    calendar.get(Calendar.YEAR)
                }.distinct().sortedDescending()
            }
            else -> emptyList()
        }
    }

    // Доступные месяцы для выбранного года
    val availableMonths = remember(selectedYear, boutsState) {
        if (selectedYear <= 0) emptyList() else {
            when (boutsState) {
                is UIState.Success -> {
                    val bouts = (boutsState as UIState.Success<List<Bout>>).data
                    bouts.filter { bout ->
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = bout.date
                        calendar.get(Calendar.YEAR) == selectedYear
                    }.map { bout ->
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = bout.date
                        calendar.get(Calendar.MONTH) + 1
                    }.distinct().sorted()
                }
                else -> emptyList()
            }
        }
    }

    // Доступные соперники для фильтрации
    val availableOpponents = remember(opponentsState) {
        when (opponentsState) {
            is UIState.Success -> (opponentsState as UIState.Success<List<Opponent>>).data
            else -> emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Статистика", color = Color.White)
                        // Кнопка фильтров
                        Icon(
                            painter = painterResource(R.drawable.filter),
                            contentDescription = "Фильтры",
                            tint = if (selectedYear > 0 || selectedMonth > 0 || selectedOpponentId != null)
                                Color(139, 0, 0)
                            else
                                Color.White,
                            modifier = Modifier
                                .clickable {  showFilters = true }
                                .padding(end = 8.dp)
                                .size(20.dp)
                        )

                    }

                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.back),
                            contentDescription = "back",
                            tint = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(25, 25, 33),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(25, 25, 33))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {



                // Показатели активности фильтра
                FilterIndicator(
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    selectedOpponent = selectedOpponentId?.let { id ->
                        availableOpponents.find { it.id == id }?.name
                    },
                    onClear = {
                        selectedYear = -1
                        selectedMonth = -1
                        selectedOpponentId = null
                    }
                )

                // Общая статистика
                OverallStatisticsCard(statistics)

                // Статистика по рукам (таблица)
                if (statistics.byWeaponHand.isNotEmpty()) {
                    WeaponHandStatistics(statistics)
                }

                // Статистика по оружию (таблица)
                if (statistics.byWeaponType.isNotEmpty()) {
                    WeaponTypeStatistics(statistics)
                }

                // Статистика по соперникам (список)
                if (statistics.byOpponent.isNotEmpty()) {
                    OpponentStatistics(
                        statistics = statistics,
                        onOpponentClick = { opponentId ->
                            selectedOpponentId = if (selectedOpponentId == opponentId) null else opponentId
                        },
                        selectedOpponentId = selectedOpponentId
                    )
                }

                // Легенда сокращений
                LegendSection()
            }

            // BottomSheet для фильтров
            if (showFilters) {
                ModalBottomSheet(
                    onDismissRequest = { showFilters = false },
                    containerColor = Color(44, 44, 51)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Фильтры статистики",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Фильтр по году
                        StatisticsFilterSection(
                            title = "Год",
                            items = listOf("Все время") + availableYears.map { it.toString() },
                            selectedItem = if (selectedYear <= 0) "Все время" else selectedYear.toString(),
                            onItemSelected = { item ->
                                selectedYear = if (item == "Все время") -1 else item.toInt()
                                selectedMonth = -1 // Сбрасываем месяц при смене года
                            }
                        )

                        // Фильтр по месяцу (только если выбран год)
                        if (selectedYear > 0 && availableMonths.isNotEmpty()) {
                            StatisticsFilterSection(
                                title = "Месяц",
                                items = listOf("Все месяцы") + availableMonths.map { month ->
                                    when (month) {
                                        1 -> "Январь"; 2 -> "Февраль"; 3 -> "Март"; 4 -> "Апрель"
                                        5 -> "Май"; 6 -> "Июнь"; 7 -> "Июль"; 8 -> "Август"
                                        9 -> "Сентябрь"; 10 -> "Октябрь"; 11 -> "Ноябрь"; 12 -> "Декабрь"
                                        else -> "Месяц $month"
                                    }
                                },
                                selectedItem = if (selectedMonth <= 0) "Все месяцы" else when (selectedMonth) {
                                    1 -> "Январь"; 2 -> "Февраль"; 3 -> "Март"; 4 -> "Апрель"
                                    5 -> "Май"; 6 -> "Июнь"; 7 -> "Июль"; 8 -> "Август"
                                    9 -> "Сентябрь"; 10 -> "Октябрь"; 11 -> "Ноябрь"; 12 -> "Декабрь"
                                    else -> "Месяц $selectedMonth"
                                },
                                onItemSelected = { item ->
                                    selectedMonth = when (item) {
                                        "Все месяцы" -> -1
                                        "Январь" -> 1; "Февраль" -> 2; "Март" -> 3; "Апрель" -> 4
                                        "Май" -> 5; "Июнь" -> 6; "Июль" -> 7; "Август" -> 8
                                        "Сентябрь" -> 9; "Октябрь" -> 10; "Ноябрь" -> 11; "Декабрь" -> 12
                                        else -> -1
                                    }
                                }
                            )
                        }

                        // Фильтр по сопернику
                        StatisticsFilterSection(
                            title = "Соперник",
                            items = listOf("Все соперники") + availableOpponents.map { it.name },
                            selectedItem = selectedOpponentId?.let { id ->
                                availableOpponents.find { it.id == id }?.name
                            } ?: "Все соперники",
                            onItemSelected = { item ->
                                selectedOpponentId = if (item == "Все соперники") null else {
                                    availableOpponents.find { it.name == item }?.id
                                }
                            }
                        )

                        // Кнопки
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    selectedYear = -1
                                    selectedMonth = -1
                                    selectedOpponentId = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(133, 133, 133),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Сбросить все")
                            }

                            Button(
                                onClick = { showFilters = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(139, 0, 0),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Применить")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterIndicator(
    selectedYear: Int,
    selectedMonth: Int,
    selectedOpponent: String?,
    onClear: () -> Unit
) {
    val hasFilters = selectedYear > 0 || selectedMonth > 0 || selectedOpponent != null

    if (hasFilters) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(44, 44, 51)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Активные фильтры:",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    val filtersText = buildString {
                        if (selectedYear > 0) {
                            append("Год: $selectedYear")
                            if (selectedMonth > 0) {
                                val monthName = when (selectedMonth) {
                                    1 -> "Январь"; 2 -> "Февраль"; 3 -> "Март"; 4 -> "Апрель"
                                    5 -> "Май"; 6 -> "Июнь"; 7 -> "Июль"; 8 -> "Август"
                                    9 -> "Сентябрь"; 10 -> "Октябрь"; 11 -> "Ноябрь"; 12 -> "Декабрь"
                                    else -> "Месяц $selectedMonth"
                                }
                                append(", $monthName")
                            }
                        }
                        if (selectedOpponent != null) {
                            if (isNotEmpty()) append(", ")
                            append("Соперник: $selectedOpponent")
                        }
                        if (isEmpty()) append("Все время")
                    }

                    Text(
                        text = filtersText,
                        color = Color(200, 200, 200),
                        fontSize = 12.sp
                    )
                }

                TextButton(onClick = onClear) {
                    Text(
                        text = "Очистить",
                        color = Color(139, 0, 0),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OverallStatisticsCard(statistics: StatisticsData) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(44, 44, 51)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Общая статистика",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Показатели в ряд
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    value = statistics.totalBouts.toString(),
                    label = "Бои",
                    color = Color.White
                )

                StatItem(
                    value = statistics.victories.toString(),
                    label = "Победы",
                    color = Color(0xFF4CAF50)
                )

                StatItem(
                    value = statistics.defeats.toString(),
                    label = "Поражения",
                    color = Color(0xFFF44336)
                )

                StatItem(
                    value = statistics.draws.toString(),
                    label = "Ничьи",
                    color = Color(0xFFFF9800)
                )
            }

            // Уколы
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    value = statistics.userScore.toString(),
                    label = "Нанесено",
                    color = Color(0xFF2196F3)
                )

                StatItem(
                    value = statistics.opponentScore.toString(),
                    label = "Получено",
                    color = Color(0xFFFF5722)
                )

                val ratio = if (statistics.opponentScore > 0) {
                    String.format("%.2f", statistics.userScore.toFloat() / statistics.opponentScore)
                } else "∞"

                StatItem(
                    value = ratio,
                    label = "Коэф.",
                    color = Color(0xFF9C27B0)
                )
            }
        }
    }
}

@Composable
private fun WeaponHandStatistics(statistics: StatisticsData) {
    if (statistics.byWeaponHand.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "По ведущей руке",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // Таблица статистики по рукам
            StatisticsTable(
                categories = statistics.byWeaponHand.mapValues { it.value },
                columnNames = listOf("Рука", "Б", "V", "D", "N", "У+", "У-")
            )
        }
    }
}

@Composable
private fun WeaponTypeStatistics(statistics: StatisticsData) {
    if (statistics.byWeaponType.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "По виду оружия",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // Таблица статистики по оружию
            StatisticsTable(
                categories = statistics.byWeaponType.mapValues { it.value },
                columnNames = listOf("Оружие", "Б", "V", "D", "N", "У+", "У-")
            )
        }
    }
}

@Composable
private fun StatisticsTable(
    categories: Map<String, StatsByCategory>,
    columnNames: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(61, 61, 70)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Заголовок таблицы
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(44, 44, 51))
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                columnNames.forEachIndexed { index, name ->
                    Box(
                        modifier = Modifier.weight(
                            when (index) {
                                0 -> 1.5f // Название категории
                                else -> 1f // Числовые колонки
                            }
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Данные таблицы
            categories.forEach { (categoryName, stats) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    // Название категории
                    Box(
                        modifier = Modifier.weight(1.5f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = categoryName,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    // Бои
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stats.bouts.toString(),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    // Победы
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stats.victories.toString(),
                            color = Color(0xFF4CAF50),
                            fontSize = 14.sp
                        )
                    }

                    // Поражения
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stats.defeats.toString(),
                            color = Color(0xFFF44336),
                            fontSize = 14.sp
                        )
                    }

                    // Ничьи
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stats.draws.toString(),
                            color = Color(0xFFFF9800),
                            fontSize = 14.sp
                        )
                    }

                    // Нанесено уколов
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stats.userScore.toString(),
                            color = Color(0xFF2196F3),
                            fontSize = 14.sp
                        )
                    }

                    // Получено уколов
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stats.opponentScore.toString(),
                            color = Color(0xFFFF5722),
                            fontSize = 14.sp
                        )
                    }
                }

                // Разделитель
                if (categoryName != categories.keys.last()) {
                    Divider(
                        color = Color(100, 100, 100),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OpponentStatistics(
    statistics: StatisticsData,
    onOpponentClick: (String) -> Unit,
    selectedOpponentId: String?
) {
    if (statistics.byOpponent.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "По соперникам",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(300.dp)
            ) {
                items(statistics.byOpponent.values.toList().sortedByDescending { it.bouts }) { stats ->
                    OpponentStatCard(
                        stats = stats,
                        isSelected = selectedOpponentId == stats.opponentId,
                        onClick = { onOpponentClick(stats.opponentId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OpponentStatCard(
    stats: OpponentStats,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(139, 0, 0, 50) else Color(44, 44, 51)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stats.opponentName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "${stats.bouts} боев • ${stats.victories}V/${stats.defeats}D/${stats.draws}N",
                    color = Color(200, 200, 200),
                    fontSize = 12.sp
                )

                Text(
                    text = "Уколы: ${stats.userScore}+ / ${stats.opponentScore}-",
                    color = Color(200, 200, 200),
                    fontSize = 12.sp
                )
            }

            // Процент побед
            val winRate = if (stats.bouts > 0) {
                (stats.victories.toFloat() / stats.bouts * 100).toInt()
            } else 0

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = when {
                            winRate >= 70 -> Color(0xFF4CAF50)
                            winRate >= 40 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        },
                        shape = CircleShape
                    )
            ) {
                Text(
                    text = "$winRate%",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LegendSection() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(44, 44, 51)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Обозначения:",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            LegendItem("Б", "Бои (количество)")
            LegendItem("V", "Победы (Victories)")
            LegendItem("D", "Поражения (Defeats)")
            LegendItem("N", "Ничьи (Draws)")
            LegendItem("У+", "Нанесено уколов")
            LegendItem("У-", "Получено уколов")
            LegendItem("Коэф.", "Коэффициент (Нанесено/Получено)")
        }
    }
}

@Composable
private fun LegendItem(abbreviation: String, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = abbreviation,
            color = Color(139, 0, 0),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )

        Text(
            text = description,
            color = Color(200, 200, 200),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color(150, 150, 150),
            fontSize = 12.sp
        )
    }
}
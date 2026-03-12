package com.example.fencing_project.view

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fencing_project.R
import com.example.fencing_project.data.local.LocalBout
import com.example.fencing_project.data.local.LocalOpponent
import com.example.fencing_project.getString

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
    val byOpponent: Map<Long, OpponentStats> = emptyMap()
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
    val opponentId: Long,
    val opponentName: String,
    val bouts: Int = 0,
    val victories: Int = 0,
    val defeats: Int = 0,
    val draws: Int = 0,
    val userScore: Int = 0,
    val opponentScore: Int = 0
)


private fun calculateStatistics(
    boutsState: UIState<List<LocalBout>>,
    opponentsState: UIState<List<LocalOpponent>>,
    selectedYear: Int,
    selectedMonth: Int,
    selectedOpponentId: Long?,
    pref: SharedPrefsManager,
    context: Context
): StatisticsData {
    return when (boutsState) {
        is UIState.Success -> {
            val bouts = (boutsState as UIState.Success<List<LocalBout>>).data
            val opponents = when (opponentsState) {
                is UIState.Success -> (opponentsState as UIState.Success<List<LocalOpponent>>).data
                else -> emptyList()
            }

            val filteredBouts = bouts.filter { bout ->
                val matchesOpponent = selectedOpponentId?.let { id ->
                    bout.opponentId == id
                } ?: true

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

            var totalBouts = 0
            var victories = 0
            var defeats = 0
            var draws = 0
            var userScore = 0
            var opponentScore = 0

            val byWeaponHand = mutableMapOf<String, StatsByCategory>()
            val byWeaponType = mutableMapOf<String, StatsByCategory>()
            val byOpponent = mutableMapOf<Long, OpponentStats>()

            filteredBouts.forEach { bout ->
                val opponent = opponents.find { it.id == bout.opponentId }

                totalBouts++
                userScore += bout.userScore
                opponentScore += bout.opponentScore

                when {
                    bout.userScore > bout.opponentScore -> victories++
                    bout.userScore < bout.opponentScore -> defeats++
                    else -> draws++
                }

                opponent?.weaponHand?.let { hand ->
                    val hand_res = if(hand=="right"){getString(context,R.string.right,pref.getLanguage())}else{getString(context,R.string.left,pref.getLanguage())}

                    val stats = byWeaponHand.getOrPut(hand_res) { StatsByCategory() }
                    byWeaponHand[hand_res] = stats.copy(
                        bouts = stats.bouts + 1,
                        victories = stats.victories + if (bout.userScore > bout.opponentScore) 1 else 0,
                        defeats = stats.defeats + if (bout.userScore < bout.opponentScore) 1 else 0,
                        draws = stats.draws + if (bout.userScore == bout.opponentScore) 1 else 0,
                        userScore = stats.userScore + bout.userScore,
                        opponentScore = stats.opponentScore + bout.opponentScore
                    )
                }


                opponent?.weaponType?.let { type ->
                    val type_res = if(type=="foil"){getString(context,R.string.foil,pref.getLanguage())}else if(type=="epee"){getString(context,R.string.epee,pref.getLanguage())}else{getString(context,R.string.sabre,pref.getLanguage())}

                    val stats = byWeaponType.getOrPut(type_res) { StatsByCategory() }
                    byWeaponType[type_res] = stats.copy(
                        bouts = stats.bouts + 1,
                        victories = stats.victories + if (bout.userScore > bout.opponentScore) 1 else 0,
                        defeats = stats.defeats + if (bout.userScore < bout.opponentScore) 1 else 0,
                        draws = stats.draws + if (bout.userScore == bout.opponentScore) 1 else 0,
                        userScore = stats.userScore + bout.userScore,
                        opponentScore = stats.opponentScore + bout.opponentScore
                    )
                }


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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val userId = pref.getUserId()

    var selectedYear by remember { mutableIntStateOf(-1) }
    var selectedMonth by remember { mutableIntStateOf(-1) }
    var selectedOpponentId by remember { mutableStateOf<Long?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    val boutsState by homeViewModel.bouts.collectAsState()
    val opponentsState by homeViewModel.opponents.collectAsState()

    LaunchedEffect(key1 = userId) {
        if (userId != null) {
            homeViewModel.loadUserData(userId)
        }
    }


    val statistics = remember(boutsState, opponentsState, selectedYear, selectedMonth, selectedOpponentId) {
        calculateStatistics(
            boutsState = boutsState,
            opponentsState = opponentsState,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            selectedOpponentId = selectedOpponentId,
            pref = pref,
            context = context,
        )
    }

    val availableYears = remember(boutsState) {
        when (boutsState) {
            is UIState.Success -> {
                val bouts = (boutsState as UIState.Success<List<LocalBout>>).data
                bouts.map { bout ->
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = bout.date
                    calendar.get(Calendar.YEAR)
                }.distinct().sortedDescending()
            }
            else -> emptyList()
        }
    }


    val availableMonths = remember(selectedYear, boutsState) {
        if (selectedYear <= 0) emptyList() else {
            when (boutsState) {
                is UIState.Success -> {
                    val bouts = (boutsState as UIState.Success<List<LocalBout>>).data
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

    val availableOpponents = remember(opponentsState) {
        when (opponentsState) {
            is UIState.Success -> (opponentsState as UIState.Success<List<LocalOpponent>>).data
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
                        Text(getString(context,R.string.statistics,pref.getLanguage()), color = Color.White)
                        Icon(
                            painter = painterResource(R.drawable.filter),
                            contentDescription = getString(context,R.string.filters,pref.getLanguage()),
                            tint = if (selectedYear > 0 || selectedMonth > 0 || selectedOpponentId != null)
                                Color(139, 0, 0)
                            else
                                Color.White,
                            modifier = Modifier
                                .clickable { showFilters = true }
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
                    },
                    context,
                    pref
                )

                OverallStatisticsCard(statistics, context, pref)
                if (statistics.byWeaponHand.isNotEmpty()) {
                    WeaponHandStatistics(statistics, context, pref)

                }

                if (statistics.byWeaponType.isNotEmpty()) {
                    WeaponTypeStatistics(statistics, context, pref)
                }

                if (statistics.byOpponent.isNotEmpty()) {
                    OpponentStatistics(
                        statistics = statistics,
                        onOpponentClick = { opponentId ->
                            selectedOpponentId = if (selectedOpponentId == opponentId) null else opponentId
                        },
                        selectedOpponentId = selectedOpponentId,
                        context,
                        pref
                    )
                }

                LegendSection(context,pref)
            }


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
                            text = getString(context,R.string.filters_statistics,pref.getLanguage()),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        StatisticsFilterSection(
                            title = getString(context,R.string.year,pref.getLanguage()),
                            items = listOf(getString(context,R.string.all_time,pref.getLanguage())) + availableYears.map { it.toString() },
                            selectedItem = if (selectedYear <= 0) getString(context,R.string.all_time,pref.getLanguage()) else selectedYear.toString(),
                            onItemSelected = { item ->
                                selectedYear = if (item == getString(context,R.string.all_time,pref.getLanguage())) -1 else item.toInt()
                                selectedMonth = -1
                            }
                        )

                        if (selectedYear > 0 && availableMonths.isNotEmpty()) {
                            StatisticsFilterSection(
                                title =getString(context,R.string.month,pref.getLanguage()),
                                items = listOf(getString(context,R.string.all_months,pref.getLanguage())) + availableMonths.map { month ->
                                    when (month) {
                                        1 -> getString(context,R.string.january,pref.getLanguage()); 2 -> getString(context,R.string.february,pref.getLanguage()); 3 -> getString(context,R.string.march,pref.getLanguage()); 4 -> getString(context,R.string.april,pref.getLanguage())
                                        5 -> getString(context,R.string.may,pref.getLanguage()); 6 -> getString(context,R.string.june,pref.getLanguage()); 7 -> getString(context,R.string.july,pref.getLanguage()); 8 ->getString(context,R.string.august,pref.getLanguage())
                                        9 -> getString(context,R.string.september,pref.getLanguage()); 10 -> getString(context,R.string.october,pref.getLanguage()); 11 -> getString(context,R.string.november,pref.getLanguage()); 12 ->  getString(context,R.string.december,pref.getLanguage())
                                        else -> getString(context,R.string.month,pref.getLanguage())
                                    }
                                },
                                selectedItem = if (selectedMonth <= 0) getString(context,R.string.all_months,pref.getLanguage()) else when (selectedMonth) {
                                    1 -> getString(context,R.string.january,pref.getLanguage()); 2 -> getString(context,R.string.february,pref.getLanguage()); 3 -> getString(context,R.string.march,pref.getLanguage()); 4 -> getString(context,R.string.april,pref.getLanguage())
                                    5 -> getString(context,R.string.may,pref.getLanguage()); 6 ->getString(context,R.string.june,pref.getLanguage()); 7 -> getString(context,R.string.july,pref.getLanguage()); 8 -> getString(context,R.string.august,pref.getLanguage())
                                    9 -> getString(context,R.string.september,pref.getLanguage()); 10 -> getString(context,R.string.october,pref.getLanguage()); 11 -> getString(context,R.string.november,pref.getLanguage()); 12 ->  getString(context,R.string.december,pref.getLanguage())
                                    else ->  getString(context,R.string.month,pref.getLanguage())
                                },
                                onItemSelected = { item ->
                                    selectedMonth = when (item) {
                                        getString(context,R.string.all_months,pref.getLanguage()) -> -1
                                        getString(context,R.string.january,pref.getLanguage()) -> 1; getString(context,R.string.february,pref.getLanguage()) -> 2; getString(context,R.string.march,pref.getLanguage()) -> 3; getString(context,R.string.april,pref.getLanguage()) -> 4
                                        getString(context,R.string.may,pref.getLanguage()) -> 5; getString(context,R.string.june,pref.getLanguage()) -> 6; getString(context,R.string.july,pref.getLanguage()) -> 7; getString(context,R.string.august,pref.getLanguage()) -> 8
                                        getString(context,R.string.september,pref.getLanguage()) -> 9; getString(context,R.string.october,pref.getLanguage()) -> 10;  getString(context,R.string.november,pref.getLanguage()) -> 11;  getString(context,R.string.december,pref.getLanguage()) -> 12
                                        else -> -1
                                    }
                                }
                            )
                        }

                        StatisticsFilterSection(
                            title = getString(context,R.string.opponent,pref.getLanguage()),
                            items = listOf(getString(context,R.string.all_opponents,pref.getLanguage())) + availableOpponents.map { it.name },
                            selectedItem = selectedOpponentId?.let { id ->
                                availableOpponents.find { it.id == id }?.name
                            } ?: getString(context,R.string.opponent,pref.getLanguage()),
                            onItemSelected = { item ->
                                selectedOpponentId = if (item == getString(context,R.string.opponent,pref.getLanguage())) null else {
                                    availableOpponents.find { it.name == item }?.id
                                }
                            }
                        )

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
                                Text(getString(context,R.string.reset_all,pref.getLanguage()))
                            }

                            Button(
                                onClick = { showFilters = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(139, 0, 0),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(getString(context,R.string.apply,pref.getLanguage()))
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
    onClear: () -> Unit,
    context:Context,
    pref: SharedPrefsManager,
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
                        text = getString(context,R.string.active_filters,pref.getLanguage()),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    val filtersText = buildString {
                        if (selectedYear > 0) {
                            append(getString(context,R.string.year,pref.getLanguage())+":"+"$selectedYear")
                            if (selectedMonth > 0) {
                                val monthName = when (selectedMonth) {
                                    1 -> getString(context,R.string.january,pref.getLanguage()); 2 -> getString(context,R.string.february,pref.getLanguage()); 3 -> getString(context,R.string.march,pref.getLanguage()); 4 -> getString(context,R.string.april,pref.getLanguage())
                                    5 -> getString(context,R.string.may,pref.getLanguage()); 6 -> getString(context,R.string.june,pref.getLanguage()); 7 -> getString(context,R.string.july,pref.getLanguage()); 8 ->getString(context,R.string.august,pref.getLanguage())
                                    9 -> getString(context,R.string.september,pref.getLanguage()); 10 -> getString(context,R.string.october,pref.getLanguage()); 11 -> getString(context,R.string.november,pref.getLanguage()); 12 ->  getString(context,R.string.december,pref.getLanguage())
                                    else -> getString(context,R.string.month,pref.getLanguage())
                                }
                                append(", $monthName")
                            }
                        }
                        if (selectedOpponent != null) {
                            if (isNotEmpty()) append(", ")
                            append(getString(context,R.string.opponent,pref.getLanguage())+":"+"${selectedOpponent}")
                        }
                        if (isEmpty()) append(getString(context,R.string.all_time,pref.getLanguage()))
                    }

                    Text(
                        text = filtersText,
                        color = Color(200, 200, 200),
                        fontSize = 12.sp
                    )
                }

                TextButton(onClick = onClear) {
                    Text(
                        text = getString(context,R.string.clear,pref.getLanguage()),
                        color = Color(139, 0, 0),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OverallStatisticsCard(statistics: StatisticsData, context:Context, pref: SharedPrefsManager) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(44, 44, 51)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = getString(context,R.string.general_statistics,pref.getLanguage()),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    value = statistics.totalBouts.toString(),
                    label = getString(context,R.string.bouts,pref.getLanguage()),
                    color = Color.White
                )

                StatItem(
                    value = statistics.victories.toString(),
                    label = getString(context,R.string.victories,pref.getLanguage()),
                    color = Color(0xFF4CAF50)
                )

                StatItem(
                    value = statistics.defeats.toString(),
                    label = getString(context,R.string.defeats,pref.getLanguage()),
                    color = Color(0xFFF44336)
                )

                StatItem(
                    value = statistics.draws.toString(),
                    label = getString(context,R.string.draws,pref.getLanguage()),
                    color = Color(0xFFFF9800)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    value = statistics.userScore.toString(),
                    label =getString(context,R.string.given,pref.getLanguage()),
                    color = Color(0xFF2196F3)
                )

                StatItem(
                    value = statistics.opponentScore.toString(),
                    label =getString(context,R.string.recieved,pref.getLanguage()),
                    color = Color(0xFFFF5722)
                )

                val ratio = if (statistics.opponentScore > 0) {
                    String.format("%.2f", statistics.victories.toFloat() / statistics.totalBouts*100)+"%"
                } else "∞"

                StatItem(
                    value = ratio,
                    label = getString(context,R.string.victories_given,pref.getLanguage()),
                    color = Color(0xFF9C27B0)
                )
            }
        }
    }
}

@Composable
private fun WeaponHandStatistics(statistics: StatisticsData, context: Context, pref: SharedPrefsManager) {
    if (statistics.byWeaponHand.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = getString(context,R.string.by_weapon_hand,pref.getLanguage()),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            StatisticsTable(
                categories = statistics.byWeaponHand.mapValues { it.value },
                columnNames = listOf(getString(context,R.string.hand,pref.getLanguage()), getString(context,R.string.bouts_design,pref.getLanguage()),
                    "V", "D", "N",
                    getString(context,R.string.given_score_des,pref.getLanguage()),
                    getString(context,R.string.recieved_score_des,pref.getLanguage()))
            )
        }
    }
}

@Composable
private fun WeaponTypeStatistics(statistics: StatisticsData, context: Context, pref: SharedPrefsManager) {
    if (statistics.byWeaponType.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = getString(context,R.string.by_weapon_type,pref.getLanguage()),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            StatisticsTable(
                categories = statistics.byWeaponType.mapValues { it.value },
                columnNames = listOf(getString(context,R.string.weapon,pref.getLanguage()), getString(context,R.string.bouts_design,pref.getLanguage()),
                    "V", "D", "N",
                    getString(context,R.string.given_score_des,pref.getLanguage()),
                    getString(context,R.string.recieved_score_des,pref.getLanguage()))
            )
        }
    }
}

@Composable
private fun StatisticsTable(
    categories: Map<String, StatsByCategory>,
    columnNames: List<String>,

) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(61, 61, 70)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
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
                                0 -> 1.5f
                                else -> 1f
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

            categories.forEach { (categoryName, stats) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                ) {

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
    onOpponentClick: (Long) -> Unit,
    selectedOpponentId:Long?,
    context: Context,
    pref: SharedPrefsManager
) {
    if (statistics.byOpponent.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = getString(context,R.string.by_opponents,pref.getLanguage()),
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
                        onClick = { onOpponentClick(stats.opponentId) },
                        context,
                        pref
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
    onClick: () -> Unit,
    context: Context,
    pref: SharedPrefsManager
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
                    text = "${stats.bouts}" +getString(context,R.string.bouts_given,pref.getLanguage())+" • ${stats.victories}V/${stats.defeats}D/${stats.draws}N",
                    color = Color(200, 200, 200),
                    fontSize = 12.sp
                )

                Text(
                    text = getString(context,R.string.score,pref.getLanguage()) +":"+" ${stats.userScore}+ / ${stats.opponentScore}-",
                    color = Color(200, 200, 200),
                    fontSize = 12.sp
                )
            }

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
private fun LegendSection(context: Context, pref: SharedPrefsManager) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(44, 44, 51)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = getString(context,R.string.designations,pref.getLanguage()),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            LegendItem(getString(context,R.string.bouts_design,pref.getLanguage()), getString(context,R.string.bouts_count,pref.getLanguage()))
            LegendItem("V", getString(context,R.string.victoties_des,pref.getLanguage()))
            LegendItem("D", getString(context,R.string.defeats_des,pref.getLanguage()))
            LegendItem("N", getString(context,R.string.draws_des,pref.getLanguage()))
            LegendItem(getString(context,R.string.given_score_des,pref.getLanguage()), getString(context,R.string.given_des,pref.getLanguage()))
            LegendItem(getString(context,R.string.recieved_score_des,pref.getLanguage()), getString(context,R.string.recieved_des,pref.getLanguage()))
            LegendItem(getString(context,R.string.victories_given,pref.getLanguage()),
                getString(context,R.string.percent_victories,pref.getLanguage())
            )
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
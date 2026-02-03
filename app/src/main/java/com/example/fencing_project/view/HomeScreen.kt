package com.example.fencing_project.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fencing_project.R
import com.example.fencing_project.data.local.LocalBout
import com.example.fencing_project.data.local.LocalOpponent
import com.example.fencing_project.utils.SharedPrefsManager


import com.example.fencing_project.utils.UIState
import com.example.fencing_project.view.components.BottomNavigationBar
import com.example.fencing_project.view.components.RestoreDataDialog
import com.example.fencing_project.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    navController: NavController,

    viewModel: HomeViewModel = hiltViewModel(),
    pref: SharedPrefsManager,

) {
    var showRestoreDialog by remember { mutableStateOf(!pref.isOffline()) }


    val userId: String? = pref.getUserId()
    LaunchedEffect(key1 = userId) {
        if (userId != null && userId.isNotBlank()) {
            println("DEBUG: Загружаем данные для userId: $userId")
            viewModel.loadUserData(userId)
        } else {
            println("DEBUG: userId is null or blank")
        }
    }

    val boutsState by viewModel.bouts.collectAsState()
    val opponentsState by viewModel.opponents.collectAsState()

    // Вычисляем топ-3 популярных соперников
    val topOpponents = remember(opponentsState) {
        when (opponentsState) {
            is UIState.Success -> {
                val opponents = (opponentsState as UIState.Success<List<LocalOpponent>>).data
                // Сортируем по количеству боев (totalBouts)
                opponents.sortedByDescending { it.totalBouts }
                    .take(3) // Берем топ-3
            }
            else -> emptyList()
        }
    }

    // Берем последние 5 боев
    val latestBouts = remember(boutsState) {
        when (boutsState) {
            is UIState.Success -> {
                val bouts = (boutsState as UIState.Success<List<LocalBout>>).data
                // Сортируем по дате (новые сверху) и берем первые 5
                bouts.sortedByDescending { it.date }
                    .take(5)
            }
            else -> emptyList()
        }
    }

    if (showRestoreDialog) {
        RestoreDataDialog(
            onDismiss = { showRestoreDialog = false }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("choice_add") },
                containerColor = Color(139, 0, 0),
                shape = CircleShape
            ) {
                Icon(
                    painterResource(R.drawable.add_bout_ic),
                    contentDescription = stringResource(R.string.add_bout),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp, 20.dp)
                )
            }
        },
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(25, 25, 33))
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Добро пожаловать в Engarde!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 16.dp)
            )


            // Секция: Самые популярные соперники
            Text(
                text = "Популярные соперники",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            when (opponentsState) {
                is UIState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(139, 0, 0))
                    }
                }

                is UIState.Success -> {
                    if (topOpponents.isEmpty()) {
                        EmptyStateOpponents(
                            text = "У вас пока нет соперников",
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        OpponentsListHome(
                            opponents = topOpponents,
                            onOpponentClick = {
                                opponent ->
                                println("DEBUG: opponentId in Home = ${opponent.id}")
                                navController.navigate("edit_opponent/${opponent.id}")
                            }
                        )


                    }
                }

                is UIState.Error -> {
                    ErrorStateOpponents(
                        message = (opponentsState as UIState.Error).message,
                        onRetry = { userId?.let { viewModel.loadUserData(it) } },
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                is UIState.Idle -> {
                    EmptyStateOpponents(
                        text = "Начните добавлять соперников",
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Секция: Последние бои
            Text(
                text = "Последние бои",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            when (boutsState) {
                is UIState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(139, 0, 0))
                    }
                }

                is UIState.Success -> {
                    if (latestBouts.isEmpty()) {
                        EmptyStateBouts(
                            text = "У вас пока нет боев",
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        // Получаем список соперников
                        val opponentsMap = when (opponentsState) {
                            is UIState.Success -> {
                                val opponentsList = (opponentsState as UIState.Success<List<LocalOpponent>>).data
                                opponentsList.associateBy { it.id }
                            }
                            else -> emptyMap()
                        }

                        BoutsListHome(
                            bouts = latestBouts,
                            opponents = (opponentsState as? UIState.Success<List<LocalOpponent>>)?.data ?: emptyList(),
                            onBoutClick = { bout ->
                                navController.navigate("edit_bout/${bout.id}")
                            }
                        )

                    }
                }

                is UIState.Error -> {
                    ErrorStateBouts(
                        message = (boutsState as UIState.Error).message,
                        onRetry = { userId?.let { viewModel.loadUserData(it) } },
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                is UIState.Idle -> {
                    EmptyStateBouts(
                        text = "Начните добавлять бои",
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        }
    }
}

// Список соперников для домашнего экрана (без изменений из OpponentsScreen)
@Composable
private fun OpponentsListHome(
    opponents: List<LocalOpponent>,
    onOpponentClick: (LocalOpponent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        opponents.forEach { opponent ->
            OpponentItem(
                opponent = opponent,
                onClick = { onOpponentClick(opponent) }
            )
        }
    }
}

// Список боев для домашнего экрана (без изменений из OpponentsScreen)
@Composable
private fun BoutsListHome(
    bouts: List<LocalBout>,
    opponents: List<LocalOpponent>,
    onBoutClick: (LocalBout) -> Unit,
    modifier: Modifier = Modifier
) {
    // Группируем бои по датам
    val groupedBouts = remember(bouts) {
        bouts.sortedByDescending { it.date }
            .groupBy { bout ->
                SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(bout.date))
            }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedBouts.forEach { (date, dateBouts) ->
            // Заголовок с датой
            Text(
                text = date,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            dateBouts.forEach { bout ->
                val opponent = opponents.find { it.id == bout.opponentId }
                    BoutItem(
                    bout = bout,
                    opponent = opponent,
                    onClick = { onBoutClick(bout) }
                )
            }
        }
    }
}

// Компоненты состояний для домашнего экрана
@Composable
private fun EmptyStateOpponents(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.fencing_mask),
            contentDescription = "Пусто",
            tint = Color(100, 100, 100),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = text,
            color = Color(150, 150, 150),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ErrorStateOpponents(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.error),
            contentDescription = "Ошибка",
            tint = Color(0xFFF44336),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            color = Color(0xFFF44336),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(139, 0, 0),
                contentColor = Color.White
            ),
            modifier = Modifier.height(36.dp)
        ) {
            Text("Повторить", fontSize = 14.sp)
        }
    }
}

@Composable
private fun EmptyStateBouts(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.bout),
            contentDescription = "Пусто",
            tint = Color(100, 100, 100),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = text,
            color = Color(150, 150, 150),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ErrorStateBouts(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.error),
            contentDescription = "Ошибка",
            tint = Color(0xFFF44336),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            color = Color(0xFFF44336),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(139, 0, 0),
                contentColor = Color.White
            ),
            modifier = Modifier.height(36.dp)
        ) {
            Text("Повторить", fontSize = 14.sp)
        }
    }
}

// Нужно добавить импорт из OpponentsScreen или скопировать компоненты:
// 1. OpponentItem
// 2. BoutItem
// 3. ScoreDisplay
// 4. StatItem
// 5. И функция расширения Bout.getResultText()
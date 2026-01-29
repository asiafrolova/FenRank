package com.example.fencing_project.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fencing_project.R
import com.example.fencing_project.data.model.Bout
import com.example.fencing_project.data.model.Opponent
import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.view.components.BottomNavigationBar
import com.example.fencing_project.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpponentsScreen(
    navController: NavController,
    pref: SharedPrefsManager,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val userId = pref.getUserId()

    // Состояния для фильтрации
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(Tab.OPPONENTS) } // Начинаем с соперников
    val focusManager = LocalFocusManager.current

    // Получаем данные из ViewModel
    val boutsState by homeViewModel.bouts.collectAsState()
    val opponentsState by homeViewModel.opponents.collectAsState()

    LaunchedEffect(key1 = userId) {
        if (userId != null) {
            homeViewModel.loadUserData(userId)
        }
    }

    // Фильтруем данные в зависимости от поискового запроса
    val filteredBouts = remember(boutsState, searchQuery) {
        when (boutsState) {
            is UIState.Success -> {
                val bouts = (boutsState as UIState.Success<List<Bout>>).data
                if (searchQuery.isBlank()) bouts
                else bouts.filter { bout ->
                    bout.comment?.contains(searchQuery, ignoreCase = true) == true ||
                            bout.getResultText().contains(searchQuery, ignoreCase = true)
                }
            }
            else -> emptyList()
        }
    }

    val filteredOpponents = remember(opponentsState, searchQuery) {
        when (opponentsState) {
            is UIState.Success -> {
                val opponents = (opponentsState as UIState.Success<List<Opponent>>).data
                if (searchQuery.isBlank()) opponents
                else opponents.filter { opponent ->
                    opponent.name.contains(searchQuery, ignoreCase = true) ||
                            opponent.comment?.contains(searchQuery, ignoreCase = true) == true ||
                            opponent.weaponType.contains(searchQuery, ignoreCase = true) ||
                            opponent.weaponHand.contains(searchQuery, ignoreCase = true)
                }
            }
            else -> emptyList()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.background(Color(25, 25, 33))
            ) {
                // Поисковая строка
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { focusManager.clearFocus() },
                    onClearQuery = { searchQuery = "" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end=16.dp, top = 40.dp, bottom = 8.dp)
                )

                // Табы для переключения между боями и соперниками
                TabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
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
        bottomBar = { BottomNavigationBar(navController = navController) },

    ) { innerPadding ->
        Box(
            modifier = Modifier

                .fillMaxSize()
                .background(Color(25, 25, 33))
        ) {
            when (selectedTab) {
                Tab.OPPONENTS -> {
                    // Отображаем список соперников
                    when (opponentsState) {
                        is UIState.Loading -> {
                            LoadingIndicator()
                        }
                        is UIState.Success -> {
                            if (filteredOpponents.isEmpty()) {
                                EmptyState(
                                    text = if (searchQuery.isBlank())
                                        "У вас пока нет соперников"
                                    else "Соперники не найдены",
                                    modifier = Modifier.padding(innerPadding)
                                )
                            } else {
                                OpponentsList(
                                    opponents = filteredOpponents,
                                    onOpponentClick = { opponent ->
                                        navController.navigate("edit_opponent/${opponent.id}")
                                    },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                        is UIState.Error -> {
                            ErrorState(
                                message = (opponentsState as UIState.Error).message,
                                onRetry = { userId?.let { homeViewModel.loadUserData(it) } },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        is UIState.Idle -> {
                            // Ничего не показываем, ждем загрузки
                        }
                    }
                }
                Tab.BOUTS -> {
                    // Отображаем список боев
                    when (boutsState) {
                        is UIState.Loading -> {
                            LoadingIndicator()
                        }
                        is UIState.Success -> {
                            if (filteredBouts.isEmpty()) {
                                EmptyState(
                                    text = if (searchQuery.isBlank())
                                        "У вас пока нет боев"
                                    else "Бои не найдены",
                                    modifier = Modifier.padding(innerPadding)
                                )
                            } else {
                                BoutsList(
                                    bouts = filteredBouts,
                                    opponents = (opponentsState as? UIState.Success<List<Opponent>>)?.data ?: emptyList(),
                                    onBoutClick = { bout ->
                                        navController.navigate("edit_bout/${bout.id}")
                                    },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                        is UIState.Error -> {
                            ErrorState(
                                message = (boutsState as UIState.Error).message,
                                onRetry = { userId?.let { homeViewModel.loadUserData(it) } },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        is UIState.Idle -> {
                            // Ничего не показываем, ждем загрузки
                        }
                    }
                }
            }
        }
    }
}

// Компонент поиска
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Поиск...",
                    color = Color(126, 126, 126, 255)
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = "Поиск",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Очистить",
                        tint = Color.White,
                        modifier = Modifier.clickable { onClearQuery() }.size(15.dp)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { onSearch() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Color(139, 0, 0),
                unfocusedBorderColor = Color(126, 126, 126, 255),
                focusedContainerColor = Color(44, 44, 51),
                unfocusedContainerColor = Color(44, 44, 51),
                focusedLeadingIconColor = Color.White,
                unfocusedLeadingIconColor = Color.White
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            textStyle = TextStyle(color = Color.White)
        )
    }
}

// Компонент табов
@Composable
private fun TabBar(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(25, 25, 33)),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Кнопка "Бои"
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { onTabSelected(Tab.BOUTS) }
                .background(if (selectedTab == Tab.BOUTS) Color(44, 44, 51) else Color(25, 25, 33))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Бои",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (selectedTab == Tab.BOUTS) FontWeight.Bold else FontWeight.Medium
                )

                // Подчеркивание для выбранной вкладки
                if (selectedTab == Tab.BOUTS) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(3.dp)
                            .background(Color(139, 0, 0), RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        // Кнопка "Соперники"
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { onTabSelected(Tab.OPPONENTS) }
                .background(if (selectedTab == Tab.OPPONENTS) Color(44, 44, 51) else Color(25, 25, 33))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Соперники",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (selectedTab == Tab.OPPONENTS) FontWeight.Bold else FontWeight.Medium
                )

                // Подчеркивание для выбранной вкладки
                if (selectedTab == Tab.OPPONENTS) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(3.dp)
                            .background(Color(139, 0, 0), RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }

}


// Компонент списка соперников
@Composable
public fun OpponentsList(
    opponents: List<Opponent>,
    onOpponentClick: (Opponent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(opponents, key = { it.id }) { opponent ->
            OpponentItem(
                opponent = opponent,
                onClick = { onOpponentClick(opponent) }
            )
        }
    }
}

@Composable
public fun OpponentItem(
    opponent: Opponent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(44, 44, 51)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Аватарка
            AsyncImage(
                model = opponent.avatarUrl,
                contentDescription = "Аватар ${opponent.name}",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.avatar)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = opponent.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = opponent.weaponType,
                        color = Color(200, 200, 200),
                        fontSize = 14.sp
                    )

                    Text(
                        text = "•",
                        color = Color(200, 200, 200)
                    )

                    Text(
                        text = opponent.weaponHand,
                        color = Color(200, 200, 200),
                        fontSize = 14.sp
                    )
                }

                opponent.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                    Text(
                        text = comment,
                        color = Color(150, 150, 150),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Статистика
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    StatItem(
                        label = "Бои",
                        value = opponent.totalBouts.toString()
                    )

                    StatItem(
                        label = "Победы",
                        value = opponent.userWins.toString(),
                        color = Color(0xFF4CAF50)
                    )

                    StatItem(
                        label = "Поражения",
                        value = opponent.opponentWins.toString(),
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

// Компонент списка боев
@Composable
public fun BoutsList(
    bouts: List<Bout>,
    opponents: List<Opponent>,
    onBoutClick: (Bout) -> Unit,
    modifier: Modifier = Modifier
) {
    // Группируем бои по датам
    val groupedBouts = remember(bouts) {
        bouts.sortedByDescending { it.date }
            .groupBy { bout ->
                SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(bout.date))
            }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedBouts.forEach { (date, dateBouts) ->
            // Заголовок с датой
            item {
                Text(
                    text = date,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(dateBouts, key = { it.id }) { bout ->
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

@Composable
public fun BoutItem(
    bout: Bout,
    opponent: Opponent?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(44, 44, 51)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Информация о сопернике
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = opponent?.avatarUrl,
                    contentDescription = "Аватар ${opponent?.name ?: "Неизвестный"}",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.avatar)
                )

                Text(
                    text = opponent?.name ?: "Неизвестный соперник",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Счет
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreDisplay(
                    userScore = bout.userScore,
                    opponentScore = bout.opponentScore,
                    resultText = bout.getResultText()
                )


            }

            // Комментарий если есть
            bout.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                Text(
                    text = comment,
                    color = Color(150, 150, 150),
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ScoreDisplay(
    userScore: Int,
    opponentScore: Int,
    resultText: String
) {
    val resultColor = remember(userScore, opponentScore) {
        when {
            userScore > opponentScore -> Color(0xFF4CAF50) // Победа
            userScore < opponentScore -> Color(0xFFF44336) // Поражение
            else -> Color(0xFFFF9800) // Ничья
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$userScore : $opponentScore",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = resultText,
            color = resultColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Вспомогательные компоненты
@Composable
public fun StatItem(
    label: String,
    value: String,
    color: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color(150, 150, 150),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = Color(139, 0, 0),
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun EmptyState(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.bout),
            contentDescription = "Пусто",
            tint = Color(100, 100, 100),
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = text,
            color = Color(150, 150, 150),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.error),
            contentDescription = "Ошибка",
            tint = Color(0xFFF44336),
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            color = Color(0xFFF44336),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(139, 0, 0),
                contentColor = Color.White
            )
        ) {
            Text("Повторить")
        }
    }
}

// Расширение для Bout чтобы получить текст результата
public fun Bout.getResultText(): String {
    return when {
        userScore > opponentScore -> "Победа"
        userScore < opponentScore -> "Поражение"
        else -> "Ничья"
    }
}

// Enum для табов
private enum class Tab {
    BOUTS, OPPONENTS
}
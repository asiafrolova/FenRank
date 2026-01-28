package com.example.fencing_project.view


import SimpleDatePickerButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.fencing_project.viewmodel.BoutViewModel
import com.example.fencing_project.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBoutScreen(
    navController: NavController,
    pref: SharedPrefsManager,
    boutViewModel: BoutViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val userId = pref.getUserId()

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedOpponentId by remember { mutableStateOf("") }
    var userScore by remember { mutableStateOf("") }
    var opponentScore by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val opponents by homeViewModel.opponents.collectAsState()
    val opponentsState by homeViewModel.opponents.collectAsState()


//    LaunchedEffect(key1 = userId) {
//        homeViewModel.loadUserData(userId!!)
//    }
    LaunchedEffect(opponentsState) {
        println("DEBUG: opponentsState изменилось: $opponentsState")
    }
    LaunchedEffect(key1 = userId) {
        println("DEBUG: Запускаем loadUserData с userId: $userId")
        if (userId != null) {
            homeViewModel.loadUserData(userId)
        } else {
            println("DEBUG: userId = null!")
        }
    }

    // Обработка состояния добавления боя
    val addBoutState by boutViewModel.addBoutState.collectAsState()
    LaunchedEffect(key1 = addBoutState) {
        when (addBoutState) {
            is UIState.Success -> {
                navController.popBackStack()
                boutViewModel.resetState()
            }
            is UIState.Error -> {
                // Можно показать Snackbar
                boutViewModel.resetState()
            }
            else -> {}
        }
    }

    val resultText = remember(userScore, opponentScore) {
        val user = userScore.toIntOrNull() ?: 0
        val opponent = opponentScore.toIntOrNull() ?: 0

        when {
            user == 0 && opponent == 0 -> "" // Пусто если счет не введен
            user > opponent -> "Победа"
            user < opponent -> "Поражение"
            user == opponent -> "Ничья"
            else -> ""
        }
    }

    val resultColor = remember(userScore, opponentScore) {
        val user = userScore.toIntOrNull() ?: 0
        val opponent = opponentScore.toIntOrNull() ?: 0

        when {
            user == 0 && opponent == 0 -> Color.White // Белый если счет не введен
            user > opponent -> Color(0xFF4CAF50) // Зеленый для победы
            user < opponent -> Color(0xFFF44336) // Красный для поражения
            user == opponent -> Color(0xFFFF9800) // Оранжевый для ничьи
            else -> Color.White
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый бой", color = Color.White) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(25, 25, 33))
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Выбор соперника
            // Выбор соперника из выпадающего списка
            var expanded by remember { mutableStateOf(false) }

            // ПРАВИЛЬНОЕ ПОЛУЧЕНИЕ СПИСКА СОПЕРНИКОВ
            val opponentList = remember(opponentsState) {
                when (opponentsState) {
                    is UIState.Success -> {
                        val list = (opponentsState as UIState.Success<List<Opponent>>).data
                        println("DEBUG: Получен список из ${list.size} соперников")
                        list.forEachIndexed { i, opp ->
                            println("DEBUG: Соперник $i: ${opp.name} (id: ${opp.id})")
                        }
                        list
                    }
                    is UIState.Loading -> {
                        println("DEBUG: Загрузка списка соперников...")
                        emptyList()
                    }
                    is UIState.Error -> {
                        println("DEBUG: Ошибка загрузки: ${(opponentsState as UIState.Error).message}")
                        emptyList()
                    }
                    is UIState.Idle -> {
                        println("DEBUG: Состояние Idle")
                        emptyList()
                    }
                }
            }

            val selectedOpponent = opponentList.find { it.id == selectedOpponentId }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                OutlinedTextField(
                    value = selectedOpponent?.name ?: "Выберите соперника",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Соперник") },
                    leadingIcon = {
                        println("DEBUG url ="+ selectedOpponent?.avatarUrl.toString())
                        AsyncImage(
                            model =selectedOpponent?.avatarUrl,
                            contentDescription = "Аватар ${selectedOpponent?.name}",
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color.White, CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.avatar)
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        focusedContainerColor = Color(44, 44, 51),
                        unfocusedContainerColor = Color(44, 44, 51)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(20.dp)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    // Проверка, что список не пустой
                    if (opponentList.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Нет соперников") },
                            onClick = { expanded = false }
                        )
                    } else {
                        opponentList.forEach { opponent ->
                            DropdownMenuItem(
                                text = { Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AsyncImage(
                                        model = opponent.avatarUrl,
                                        contentDescription = "Аватар ${opponent.name}",
                                        modifier = Modifier
                                            .size(35.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color.White, CircleShape),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(id = R.drawable.avatar)
                                    )

                                    // Имя соперника
                                    Text(
                                        text = opponent.name,
                                    )
                                } },
                                onClick = {
                                    selectedOpponentId = opponent.id
                                    expanded = false
                                    println("DEBUG: Выбран соперник: ${opponent.name} (id: ${opponent.id})")
                                }
                            )
                        }
                    }
                }
            }

            if (resultText.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = resultText,
                        color = resultColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }else{
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Введите счёт",
                        color = Color(126, 126, 126, 255),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }


            // Ввод счета
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    value = userScore,
                    onValueChange = {
                            newValue ->
                        val filteredValue = newValue.filter { it.isDigit() }
                        userScore = filteredValue },
                    label = { Text("Ваши уколы", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        focusedContainerColor = Color(44, 44, 51),
                        unfocusedContainerColor = Color(44, 44, 51)
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp)
                )
                Text(
                    " : ",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = Color.White,
                    fontSize = 18.sp
                )
                OutlinedTextField(
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    value = opponentScore,
                    onValueChange = { newValue ->
                        val filteredValue = newValue.filter { it.isDigit() }
                        opponentScore = filteredValue},
                    label = { Text("Уколы соперника", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        focusedContainerColor = Color(44, 44, 51),
                        unfocusedContainerColor = Color(44, 44, 51)
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp)
                )
            }

            SimpleDatePickerButton(selectedDate = selectedDate, onDateSelected = { newDate ->
                    selectedDate = newDate
                })

            // Поле комментария
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Комментарий") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White,
                    focusedContainerColor = Color(44, 44, 51),
                    unfocusedContainerColor = Color(44, 44, 51)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                shape = RoundedCornerShape(20.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Кнопка сохранения
            Button(
                onClick = {
                    if (selectedOpponentId.isNotBlank() && userScore.isNotBlank() && opponentScore.isNotBlank()) {
                        val newBout = Bout(
                            opponentId = selectedOpponentId,
                            authorId = userId ?: "",
                            userScore = userScore.toIntOrNull() ?: 0,
                            opponentScore = opponentScore.toIntOrNull() ?: 0,
                            date = selectedDate,
                            comment = comment
                        )
                        boutViewModel.addBout(newBout)
                    }else {

                        // Показываем Snackbar через coroutineScope
                        coroutineScope.launch {
                            when {
                                selectedOpponentId.isBlank() ->
                                    snackbarHostState.showSnackbar("Выберите соперника")
                                userScore.isBlank() ->
                                    snackbarHostState.showSnackbar("Введите количество нанесенных уколов")
                                opponentScore.isBlank() ->
                                    snackbarHostState.showSnackbar("Введите количество полученных уколов")
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                enabled = addBoutState !is UIState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(139, 0, 0),
                    contentColor = Color.White,
                    disabledContainerColor = Color(139, 0, 0).copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (addBoutState is UIState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Добавить бой", fontSize = 15.sp, modifier = Modifier.padding(5.dp))
                }
            }
        }
    }
}
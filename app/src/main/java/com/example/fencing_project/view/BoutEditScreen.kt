
package com.example.fencing_project.view

import SimpleDatePickerButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.example.fencing_project.data.local.LocalBout
import com.example.fencing_project.data.local.LocalOpponent
import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.viewmodel.BoutViewModel
import com.example.fencing_project.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoutEditScreen(
    navController: NavController,



    pref: SharedPrefsManager,
    boutId: Long? = null,
    startOpponentId: Long? =0,
    boutViewModel: BoutViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val userId = pref.getUserId()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Состояния формы
    var selectedOpponentId by remember { mutableStateOf(startOpponentId?: 0) }
    var userScore by remember { mutableStateOf("") }
    var opponentScore by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val opponentsState by homeViewModel.opponents.collectAsState()
    val boutState by boutViewModel.boutState.collectAsState()
    val saveBoutState by boutViewModel.saveBoutState.collectAsState()
    val deleteBoutState by boutViewModel.deleteBoutState.collectAsState()

    // Состояния для диалогов
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    // Флаг для предотвращения повторного показа диалога
    var hasShownSuccessDialog by remember { mutableStateOf(false) }

    // Загружаем данные при открытии экрана
    LaunchedEffect(key1 = userId) {
        if (userId != null) {
            homeViewModel.loadUserData(userId)

            // Если передан boutId, загружаем данные боя для редактирования
            if (boutId != null) {
                boutViewModel.getBout(boutId)
            }
        }
    }

    // Заполняем форму данными боя при загрузке
    LaunchedEffect(boutState) {
        if (boutState is UIState.Success && boutId != null) {
            val bout = (boutState as UIState.Success<LocalBout>).data
            selectedOpponentId = bout.opponentId
            userScore = bout.userScore.toString()
            opponentScore = bout.opponentScore.toString()
            comment = bout.comment
            selectedDate = bout.date
        }
    }

    // Обработка состояния сохранения
    LaunchedEffect(saveBoutState) {
        when (saveBoutState) {
            is UIState.Success -> {
                if (!hasShownSuccessDialog) {
                    successMessage = if (boutId == null) "Бой успешно добавлен" else "Бой успешно обновлен"
                    showSuccessDialog = true
                    hasShownSuccessDialog = true

                    // Сбрасываем состояние после показа диалога
                    coroutineScope.launch {
                        delay(100) // Небольшая задержка
                        boutViewModel.resetSaveState()
                    }
                }
            }
            is UIState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Ошибка: ${(saveBoutState as UIState.Error).message}",
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short
                    )
                    boutViewModel.resetSaveState()
                }
            }
            else -> {}
        }
    }

    // Обработка состояния удаления
    LaunchedEffect(deleteBoutState) {
        when (deleteBoutState) {
            is UIState.Success -> {
                showDeleteConfirmationDialog = false
                // Сразу сбрасываем состояние
                boutViewModel.resetDeleteState()

                // Показываем диалог и сразу возвращаемся
                successMessage = "Бой успешно удален"
                showSuccessDialog = true
            }
            is UIState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Ошибка удаления: ${(deleteBoutState as UIState.Error).message}",
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short
                    )
                    boutViewModel.resetDeleteState()
                }
            }
            else -> {}
        }
    }

    // Диалог подтверждения удаления
    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                if (deleteBoutState !is UIState.Loading) {
                    showDeleteConfirmationDialog = false
                }
            },
            title = {
                Text(
                    "Удалить бой?",
                    color = Color.White
                )
            },
            text = {
                Text(
                    "Вы уверены, что хотите удалить этот бой? Это действие нельзя отменить.",
                    color = Color.White
                )
            },
            containerColor = Color(61, 61, 70),
            confirmButton = {
                TextButton(
                    onClick = {
                        if (boutId != null && deleteBoutState !is UIState.Loading) {
                            boutViewModel.deleteBout(boutId)
                            showDeleteConfirmationDialog = false
                        }
                    },
                    enabled = deleteBoutState !is UIState.Loading,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color(139,0,0),
                        contentColor = Color.White
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        boutViewModel.resetDeleteState()
                    },
                    enabled = deleteBoutState !is UIState.Loading,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color(44,44,51),
                        contentColor = Color.White
                    )
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог успешного действия
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                navController.popBackStack()
            },
            title = {
                Text(
                    "Успешно",
                    color = Color.White
                )
            },
            text = {
                Text(
                    successMessage,
                    color = Color.White
                )
            },
            containerColor = Color(61, 61, 70),
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(44, 44, 51),
                        contentColor = Color.White
                    )
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Расчет результата
    val resultText = remember(userScore, opponentScore) {
        val user = userScore.toIntOrNull() ?: 0
        val opponent = opponentScore.toIntOrNull() ?: 0

        when {
            user == 0 && opponent == 0 -> ""
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
            user == 0 && opponent == 0 -> Color.White
            user > opponent -> Color(0xFF4CAF50)
            user < opponent -> Color(0xFFF44336)
            user == opponent -> Color(0xFFFF9800)
            else -> Color.White
        }
    }

    // Заголовок экрана
    val screenTitle = if (boutId == null) "Новый бой" else "Редактировать бой"
    val buttonText = if (boutId == null) "Добавить бой" else "Сохранить изменения"

    // Проверяем, идет ли загрузка
    val isLoading = saveBoutState is UIState.Loading || deleteBoutState is UIState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(screenTitle, color = Color.White)

                },
                navigationIcon = {
                    IconButton(
                            onClick = { navController.popBackStack() },
                            enabled = !isLoading
                        ) {
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
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Получаем список соперников
                val opponentList = remember(opponentsState) {
                    when (opponentsState) {
                        is UIState.Success -> (opponentsState as UIState.Success<List<LocalOpponent>>).data
                        else -> emptyList()
                    }
                }

                // Выбор соперника
                val selectedOpponent = opponentList.find { it.id == selectedOpponentId }
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isLoading) expanded = !expanded },
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    OutlinedTextField(
                        value = selectedOpponent?.name ?: "Выберите соперника",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Соперник") },
                        leadingIcon = {
                            AsyncImage(
                                model = selectedOpponent?.avatarPath,
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
                        shape = RoundedCornerShape(20.dp),
                        enabled = !isLoading
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(44, 44, 51))
                    ) {
                        if (opponentList.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Нет соперников") },
                                onClick = { expanded = false }
                            )
                        } else {
                            opponentList.forEach { opponent ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            AsyncImage(
                                                model = opponent.avatarPath,
                                                contentDescription = "Аватар ${opponent.name}",
                                                modifier = Modifier
                                                    .size(35.dp)
                                                    .clip(CircleShape)
                                                    .border(1.dp, Color.White, CircleShape),
                                                contentScale = ContentScale.Crop,
                                                error = painterResource(id = R.drawable.avatar)
                                            )
                                            Text(
                                                text = opponent.name,
                                                color = Color.White
                                            )
                                        }
                                    },
                                    onClick = {
                                        if (!isLoading) {
                                            selectedOpponentId = opponent.id
                                            expanded = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedOpponentId!=0L) {
                    TextButton(
                        onClick = {
                            if (!isLoading) {
                                navController.navigate("edit_opponent/${selectedOpponentId}")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                            //.padding(horizontal = 10.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Подробнее о сопернике →",
                            fontSize = 14.sp
                        )
                    }
                }

                // Отображение результата
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (resultText.isNotEmpty()) {
                        Text(
                            text = resultText,
                            color = resultColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
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
                        onValueChange = { newValue ->
                            if (!isLoading) {
                                val filteredValue = newValue.filter { it.isDigit() }
                                userScore = filteredValue
                            }
                        },
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
                        shape = RoundedCornerShape(20.dp),
                        enabled = !isLoading
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
                            if (!isLoading) {
                                val filteredValue = newValue.filter { it.isDigit() }
                                opponentScore = filteredValue
                            }
                        },
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
                        shape = RoundedCornerShape(20.dp),
                        enabled = !isLoading
                    )
                }

                // Выбор даты
                SimpleDatePickerButton(
                    selectedDate = selectedDate,
                    onDateSelected = { newSelectedDate ->
                        if (!isLoading) selectedDate = newSelectedDate
                    },
                )

                // Поле комментария
                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (!isLoading) comment = it },
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
                    shape = RoundedCornerShape(20.dp),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Кнопка сохранения/добавления
                Button(
                    onClick = {
                        if (!isLoading && selectedOpponentId!=0L &&
                            userScore.isNotBlank() && opponentScore.isNotBlank()) {
                            val bout = LocalBout(
                                id = boutId?:0,
                                opponentId = selectedOpponentId,
                                authorId = userId ?: "",
                                userScore = userScore.toIntOrNull() ?: 0,
                                opponentScore = opponentScore.toIntOrNull() ?: 0,
                                date = selectedDate,
                                comment = comment
                            )

                            if (boutId == null) {
                                boutViewModel.addBout(bout)
                            } else {
                                boutViewModel.updateBout(bout)
                            }
                        } else if (!isLoading) {
                            coroutineScope.launch {
                                when {
                                    selectedOpponentId==0L ->
                                        snackbarHostState.showSnackbar(
                                            message = "Выберите соперника",
                                            duration = SnackbarDuration.Short
                                        )
                                    userScore.isBlank() ->
                                        snackbarHostState.showSnackbar(
                                            message = "Введите количество нанесенных уколов",
                                            duration = SnackbarDuration.Short
                                        )
                                    opponentScore.isBlank() ->
                                        snackbarHostState.showSnackbar(
                                            message = "Введите количество полученных уколов",
                                            duration = SnackbarDuration.Short
                                        )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    enabled = !isLoading && saveBoutState !is UIState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(139, 0, 0),
                        contentColor = Color.White,
                        disabledContainerColor = Color(139, 0, 0).copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                        Text(buttonText, fontSize = 15.sp, modifier = Modifier.padding(5.dp))

                }

                // Кнопка удаления (только при редактировании)
                if (boutId != null) {
                    Button(
                        onClick = {
                            if (!isLoading) {
                                showDeleteConfirmationDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        enabled = !isLoading && deleteBoutState !is UIState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF757575),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF757575).copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Удалить бой", fontSize = 15.sp, modifier = Modifier.padding(5.dp))
                    }
                }
            }

            // Полупрозрачный оверлей с индикатором загрузки
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(50.dp),
                            color = Color(139, 0, 0),
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = if (deleteBoutState is UIState.Loading) "Удаление..." else "Сохранение...",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
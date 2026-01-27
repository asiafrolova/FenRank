package com.example.fencing_project.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fencing_project.data.model.Bout
import com.example.fencing_project.data.model.Opponent
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.viewmodel.BoutViewModel
import com.example.fencing_project.viewmodel.HomeViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBoutScreen(
    navController: NavController,
    userId: String,
    boutViewModel: BoutViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel() // Чтобы получить список соперников
) {
    var selectedOpponentId by remember { mutableStateOf("") }
    var userScore by remember { mutableStateOf("") }
    var opponentScore by remember { mutableStateOf("") }
    var boutDate by remember { mutableStateOf(Date()) }
    var comment by remember { mutableStateOf("") }

    val opponents by homeViewModel.opponents.collectAsState()

    LaunchedEffect(key1 = userId) {
        homeViewModel.loadUserData(userId)
    }

    // Обработка состояния добавления боя
    val addBoutState by boutViewModel.addBoutState.collectAsState()
    LaunchedEffect(key1 = addBoutState) {
        when (addBoutState) {
            is UIState.Success -> {
                // Показать Snackbar и navigate back
                navController.popBackStack()
                boutViewModel.resetState()
            }
            is UIState.Error -> {
                // Показать ошибку
                boutViewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Новая запись боя") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Выбор соперника из выпадающего списка
                var expanded by remember { mutableStateOf(false) }
                val opponentList = (opponents as? UIState.Success<List<Opponent>>)?.data ?: emptyList()
                val selectedOpponent = opponentList.find { it.id == selectedOpponentId }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedOpponent?.name ?: "Выберите соперника",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Соперник") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        opponentList.forEach { opponent ->
                            DropdownMenuItem(
                                text = { Text(opponent.name) },
                                onClick = {
                                    selectedOpponentId = opponent.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                // Ввод счета
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userScore,
                        onValueChange = { userScore = it },
                        label = { Text("Ваши уколы") },
                        modifier = Modifier.weight(1f)
                    )
                    Text(" : ", modifier = Modifier.padding(horizontal = 8.dp))
                    OutlinedTextField(
                        value = opponentScore,
                        onValueChange = { opponentScore = it },
                        label = { Text("Уколы соперника") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                // Выбор даты (упрощенно - TextField)
                // Лучше использовать DatePicker
                OutlinedTextField(
                    value = boutDate.toString(), // Замените на форматированную дату
                    onValueChange = { /* Логика выбора даты */ },
                    label = { Text("Дата боя") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true // Для активации DatePicker по клику
                )
            }

            item {
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )
            }

            item {
                Button(
                    onClick = {
                        if (selectedOpponentId.isNotBlank() && userScore.isNotBlank() && opponentScore.isNotBlank()) {
                            val newBout = Bout(
                                opponentId = selectedOpponentId,
                                authorId = userId,
                                userScore = userScore.toIntOrNull() ?: 0,
                                opponentScore = opponentScore.toIntOrNull() ?: 0,
                                date = boutDate,
                                comment = comment
                            )
                            boutViewModel.addBout(newBout)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = addBoutState !is UIState.Loading
                ) {
                    if (addBoutState is UIState.Loading) {
                        CircularProgressIndicator()
                    } else {
                        Text("Сохранить запись")
                    }
                }
            }
        }
    }
}
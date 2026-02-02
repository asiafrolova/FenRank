package com.example.fencing_project.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.utils.formatDate
import com.example.fencing_project.viewmodel.SyncViewModel
import java.util.Date

@Composable
fun RestoreDataDialog(
    onDismiss: () -> Unit,
    viewModel: SyncViewModel = hiltViewModel()
) {
    val syncState by viewModel.syncState.collectAsState()
    val lastSyncDate by viewModel.lastSyncDate.collectAsState()
    val hasSyncData by viewModel.hasSyncData.collectAsState()

    // Состояние диалога: 0 - проверка, 1 - вопрос, 2 - загрузка, 3 - результат
    var dialogState by remember { mutableStateOf(0) }

    // При открытии диалога проверяем наличие резервной копии
    LaunchedEffect(Unit) {
        dialogState = 0
        if (syncState !is UIState.Loading) {
            viewModel.checkSyncData()
        }
    }

    // Обновляем состояние диалога в зависимости от состояния синхронизации
    LaunchedEffect(syncState) {
        when (syncState) {
            is UIState.Loading -> {
                if (lastSyncDate == 0L) {
                    dialogState = 0 // Проверка
                } else if (dialogState == 1) {
                    dialogState = 2 // Началась загрузка
                }
            }
            is UIState.Success -> {
                if (dialogState == 2) {
                    dialogState = 3 // Загрузка завершена
                } else if (dialogState == 0) {
                    dialogState = 1 // Проверка завершена, показываем вопрос
                }
            }
            is UIState.Error -> {
                if (dialogState == 2) {
                    dialogState = 3 // Загрузка завершилась с ошибкой
                }
            }
            else -> {}
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (dialogState != 2) { // Нельзя закрыть во время загрузки
                onDismiss()
                viewModel.resetState()
            }
        },
        title = {
            Text(
                when (dialogState) {
                    0 -> "Проверка..."
                    1 -> "Восстановление данных"
                    2 -> "Восстановление..."
                    3 -> "Результат"
                    else -> "Восстановление данных"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                when (dialogState) {
                    0 -> { // Проверка резервной копии
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(139, 0, 0),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Проверка резервной копии...", color = Color.White)
                        }
                    }

                    1 -> { // Вопрос о восстановлении
                        if (hasSyncData && lastSyncDate > 0) {
                            Column {
                                Text(
                                    text = "Найдена резервная копия от:",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = formatDate(Date(lastSyncDate)),
                                    color = Color(0xFF4CAF50),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    "Все текущие данные будут заменены данными из резервной копии.",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    "Это действие невозможно отменить.",
                                    color = Color(0xFFFF9800),
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    "Вы уверены, что хотите восстановить данные?",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "❌ Резервная копия не найдена",
                                    color = Color(0xFFF44336),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    "У вас нет сохраненных данных в облаке.",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    2 -> { // Идет загрузка
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color(139, 0, 0),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Восстановление данных...", color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Пожалуйста, не закрывайте приложение",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    3 -> { // Результат
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (syncState) {
                                is UIState.Success -> {
                                    Text(
                                        "✅ Данные успешно восстановлены!",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                is UIState.Error -> {
                                    Text(
                                        "❌ ${(syncState as UIState.Error).message}",
                                        color = Color(0xFFF44336),
                                        fontSize = 14.sp
                                    )
                                }

                                else -> {}
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(61, 61, 70),
        confirmButton = {
            when (dialogState) {
                1 -> { // Вопрос - показываем кнопки "Да" и "Нет"
                    if (hasSyncData && lastSyncDate > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    onDismiss()
                                    viewModel.resetState()
                                }
                            ) {
                                Text("Нет", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    viewModel.loadDataFromCloud()
                                    //viewModel.startBackgroundRestore()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(139, 0, 0),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Да, восстановить", fontSize = 14.sp)
                            }
                        }
                    } else {
                        // Если нет резервной копии, только кнопка "ОК"
                        Button(
                            onClick = {
                                onDismiss()
                                viewModel.resetState()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(139, 0, 0),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ОК", fontSize = 14.sp)
                        }
                    }
                }

                2 -> { // Загрузка - кнопка отключена
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(139, 0, 0).copy(alpha = 0.5f),
                            contentColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Восстановление...", fontSize = 14.sp)
                    }
                }

                3 -> { // Результат - кнопка "Закрыть"
                    Button(
                        onClick = {
                            onDismiss()
                            viewModel.resetState()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(139, 0, 0),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Закрыть", fontSize = 14.sp)
                    }
                }

                else -> {} // Для состояния 0 не показываем кнопки
            }
        },
        dismissButton = {
            when (dialogState) {
                0, 2 -> { // Во время проверки и загрузки скрываем кнопку отмены
                    // Не показываем dismissButton
                }
                else -> { // В других состояниях показываем
                    TextButton(
                        onClick = {
                            onDismiss()
                            viewModel.resetState()
                        },
                        enabled = dialogState != 2
                    ) {
                        Text(
                            when (dialogState) {
                                1 -> "Отмена"
                                else -> "Закрыть"
                            },
                            color = Color.White
                        )
                    }
                }
            }
        }
    )
}
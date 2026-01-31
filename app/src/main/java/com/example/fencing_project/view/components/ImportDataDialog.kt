// ImportDataDialog.kt
package com.example.fencing_project.view.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.viewmodel.ImportViewModel
import kotlinx.coroutines.launch

@Composable
fun ImportDataDialog(
    onDismiss: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel(),
    userId: String
) {
    val importState by viewModel.importState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Лончер для выбора файла
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    viewModel.importData(uri, userId)
                }
            }
        }
    )

    AlertDialog(
        onDismissRequest = {
            if (importState !is UIState.Loading) {
                onDismiss()
                viewModel.resetState()
            }
        },
        title = {
            Text("Импорт данных", color = Color.White)
        },
        text = {
            Column {
                Text(
                    "Выберите файл Excel для импорта данных:",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "• Файл должен содержать листы 'Соперники' и 'Бои'",
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text(
                    "• Формат файла должен соответствовать экспортированным данным",
                    color = Color.White,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (importState) {
                    is UIState.Loading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(139, 0, 0),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Импорт данных...", color = Color.White)
                        }
                    }

                    is UIState.Success -> {
                        val result = (importState as UIState.Success<ImportViewModel.ImportResult>).data
                        Column {
                            Text(
                                "✅ Импорт завершен успешно!",
                                color = Color(0xFF4CAF50),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Импортировано:",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Text(
                                "• Соперников: ${result.importedOpponents}",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Text(
                                "• Боев: ${result.importedBouts}",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    is UIState.Error -> {
                        Text(
                            "❌ ${(importState as UIState.Error).message}",
                            color = Color(0xFFF44336),
                            fontSize = 14.sp
                        )
                    }

                    else -> {}
                }
            }
        },
        containerColor = Color(61, 61, 70),
        confirmButton = {
            Button(
                onClick = {
                    if (importState !is UIState.Loading) {
                        // Открываем выбор файла
                        filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    }
                },
                enabled = importState !is UIState.Loading && importState !is UIState.Success,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(139,0,0),
                    contentColor = Color.White
                )
            ) {
                Text(
                    when (importState) {
                        is UIState.Success -> "Готово"
                        is UIState.Loading -> "Идет импорт..."
                        else -> "Выбрать файл"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (importState !is UIState.Loading) {
                        onDismiss()
                        viewModel.resetState()
                    }
                },
                enabled = importState !is UIState.Loading
            ) {
                Text("Отмена", color = Color.White)
            }
        }
    )
}
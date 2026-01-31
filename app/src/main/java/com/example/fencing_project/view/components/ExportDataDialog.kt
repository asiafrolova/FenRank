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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.viewmodel.ExportViewModel
import org.apache.poi.ss.formula.functions.Column

// ExportScreen.kt или диалог в SettingsScreen
@Composable
fun ExportDataDialog(
    onDismiss: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel<ExportViewModel>(),
    userId: String
) {
    val exportState by viewModel.exportState.collectAsState()

    AlertDialog(
        onDismissRequest = {
            if (exportState !is UIState.Loading) {
                onDismiss()
                viewModel.resetState()
            }
        },
        title = {
            Text("Экспорт данных", color = Color.White)
        },
        text = {
            Column {
                Text(
                    "Все данные будут экспортированы в файл Excel с двумя листами:",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    "• Соперники - полная информация о соперниках",
                    color = Color.White, fontSize = 12.sp
                )
                Text(
                    "• Бои - все бои с результатами",
                    color = Color.White, fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (exportState) {
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
                            Text("Создание файла...", color = Color.White)
                        }
                    }

                    is UIState.Success -> {
                        Text(
                            "✅ Файл успешно создан и сохранен в папке Загрузки",
                            color = Color(0xFF4CAF50),
                            fontSize = 14.sp
                        )
                    }

                    is UIState.Error -> {
                        Text(
                            "❌ ${(exportState as UIState.Error).message}",
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
                    if (exportState !is UIState.Loading) {
                        viewModel.exportData(userId)
                    }
                },
                enabled = exportState !is UIState.Loading && exportState !is UIState.Success,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(139, 0, 0),
                    contentColor = Color.White
                )
            ) {
                Text(
                    when (exportState) {
                        is UIState.Success -> "Готово"
                        else -> "Экспортировать"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (exportState !is UIState.Loading) {
                        onDismiss()
                        viewModel.resetState()
                    }
                },
                enabled = exportState !is UIState.Loading
            ) {
                Text("Отмена", color = Color.White)
            }
        }
    )
}
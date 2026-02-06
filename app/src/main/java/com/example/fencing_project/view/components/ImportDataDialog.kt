package com.example.fencing_project.view.components

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fencing_project.getString
import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.viewmodel.ImportViewModel
import kotlinx.coroutines.launch
import com.example.fencing_project.R

@Composable
fun ImportDataDialog(
    onDismiss: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel(),
    userId: String,
    context: Context,
    pref: SharedPrefsManager
) {
    val importState by viewModel.importState.collectAsState()
    val scope = rememberCoroutineScope()

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
            Text(getString(context,R.string.import_data,pref.getLanguage()), color = Color.White)
        },
        text = {
            Column {
                Text(
                    getString(context,R.string.choice_file_import,pref.getLanguage()),
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    getString(context,R.string.file_must,pref.getLanguage()),
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text(getString(context,R.string.file_must_format,pref.getLanguage()),
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
                            Text(getString(context,R.string.import_data_proccess,pref.getLanguage()), color = Color.White)
                        }
                    }

                    is UIState.Success -> {
                        val result = (importState as UIState.Success<ImportViewModel.ImportResult>).data
                        Column {
                            Text(
                                getString(context,R.string.import_data_successfull,pref.getLanguage()),
                                color = Color(0xFF4CAF50),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                getString(context,R.string.import_result,pref.getLanguage()),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Text(
                                getString(context,R.string.import_result_opponents,pref.getLanguage()) +" ${result.importedOpponents}",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Text(
                                getString(context,R.string.import_result_bouts,pref.getLanguage()) +" ${result.importedBouts}",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    is UIState.Error -> {
                        Text(
                            "${(importState as UIState.Error).message}",
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
                        is UIState.Success -> getString(context,R.string.ready,pref.getLanguage())
                        is UIState.Loading -> getString(context,R.string.import_proccess,pref.getLanguage())
                        else -> getString(context,R.string.add_file,pref.getLanguage())
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
                Text(getString(context,R.string.cancel,pref.getLanguage()), color = Color.White)
            }
        }
    )
}
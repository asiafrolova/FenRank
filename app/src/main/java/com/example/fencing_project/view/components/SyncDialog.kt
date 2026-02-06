package com.example.fencing_project.view.components

import android.content.Context
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fencing_project.R
import com.example.fencing_project.getString
import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.utils.formatDate
import com.example.fencing_project.viewmodel.SyncViewModel
import java.util.Date

@Composable
fun RestoreDataDialog(
    onDismiss: () -> Unit,
    viewModel: SyncViewModel = hiltViewModel(),
    context: Context,
    pref: SharedPrefsManager
) {
    val syncState by viewModel.syncState.collectAsState()
    val lastSyncDate by viewModel.lastSyncDate.collectAsState()
    val hasSyncData by viewModel.hasSyncData.collectAsState()
    var dialogState by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        dialogState = 0
        if (syncState !is UIState.Loading) {
            viewModel.checkSyncData()
        }
    }

    LaunchedEffect(syncState) {
        when (syncState) {
            is UIState.Loading -> {
                if (lastSyncDate == 0L) {
                    dialogState = 0
                } else if (dialogState == 1) {
                    dialogState = 2
                }
            }
            is UIState.Success -> {
                if (dialogState == 1) {
                    dialogState = 3
                } else if (dialogState == 0) {
                    dialogState = 1
                }
            }
            is UIState.Error -> {
                    dialogState = 3

            }
            else -> {}
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (dialogState != 2) {
                onDismiss()
                viewModel.resetState()
            }
        },
        title = {
            Text(
                when (dialogState) {
                    0 -> getString(context,R.string.checking,pref.getLanguage())
                    1 -> getString(context,R.string.recovery_data,pref.getLanguage())
                    3 -> getString(context,R.string.recovery_data,pref.getLanguage())
                    else ->  getString(context,R.string.recovery_data,pref.getLanguage())
                },
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                when (dialogState) {
                    0 -> {
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
                            Text(getString(context,R.string.recovery_check,pref.getLanguage()), color = Color.White)
                        }
                    }

                    1 -> {
                        if (hasSyncData && lastSyncDate > 0) {
                            Column {
                                Text(
                                    text = getString(context,R.string.recovery_found,pref.getLanguage()),
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
                                    getString(context,R.string.data_replace_recovery,pref.getLanguage()),
                                    color = Color.White,
                                    fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    getString(context,R.string.action_no_cancel,pref.getLanguage()),
                                    color = Color(0xFFFF9800),
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    getString(context,R.string.sure_recovery,pref.getLanguage()),
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
                                    getString(context,R.string.recovery_nout_found,pref.getLanguage()),
                                    color = Color(0xFFF44336),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    getString(context,R.string.no_recovry_cloud,pref.getLanguage()),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    3 -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (syncState) {
                                is UIState.Success -> {
                                    Text(
                                        getString(context,R.string.start_recovery_background,pref.getLanguage()),
                                        color = Color(0xFF4CAF50),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                is UIState.Error -> {
                                    Text(
                                        "${(syncState as UIState.Error).message}",
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
                1 -> {
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
                                Text(getString(context,R.string.no,pref.getLanguage()), color = Color.White)
                            }

                            Button(
                                onClick = {
                                    viewModel.startBackgroundRestore()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(139, 0, 0),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(getString(context,R.string.yes_recovery,pref.getLanguage()), fontSize = 14.sp)
                            }
                        }
                    } else {
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

                2 -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(139, 0, 0).copy(alpha = 0.5f),
                            contentColor = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(getString(context,R.string.recovery,pref.getLanguage()), fontSize = 14.sp)
                    }
                }

                3 -> {
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
                        Text(getString(context,R.string.close,pref.getLanguage()), fontSize = 14.sp)
                    }
                }

                else -> {}
            }
        },

    )
}

@Composable
fun SyncDataDialog(
    onDismiss: () -> Unit,
    viewModel: SyncViewModel = hiltViewModel(),
    text:String,
    dismissText:String,
    context: Context,
    pref: SharedPrefsManager
) {
    val syncState by viewModel.syncState.collectAsState()
    val lastSyncDate by viewModel.lastSyncDate.collectAsState()
    val hasSyncData by viewModel.hasSyncData.collectAsState()

    var dialogState by remember { mutableStateOf(1) }
    LaunchedEffect(syncState) {
        when (syncState) {

            is UIState.Success -> {
                if (dialogState == 1) {
                    dialogState = 3
                }
            }

            is UIState.Error -> {
                dialogState = 3

            }

            else -> {}
        }
    }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
            viewModel.resetState()

        },
        title = {
            Text(
                getString(context,R.string.save_data,pref.getLanguage()),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                when (dialogState) {


                    1 -> {

                        Text(
                            text,
                            color = Color.White
                        )

                    }

                    3 -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (syncState) {
                                is UIState.Success -> {
                                    Text(
                                        getString(context,R.string.start_save_data_background,pref.getLanguage()),
                                        color = Color(0xFF4CAF50),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                is UIState.Error -> {
                                    Text(
                                        "${(syncState as UIState.Error).message}",
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
                1 -> {

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
                                Text(dismissText, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    viewModel.startBackgroundSync()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(139, 0, 0),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(getString(context,R.string.yes_save_data,pref.getLanguage()), fontSize = 14.sp)
                            }
                        }

                }


                3 -> {
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
                        Text(getString(context,R.string.close,pref.getLanguage()), fontSize = 14.sp)
                    }
                }

                else -> {}
            }
        },

    )
}
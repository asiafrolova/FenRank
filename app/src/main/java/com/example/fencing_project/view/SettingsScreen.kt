package com.example.fencing_project.view

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fencing_project.R
import com.example.fencing_project.getString
import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.view.components.ExportDataDialog
import com.example.fencing_project.view.components.ImportDataDialog
import com.example.fencing_project.view.components.RestoreDataDialog
import com.example.fencing_project.view.components.SyncDataDialog
import com.example.fencing_project.viewmodel.ProfileViewModel
import com.example.fencing_project.viewmodel.SyncViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController,
                         pref: SharedPrefsManager,
                   viewModel: ProfileViewModel= hiltViewModel(),
                   syncViewModel : SyncViewModel = hiltViewModel(),

                  ){


    var showRestoreDialog by remember { mutableStateOf(false) }

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showSyncConfirmationDialog by remember {mutableStateOf(false)}
    var userId = pref.getUserId()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val menuItemsLanguages= listOf("English", "Русский")
    var language by remember { mutableStateOf(if(pref.getLanguage()=="en"){"English"}else{"Русский"} )}
    var expandedLanguage by remember { mutableStateOf(false) }


    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    val deleteState by viewModel.deleteProfileState.collectAsState()
    val context = LocalContext.current

    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    val deleteAccountState by viewModel.deleteProfileState.collectAsState()



    val syncState by syncViewModel.syncState.collectAsState()
    val isLoading = deleteAccountState is UIState.Loading || syncState is UIState.Loading

            LaunchedEffect(syncState) {
        if (syncState is UIState.Success || syncState is UIState.Error) {
        }
    }


    if (showRestoreDialog) {
        RestoreDataDialog(
            onDismiss = { showRestoreDialog = false },
            context=context,
            pref=pref
        )
    }

    if (showSyncConfirmationDialog) {
        SyncDataDialog(

            onDismiss = {
                showSyncConfirmationDialog = false
            },
            text = getString(context,R.string.you_want_sync,pref.getLanguage()),
            dismissText = getString(context,R.string.no,pref.getLanguage()),
            context=context,
            pref=pref
        )
    }


    if (showExportDialog && userId != null) {
        ExportDataDialog(
            onDismiss = { showExportDialog = false },
            userId = userId,
            context = context,
            pref=pref
        )
    }
    if (showImportDialog && userId != null) {
        ImportDataDialog(
            onDismiss = { showImportDialog = false },
            userId = userId,
            context = context,
            pref=pref
        )
    }

    LaunchedEffect(deleteAccountState) {
        when (deleteAccountState) {
            is UIState.Success -> {
                successMessage = getString(context,R.string.account_delete_successfull,pref.getLanguage())
                showSuccessDialog = true


                delay(2000)
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
            is UIState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = getString(context,R.string.error,pref.getLanguage()),
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short
                    )
                }
            }
            else -> {}
        }
    }


    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmationDialog = false

            },
            title = {
                Text(
                    getString(context,R.string.delete_account_forever,pref.getLanguage()),
                    color = Color.White
                )
            },
            text = {
                Text(
                    getString(context,R.string.you_sure_delete_account_forever,pref.getLanguage()),
                    color = Color.White
                )
            },
            containerColor = Color(61, 61, 70),
            confirmButton = {
                TextButton(
                    onClick = {

                        showDeleteConfirmationDialog = false

                    },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color(139, 0, 0),
                        contentColor =Color.White
                    )
                ) {
                    Text(getString(context,R.string.delete_btn,pref.getLanguage()))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmationDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color(44, 44, 51),
                        contentColor = Color.White
                    )
                ) {
                    Text(getString(context,R.string.cancel,pref.getLanguage()))
                }
            }
        )
    }


    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                password = ""
                viewModel.resetDeleteAccountState()
            },
            title = {
                Text(getString(context,R.string.enter_password_for_confirm,pref.getLanguage()), color = Color.White)
            },
            text = {
                Column {
                    Text(
                        getString(context,R.string.enter_password_for_delete,pref.getLanguage()),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {password = it },
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
                        placeholder = {Text(text=getString(context,R.string.enter_current_password,pref.getLanguage()), color=Color.White)},
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    tint = Color.White,
                                    painter = (if (showPassword) painterResource(R.drawable.face) else painterResource(R.drawable.fencing_mask)) ,
                                    contentDescription = if (showPassword) "Hide password" else "Show password",
                                    modifier = Modifier.padding(7.dp)
                                )
                            }
                        },
                    )
                }
            },
            containerColor = Color(61, 61, 70),
            confirmButton = {
                Button(
                    onClick = {
                        if (password.isNotBlank()) {
                            viewModel.deleteAccount(password)
                        }
                    },
                    enabled = password.isNotBlank() && deleteAccountState !is UIState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(139, 0, 0),
                        contentColor = Color.White
                    )
                ) {
                    if (deleteAccountState is UIState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(getString(context,R.string.delete_account,pref.getLanguage()))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        password = ""
                        viewModel.resetDeleteAccountState()
                    },
                    enabled = deleteAccountState !is UIState.Loading,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(getString(context,R.string.cancel,pref.getLanguage()))
                }
            }
        )
    }


    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                navController.popBackStack()
            },
            title = {
                Text(
                    getString(context,R.string.successfull,pref.getLanguage()),
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(getString(context,R.string.settings,pref.getLanguage()), color = Color.White) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },

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
                ExposedDropdownMenuBox(
                    expanded = expandedLanguage,
                    onExpandedChange = { expandedLanguage = !expandedLanguage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                ) {
                    OutlinedTextField(
                        value = language,
                        onValueChange = {

                        },
                        readOnly = true,
                        label = { Text(getString(context,R.string.language,pref.getLanguage())) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLanguage)
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
                    )

                    ExposedDropdownMenu(
                        expanded = expandedLanguage,
                        onDismissRequest = { expandedLanguage = false }
                    ) {
                        menuItemsLanguages.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    val langCode = if(item=="English"){"en"}else{"ru"}
                                    pref.setLanguage(langCode)
                                    language = item
                                    expandedLanguage = false
                                }
                            )
                        }
                    }
                }
                Button(

                    onClick = {

                        showPasswordDialog = true},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(133, 133, 133),
                        contentColor = Color.White,
                        disabledContainerColor = Color(133, 133, 133).copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !pref.isOffline()
                ) {
                    Text(getString(context,R.string.delete_account,pref.getLanguage()), fontSize = 15.sp, modifier = Modifier.padding(5.dp))

                }





                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(44, 44, 51)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = getString(context,R.string.export_data,pref.getLanguage()),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text =getString(context,R.string.save_you_data_excel,pref.getLanguage()),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            enabled = !pref.isOffline(),
                            onClick = { showExportDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(139,0,0),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.export),
                                contentDescription = "Экспорт",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(getString(context,R.string.export_excel,pref.getLanguage()))
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(44, 44, 51)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = getString(context,R.string.import_data,pref.getLanguage()),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = getString(context,R.string.load_data_excel,pref.getLanguage()),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            enabled = !pref.isOffline(),
                            onClick = { showImportDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(139,0,0),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.resource_import),
                                contentDescription = "Импорт",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(getString(context,R.string.import_excel,pref.getLanguage()))
                        }
                    }
                }





                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(44, 44, 51)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = getString(context,R.string.create_backup,pref.getLanguage()),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = getString(context,R.string.load_data_backup,pref.getLanguage()),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            enabled = !pref.isOffline(),
                            onClick = {
                                showSyncConfirmationDialog=true
                                 },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(139, 0, 0),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.sync),
                                contentDescription = getString(context,R.string.background_sync,pref.getLanguage()),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(getString(context,R.string.start_sync,pref.getLanguage()))
                        }
                    }
                }





                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(44, 44, 51)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = getString(context,R.string.automatic_sync,pref.getLanguage()),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = getString(context,R.string.set_up_sync,pref.getLanguage()),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            enabled = !pref.isOffline(),
                            onClick = { navController.navigate("sync") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(139, 0, 0),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.sync),
                                contentDescription = "Расписание",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(getString(context,R.string.set_up_schedule,pref.getLanguage()))
                        }
                    }
                }
                Button(
                    onClick = {
                        showRestoreDialog = true},
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(44, 44, 51)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    enabled = syncState !is UIState.Loading && !pref.isOffline()
                ) {
                    Text(getString(context,R.string.download_sync,pref.getLanguage()), fontSize = 16.sp)
                }

                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(painter = painterResource(R.drawable.github_invertocat_white_clearspace),
                        contentDescription = "github",
                        modifier = Modifier
                            .size(40.dp)
                            .padding(horizontal = 4.dp),
                        tint = Color.White)
                    Text(text="github.com/asiafrolova/FenRank", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp))


            }


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
                            text = getString(context,R.string.save,pref.getLanguage())
                            ,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
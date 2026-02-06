package com.example.fencing_project.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.example.fencing_project.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(navController: NavController,
                      pref: SharedPrefsManager,
                      viewModel: ProfileViewModel = hiltViewModel()){
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf(" ") }
    var currentEmail by remember { mutableStateOf("") }
    var oldPassword by remember { mutableStateOf("") }
    var oldEmailPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var successMessage by remember {  mutableStateOf("")}

    val updateEmailState by viewModel.updateEmailState.collectAsState()
    val updatePasswordState by viewModel.updatePasswordState.collectAsState()
    val updatePasswordStateSimple by viewModel.updatePasswordState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    var showSendEmailDialog by remember { mutableStateOf(false) }

    val isLoading = updatePasswordState is UIState.Loading || updateEmailState is UIState.Loading || updateState is UIState.Loading || updatePasswordStateSimple is UIState.Loading

    val user = viewModel.getCurrentUser()
    val context = LocalContext.current

    var showPassword = remember { mutableStateOf(false) }
    var retryShowPassword = remember { mutableStateOf(false) }
    var showOldPassword = remember { mutableStateOf(false) }
    var showOldEmailPassword = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        currentEmail = user?.email ?: ""
        email = currentEmail

    }
    LaunchedEffect(updatePasswordState) {
        when (updatePasswordState) {
            is UIState.Success -> {
                successMessage= getString(context,R.string.password_change_successfull,pref.getLanguage())
                showSuccessDialog = true
                viewModel.resetState()
            }
            is UIState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = getString(context,R.string.error,pref.getLanguage()),
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short
                    )
                    viewModel.resetState()
                }
            }
            else -> {}
        }
    }

    LaunchedEffect(updateEmailState) {
        when (updateEmailState) {
            is UIState.Success -> {
                successMessage= getString(context,R.string.email_change_successfull,pref.getLanguage())
                showSuccessDialog = true
                viewModel.resetState()
            }
            is UIState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = getString(context,R.string.error,pref.getLanguage()),
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short
                    )
                    viewModel.resetState()
                }
            }
            else -> {}
        }
    }
    LaunchedEffect(updateState) {
        when (updateState) {
            is UIState.Success -> {
                showSuccessDialog = true
                viewModel.resetState()
            }
            is UIState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message =getString(context,R.string.error,pref.getLanguage()),
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short
                    )
                    viewModel.resetState()
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
                    getString(context,R.string.clear_profile_image,pref.getLanguage()),
                    color = Color.White
                )
            },
            text = {
                Text(
                    getString(context,R.string.you_sure_clear_image,pref.getLanguage()),
                    color = Color.White
                )
            },
            containerColor = Color(61, 61, 70),
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteUserAvatar()
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


    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false

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
                title = { Text(getString(context,R.string.change_profile,pref.getLanguage()), color = Color.White) },
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
    ){innerPadding->
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
                OutlinedTextField(
                    value = email,
                    onValueChange = {email = it },
                    label = { Text(getString(context,R.string.email,pref.getLanguage())) },
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
                    readOnly = pref.isOffline()


                )
                OutlinedTextField(
                    value = oldEmailPassword,
                    onValueChange = {oldEmailPassword = it },
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
                    placeholder = {Text(text= getString(context,R.string.enter_current_password,pref.getLanguage()), color=Color.White)},
                    visualTransformation = if (showOldEmailPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showOldEmailPassword.value = !showOldEmailPassword.value }) {
                            Icon(
                                tint = Color.White,
                                painter = (if (showOldEmailPassword.value) painterResource(R.drawable.face) else painterResource(R.drawable.fencing_mask)) ,
                                contentDescription = if (showOldEmailPassword.value) "Hide password" else "Show password",
                                modifier = Modifier.padding(7.dp)
                            )
                        }
                    },
                    readOnly = pref.isOffline()
                )
                Button(

                    onClick = {

                        if(email.isNotBlank() && oldEmailPassword.isNotBlank()){
                            viewModel.updateEmail(
                                currentEmail = currentEmail,
                                currentPassword = oldEmailPassword,
                                newEmail = email,
                            )
                        }
                        else{
                            coroutineScope.launch {
                                when {
                                    email.isBlank() ->
                                        snackbarHostState.showSnackbar(
                                            message = getString(context,R.string.enter_email,pref.getLanguage()),
                                            duration = SnackbarDuration.Short
                                        )
                                    email.isNotBlank() && oldEmailPassword.isBlank()->
                                        snackbarHostState.showSnackbar(
                                            message = getString(context,R.string.enter_current_password,pref.getLanguage()),
                                            duration = SnackbarDuration.Short
                                        )

                                }
                            }
                        } },
                    enabled = (updateEmailState !is UIState.Loading)&&(!pref.isOffline()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(139, 0, 0),
                        contentColor = Color.White,
                        disabledContainerColor = Color(139, 0, 0).copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(getString(context,R.string.change_email,pref.getLanguage()), fontSize = 15.sp, modifier = Modifier.padding(5.dp))

                }
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = {oldPassword = it },
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
                    visualTransformation = if (showOldPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showOldPassword.value = !showOldPassword.value }) {
                            Icon(
                                tint = Color.White,
                                painter = (if (showOldPassword.value) painterResource(R.drawable.face) else painterResource(R.drawable.fencing_mask)) ,
                                contentDescription = if (showOldPassword.value) "Hide password" else "Show password",
                                modifier = Modifier.padding(7.dp)
                            )
                        }
                    },
                    readOnly = pref.isOffline()
                    )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {newPassword = it },
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
                    placeholder = {Text(text= getString(context,R.string.enter_new_password,pref.getLanguage()), color = Color.White)},
                    visualTransformation = if (showPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword.value = !showPassword.value }) {
                            Icon(
                                tint = Color.White,
                                painter = (if (showPassword.value) painterResource(R.drawable.face) else painterResource(R.drawable.fencing_mask)) ,
                                contentDescription = if (showPassword.value) "Hide password" else "Show password",
                                modifier = Modifier.padding(7.dp)
                            )
                        }
                    },
                    readOnly = pref.isOffline()

                )
                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = {confirmNewPassword = it },
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
                    placeholder = {Text(text= getString(context,R.string.confirm_new_password,pref.getLanguage()), color = Color.White)},
                    visualTransformation = if (retryShowPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { retryShowPassword.value = !retryShowPassword.value }) {
                            Icon(
                                tint = Color.White,
                                painter = (if (retryShowPassword.value) painterResource(R.drawable.face) else painterResource(R.drawable.fencing_mask)) ,
                                contentDescription = if (retryShowPassword.value) "Hide password" else "Show password",
                                modifier = Modifier.padding(7.dp)
                            )
                        }
                    },
                    readOnly = pref.isOffline()

                )
                val passwordError = remember(newPassword, confirmNewPassword) {
                    when {
                        newPassword.isNotEmpty() && newPassword.length < 6 -> getString(context,R.string.passwor_too_short,pref.getLanguage())
                        newPassword.isNotEmpty() && confirmNewPassword.isNotEmpty() &&
                                newPassword != confirmNewPassword -> getString(context,R.string.password_do_not_match,pref.getLanguage())
                        else -> null
                    }
                }
                passwordError?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
                TextButton(
                    onClick = {
                        viewModel.sendPasswordResetEmail(currentEmail)
                        successMessage=getString(context,R.string.change_password_email,pref.getLanguage())
                        showSuccessDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    ),
                    enabled = !pref.isOffline()

                ) {
                    Text(
                        getString(context,R.string.forgot_password,pref.getLanguage()),
                        fontSize = 14.sp,
                    )
                }
                Button(

                    onClick = {

                        if(newPassword.isNotBlank() && oldPassword.isNotBlank() && newPassword == confirmNewPassword &&
                            passwordError == null){
                            viewModel.updatePassword(
                                currentEmail = currentEmail,
                                currentPassword = oldPassword,
                                newPassword = newPassword
                            )
                        }
                        else{
                            coroutineScope.launch {
                                when {

                                    (oldPassword.isBlank() && newPassword.isNotBlank()) ->
                                        snackbarHostState.showSnackbar(
                                            message = getString(context,R.string.enter_current_password,pref.getLanguage()),
                                            duration = SnackbarDuration.Short
                                        )

                                }
                            }
                        } },
                    enabled = (updatePasswordState !is UIState.Loading && passwordError == null && !pref.isOffline()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(139, 0, 0),
                        contentColor = Color.White,
                        disabledContainerColor = Color(139, 0, 0).copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(getString(context,R.string.change_password,pref.getLanguage()), fontSize = 15.sp, modifier = Modifier.padding(5.dp))

                }
                Button(

                    onClick = {showDeleteConfirmationDialog=true},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(133, 133, 133),
                        contentColor = Color.White,
                        disabledContainerColor = Color(133, 133, 133).copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(getString(context,R.string.clear_profile_image,pref.getLanguage()), fontSize = 15.sp, modifier = Modifier.padding(5.dp))

                }


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
package com.example.fencing_project.view

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fencing_project.R
import com.example.fencing_project.Routes
import com.example.fencing_project.viewmodel.RegisterUiState
import com.example.fencing_project.viewmodel.RegisterViewModel
import kotlinx.coroutines.launch



@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "ViewModelConstructorInComposable")
@Composable
fun RegisterScreen(modifier: Modifier = Modifier, navController: NavController, viewModel: RegisterViewModel = hiltViewModel()){
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val registerState by viewModel.uiState.collectAsState()

    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val retryPassword = remember { mutableStateOf("") }

    // Реакция на состояние регистрации
    LaunchedEffect(registerState) {
        when (registerState) {
            is RegisterUiState.Success -> {
                scope.launch { snackbarHostState.showSnackbar("Регистрация успешна!") }
                navController.navigate(Routes.Login.route)
            }
            is RegisterUiState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar((registerState as RegisterUiState.Error).message)
                }
            }
            else -> {}
        }
    }


    Scaffold (snackbarHost = { SnackbarHost(snackbarHostState) }) {
        Box(modifier = Modifier.fillMaxSize().background(color = Color.Black)) {

            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(id = R.drawable.fon),
                contentDescription = null
            )
            var showPassword = remember { mutableStateOf(false) }
            var retryShowPassword = remember { mutableStateOf(false) }
            Column(modifier = modifier
                .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text(
                    stringResource(R.string.register_in),
                    modifier.padding(
                        vertical = 80.dp,
                        horizontal = 10.dp),
                    fontSize = 50.sp,
                    color = Color.White)
                OutlinedTextField(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = 0.dp,
                            top = 5.dp,
                            start = 55.dp,
                            end = 55.dp,
                        )
                        .height(60.dp),
                    value = email.value,
                    onValueChange = {email.value = it},
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = Color.White),
                    placeholder = {
                        Text(
                            stringResource(R.string.email),
                            color = Color.White) },
                    shape = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White,
                        cursorColor = Color.White,
                        focusedContainerColor = Color(107,107,107,175),
                        unfocusedContainerColor = Color(107,107,107,175),
                    )

                )
                OutlinedTextField(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(
                            bottom = 0.dp,
                            top = 5.dp,
                            start = 55.dp,
                            end = 55.dp,
                        ),

                    value = password.value,
                    visualTransformation = if (showPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword.value = !showPassword.value }) {
                            Icon(
                                tint = Color.White,
                                painter = (if (showPassword.value) painterResource(R.drawable.face) else painterResource(R.drawable.fencing_mask)) ,
                                contentDescription = if (showPassword.value) "Hide password" else "Show password",
                                modifier = modifier.padding(7.dp)
                            )
                        }
                    },
                    onValueChange = {password.value = it},
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = Color.White),
                    placeholder = {
                        Text(
                            stringResource(R.string.password),
                            color = Color.White)},
                    shape = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White,
                        cursorColor = Color.White,
                        focusedContainerColor = Color(107,107,107,175),
                        unfocusedContainerColor = Color(107,107,107,175),
                    )

                )

                OutlinedTextField(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(
                            bottom = 0.dp,
                            top = 5.dp,
                            start = 55.dp,
                            end = 55.dp,
                        ),

                    value = retryPassword.value,
                    visualTransformation = if (retryShowPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { retryShowPassword.value = !retryShowPassword.value }) {
                            Icon(
                                tint = Color.White,
                                painter = (if (retryShowPassword.value) painterResource(R.drawable.face) else painterResource(R.drawable.fencing_mask)) ,
                                contentDescription = if (retryShowPassword.value) "Hide password" else "Show password",
                                modifier = modifier.padding(7.dp)
                            )
                        }
                    },
                    onValueChange = {retryPassword.value = it},
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = Color.White),
                    placeholder = {
                        Text(
                            stringResource(R.string.retry_password),
                            color = Color.White)},
                    shape = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White,
                        cursorColor = Color.White,
                        focusedContainerColor = Color(107,107,107,175),
                        unfocusedContainerColor = Color(107,107,107,175),
                    )

                )
                Button(

                    onClick = {
                            viewModel.register(
                                email = email.value,
                                password = password.value,
                                confirmPassword = retryPassword.value
                            )
                              },
                    modifier = modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(
                            bottom = 0.dp,
                            top = 5.dp,
                            start = 55.dp,
                            end = 55.dp,
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(139, 0, 0),
                        disabledContainerColor = Color(197,137,137)
                    )
                ) {
                    if (registerState is RegisterUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            stringResource(R.string.register),
                            color = Color.White,
                            fontSize = 17.sp
                        )
                    }


                }
                Text(
                    stringResource(R.string.login_if_account),
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clickable {
                            navController.navigate(Routes.Login.route)
                    },
                    color = Color.White,
                    fontSize = 15.sp)

            }
        }
    }
}

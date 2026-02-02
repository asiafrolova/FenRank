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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fencing_project.MainActivity
import com.example.fencing_project.R
import com.example.fencing_project.Routes
import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.viewmodel.LoginUiState
import com.example.fencing_project.viewmodel.LoginViewModel
import com.example.fencing_project.viewmodel.ProfileViewModel
import com.example.fencing_project.viewmodel.RegisterUiState
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LoginScreen(modifier: Modifier = Modifier, navController: NavController, viewModel: LoginViewModel = hiltViewModel(),
                pref: SharedPrefsManager,profileViewModel: ProfileViewModel=hiltViewModel()){
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val coroutineScope = rememberCoroutineScope()
    var showSuccessDialog by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var uid by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()


    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.Success -> {
                val successState = uiState as LoginUiState.Success
                // Сохраняем данные пользователя
                pref.saveLoginState(
                    userId = successState.userId
                )
                println("DEBUG: Сохранен userId: ${successState.userId}")
                navController.navigate(Routes.Home.route) {
                    popUpTo(Routes.Login.route) { inclusive = true }
                }
                viewModel.resetState()
            }
            is LoginUiState.Offline -> {

                // Сохраняем данные пользователя
                pref.saveLocalLogin()
                println("DEBUG: Пользователь вошел оффлайн")
                navController.navigate(Routes.Home.route) {
                    popUpTo(Routes.Login.route) { inclusive = true }
                }
                viewModel.resetState()
            }
            is LoginUiState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar((uiState as LoginUiState.Error).message)
                }
                viewModel.resetState()
            }
            else -> {}
        }
    }
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false

            },
            title = {
                Text(
                    "Успешно",
                    color = Color.White
                )
            },
            text = {
                Text(
                    "На указанную почту отправлено письмо для сброса пароля",
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
    Scaffold (snackbarHost = { SnackbarHost(snackbarHostState) }) {

        Box(modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black)) {

            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(id = R.drawable.fon),
                contentDescription = null
            )
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            Column(modifier = modifier
                .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text(
                    stringResource(R.string.sign_in),
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
                    value = email,
                    onValueChange = {email = it},
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

                    value = password,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                tint = Color.White,
                                painter = (if (showPassword) painterResource(R.drawable.face) else painterResource(R.drawable.fencing_mask)) ,
                                contentDescription = if (showPassword) "Hide password" else "Show password",
                                modifier = modifier.padding(7.dp)
                            )
                        }
                    },
                    onValueChange = {password = it},
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
                Button(

                    onClick = {viewModel.login(email, password)},
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
                    if (uiState is LoginUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            stringResource(R.string.login),
                            color = Color.White,
                            fontSize = 17.sp)
                    }


                }
                TextButton(
                    onClick = {
                        if(email.isBlank()){
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Введите почту",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }else{
                            profileViewModel.sendPasswordResetEmail(email)
                            showSuccessDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )

                ) {
                    Text(
                        "Забыли пароль?",
                        fontSize = 14.sp,
                    )
                }}
                Column(modifier = Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Войти гостем",
                        modifier = Modifier
                            .padding(top = 10.dp, bottom = 10.dp)

                            .clickable {
                                viewModel.loginOffline()
                            },
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        stringResource(R.string.no_account_registr),
                        modifier = Modifier
                            .padding(top = 10.dp, bottom = 10.dp)

                            .clickable {
                                navController.navigate(Routes.Register.route)
                            },
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }

            }
        }
    }
}

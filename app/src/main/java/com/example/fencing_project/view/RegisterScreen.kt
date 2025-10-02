package com.example.fencing_project.view



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fencing_project.R

@Preview
@Composable
fun RegisterScreen(modifier: Modifier = Modifier){
    val email = remember{mutableStateOf("")}
    val password = remember{mutableStateOf("")}
    var showPassword = remember { mutableStateOf(false) }
    Column(modifier = modifier
        .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("Sign in",
            modifier.padding(
                vertical = 80.dp,
                horizontal = 10.dp),
            fontSize = 50.sp,
            color = Color.White)
        OutlinedTextField(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp,
                    horizontal = 55.dp)
                .height(55.dp),
            value = email.value,
            onValueChange = {email.value = it},
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = Color.White),
            placeholder = {
                Text("Email",
                color = Color.White) },
            shape = RoundedCornerShape(50.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White,
                cursorColor = Color.White
            )

        )
        OutlinedTextField(
            modifier = modifier.fillMaxWidth()
                .height(55.dp)
                .padding(vertical = 0.dp,
                    horizontal = 55.dp),
            value = password.value,
            visualTransformation = if (showPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword.value = !showPassword.value }) {
                    Icon(
                        painter = (if (showPassword.value) painterResource(R.drawable.eye_open) else painterResource(R.drawable.baseline_block_flipped_24)) ,
                        contentDescription = if (showPassword.value) "Hide password" else "Show password"
                    )
                }
            },
            onValueChange = {password.value = it},
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = Color.White),
            placeholder = {
                Text("Password",
                color = Color.White)},
            shape = RoundedCornerShape(50.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White,
                cursorColor = Color.White
            )

        )
        Button(

            onClick = {},
            modifier = modifier
                .fillMaxWidth()
                .height(59.dp)
                .padding(vertical = 7.dp,
                    horizontal = 55.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(139, 0, 0),
                disabledContainerColor = Color(197,137,137)
            )
            ) {
            Text("Login",
                color = Color.White,
                fontSize = 17.sp)
            
        }
    }

}
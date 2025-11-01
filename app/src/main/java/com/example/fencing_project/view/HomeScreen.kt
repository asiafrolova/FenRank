package com.example.fencing_project.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fencing_project.R
import com.example.fencing_project.Routes
import org.intellij.lang.annotations.JdkConstants

@Composable
fun HomeScreen(modifier: Modifier = Modifier, navController: NavController){
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Home",
            modifier = modifier
                .padding(50.dp),

            )
        Button(onClick = {navController.navigate(route = Routes.Register.route)}, modifier = modifier
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
            Text(
                stringResource(R.string.back),
                color = Color.White,
                fontSize = 17.sp)

        }
    }
}
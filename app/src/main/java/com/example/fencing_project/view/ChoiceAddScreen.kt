package com.example.fencing_project.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fencing_project.R
import com.example.fencing_project.getString
import com.example.fencing_project.utils.SharedPrefsManager

@Composable
fun ChoiceAddScreen(modifier: Modifier = Modifier, navController: NavController,
                    pref: SharedPrefsManager) {

    val context = LocalContext.current
    Scaffold(

    ){
            innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(139, 0, 0))
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {

            IconButton(onClick = {navController.navigate("add_opponent")},
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.TopCenter),
                colors = IconButtonColors(
                    containerColor = Color(139,0,0),
                    contentColor = Color.White,
                    disabledContainerColor = Color(139,0,0),
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.profile_ic),
                        contentDescription = "opponent",
                        modifier = Modifier.fillMaxSize(0.3f)
                    )

                    Text(getString(context,R.string.add_opponent,pref.getLanguage()), fontSize = 20.sp, modifier = Modifier.padding(vertical = 10.dp))
                }

            }

            IconButton(onClick = {navController.navigate("add_bout")},
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomCenter),
                colors = IconButtonColors(
                    containerColor = Color(139,0,0),
                    contentColor = Color.White,
                    disabledContainerColor = Color(139,0,0),
                    disabledContentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        painter = painterResource(R.drawable.bout),
                        contentDescription = "bout",
                        modifier = Modifier.fillMaxSize(0.3f)
                    )

                    Text(
                        getString(context,R.string.add_bout,pref.getLanguage()),
                        fontSize = 20.sp,
                        modifier = Modifier.padding(vertical = 30.dp)
                    )
                }

            }
            IconButton(onClick = {navController.popBackStack()},
                modifier = Modifier
                    .padding(10.dp)
                    .size(45.dp)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(10.dp)){
                Icon(painter = painterResource(R.drawable.back),
                    contentDescription = "back",
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp))
            }
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White)
                .align(Alignment.Center)){}
        }
    }
}
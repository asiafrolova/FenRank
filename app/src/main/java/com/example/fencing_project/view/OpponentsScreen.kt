package com.example.fencing_project.view

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fencing_project.R
import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.view.components.BottomNavigationBar
import com.example.fencing_project.viewmodel.LoginViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun OpponentsScreen(modifier: Modifier = Modifier, navController: NavController,
                    pref: SharedPrefsManager) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {FloatingActionButton(
            onClick = {navController.navigate("choice_add")},
            containerColor = Color(139,0,0),
            shape = CircleShape
        ) {
            Icon(painterResource(R.drawable.add_bout_ic),
                contentDescription = stringResource(R.string.add_bout),
                tint = Color.White,
                modifier = Modifier.size(20.dp,20.dp))
        }},
        bottomBar = { BottomNavigationBar(navController = navController) }
    ){
            innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(31,34,43))
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {}
    }
}
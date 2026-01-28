package com.example.fencing_project.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fencing_project.R
import com.example.fencing_project.data.model.Bout
import com.example.fencing_project.data.model.Opponent
import com.example.fencing_project.data.repository.AuthRepository
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.view.components.BottomNavigationBar
import com.example.fencing_project.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val userId:String = viewModel.currentUser()?.uid!!
    LaunchedEffect(key1 = userId) {
        if (userId.isNotBlank()) {
            viewModel.loadUserData(userId)
        }
    }

    val bouts by viewModel.bouts.collectAsState()
    val opponents by viewModel.opponents.collectAsState()

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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(31,34,43))
                .padding(innerPadding)
        ) {
            Text("Добро пожаловать в Engarde !", fontSize = 20.sp)

            Text("Последние записи", fontSize = 16.sp)


            // Отображение списка последних боев
            when (bouts) {
                is UIState.Loading -> Text("Загрузка...")
                is UIState.Success -> {
                    val boutsList = (bouts as UIState.Success<List<Bout>>).data
                    LazyColumn {
                        items(boutsList) { bout ->
                            // Найдем имя соперника по opponentId из списка opponents
                            val opponentName = (opponents as? UIState.Success<List<Opponent>>)
                                ?.data
                                ?.find { it.id == bout.opponentId }
                                ?.name ?: "Неизвестный соперник"

                            BoutItem(
                                userScore = bout.userScore,
                                opponentScore = bout.opponentScore,
                                opponentName = opponentName
                            )
                        }
                    }
                }
                is UIState.Error -> Text("Ошибка: ${(bouts as UIState.Error).message}")
                else -> {}
            }

        }


    }
}

// Простой Composable для отображения элемента боя
@Composable
fun BoutItem(userScore: Int, opponentScore: Int, opponentName: String) {
    Text("$userScore : $opponentScore - $opponentName")
}
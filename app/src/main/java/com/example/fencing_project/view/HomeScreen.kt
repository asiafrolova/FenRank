package com.example.fencing_project.view

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fencing_project.R
import com.example.fencing_project.data.local.LocalBout
import com.example.fencing_project.data.local.LocalOpponent
import com.example.fencing_project.getString
import com.example.fencing_project.utils.SharedPrefsManager


import com.example.fencing_project.utils.UIState
import com.example.fencing_project.view.components.BottomNavigationBar
import com.example.fencing_project.view.components.RestoreDataDialog
import com.example.fencing_project.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    navController: NavController,

    viewModel: HomeViewModel = hiltViewModel(),
    pref: SharedPrefsManager,

) {
    var showRestoreDialog by remember { mutableStateOf(!pref.isOffline()&&pref.isFirst()&&!pref.isRegistr()) }
    pref.setRegistr(false)

    val userId: String? = pref.getUserId()
    LaunchedEffect(key1 = userId) {
        if (userId != null && userId.isNotBlank()) {
            viewModel.loadUserData(userId)
        }
    }

    val boutsState by viewModel.bouts.collectAsState()
    val opponentsState by viewModel.opponents.collectAsState()

    val topOpponents = remember(opponentsState) {
        when (opponentsState) {
            is UIState.Success -> {
                val opponents = (opponentsState as UIState.Success<List<LocalOpponent>>).data
                opponents.sortedByDescending { it.totalBouts }
                    .take(3)
            }
            else -> emptyList()
        }
    }

    val latestBouts = remember(boutsState) {
        when (boutsState) {
            is UIState.Success -> {
                val bouts = (boutsState as UIState.Success<List<LocalBout>>).data
                bouts.sortedByDescending { it.date }
                    .take(5)
            }
            else -> emptyList()
        }
    }
    val context = LocalContext.current

    if (showRestoreDialog) {
        RestoreDataDialog(
            onDismiss = { showRestoreDialog = false },
            context=context,
            pref=pref
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("choice_add") },
                containerColor = Color(139, 0, 0),
                shape = CircleShape
            ) {
                Icon(
                    painterResource(R.drawable.add_bout_ic),
                    contentDescription = getString(context,R.string.add_bout,pref.getLanguage()),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp, 20.dp)
                )
            }
        },
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(25, 25, 33))
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = getString(context,R.string.welcome,pref.getLanguage()),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = getString(context,R.string.popular_opponents,pref.getLanguage()),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            when (opponentsState) {
                is UIState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(139, 0, 0))
                    }
                }

                is UIState.Success -> {
                    if (topOpponents.isEmpty()) {
                        EmptyStateOpponents(
                            text = getString(context,R.string.you_no_opponents,pref.getLanguage()),
                            modifier = Modifier.padding(vertical = 16.dp),
                            context,
                            pref
                        )
                    } else {
                        OpponentsListHome(
                            opponents = topOpponents,
                            onOpponentClick = { opponent ->
                                navController.navigate("edit_opponent/${opponent.id}")

                            },
                            context=context,
                            pref =pref,
                        )


                    }
                }

                is UIState.Error -> {
                    ErrorStateOpponents(
                        message = (opponentsState as UIState.Error).message,
                        onRetry = { userId?.let { viewModel.loadUserData(it) } },
                        modifier = Modifier.padding(vertical = 16.dp),
                        context,
                        pref
                    )
                }

                is UIState.Idle -> {
                    EmptyStateOpponents(
                        text = getString(context,R.string.start_add_opponents,pref.getLanguage()),
                        modifier = Modifier.padding(vertical = 16.dp),
                        context,
                        pref
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = getString(context,R.string.last_bouts,pref.getLanguage()),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            when (boutsState) {
                is UIState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(139, 0, 0))
                    }
                }

                is UIState.Success -> {
                    if (latestBouts.isEmpty()) {
                        EmptyStateBouts(
                            text = getString(context,R.string.you_no_bouts,pref.getLanguage()),
                            modifier = Modifier.padding(vertical = 16.dp),
                            context,
                            pref
                        )
                    } else {
                        val opponentsMap = when (opponentsState) {
                            is UIState.Success -> {
                                val opponentsList = (opponentsState as UIState.Success<List<LocalOpponent>>).data
                                opponentsList.associateBy { it.id }
                            }
                            else -> emptyMap()
                        }

                        BoutsListHome(
                            bouts = latestBouts,
                            opponents = (opponentsState as? UIState.Success<List<LocalOpponent>>)?.data ?: emptyList(),
                            onBoutClick = { bout ->
                                navController.navigate("edit_bout/${bout.id}")
                            },
                            context=context,
                            pref=pref,
                        )

                    }
                }

                is UIState.Error -> {
                    ErrorStateBouts(
                        message = (boutsState as UIState.Error).message,
                        onRetry = { userId?.let { viewModel.loadUserData(it) } },
                        modifier = Modifier.padding(vertical = 16.dp),
                        context,
                        pref
                    )
                }

                is UIState.Idle -> {
                    EmptyStateBouts(
                        text = getString(context,R.string.start_add_bouts,pref.getLanguage()),
                        modifier = Modifier.padding(vertical = 16.dp),
                        context,
                        pref
                    )
                }
            }
        }
    }
}

@Composable
private fun OpponentsListHome(
    opponents: List<LocalOpponent>,
    onOpponentClick: (LocalOpponent) -> Unit,
    modifier: Modifier = Modifier,
    context: Context,
    pref: SharedPrefsManager
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        opponents.forEach { opponent ->
            OpponentItem(
                opponent = opponent,
                onClick = { onOpponentClick(opponent) },
                context=context,
                pref=pref
            )
        }
    }
}

@Composable
private fun BoutsListHome(
    bouts: List<LocalBout>,
    opponents: List<LocalOpponent>,
    onBoutClick: (LocalBout) -> Unit,
    modifier: Modifier = Modifier,
    context:Context,
    pref: SharedPrefsManager
) {

    val groupedBouts = remember(bouts) {
        bouts.sortedByDescending { it.date }
            .groupBy { bout ->
                SimpleDateFormat("dd MMMM yyyy", Locale(pref.getLanguage())).format(Date(bout.date))
            }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        groupedBouts.forEach { (date, dateBouts) ->

            Text(
                text = date,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            dateBouts.forEach { bout ->
                val opponent = opponents.find { it.id == bout.opponentId }
                    BoutItem(
                    bout = bout,
                    opponent = opponent,
                    onClick = { onBoutClick(bout) },
                        context=context,
                        pref=pref
                )
            }
        }
    }
}


@Composable
private fun EmptyStateOpponents(
    text: String,
    modifier: Modifier = Modifier,
    context: Context,
    pref: SharedPrefsManager,
) {

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.fencing_mask),
            contentDescription = getString(context,R.string.empty,pref.getLanguage()),
            tint = Color(100, 100, 100),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = text,
            color = Color(150, 150, 150),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ErrorStateOpponents(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    context: Context,
    pref: SharedPrefsManager,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.error),
            contentDescription = getString(context,R.string.error_e,pref.getLanguage()),
            tint = Color(0xFFF44336),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            color = Color(0xFFF44336),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(139, 0, 0),
                contentColor = Color.White
            ),
            modifier = Modifier.height(36.dp)
        ) {
            Text(getString(context,R.string.retry,pref.getLanguage()), fontSize = 14.sp)
        }
    }
}

@Composable
private fun EmptyStateBouts(
    text: String,
    modifier: Modifier = Modifier,
    context: Context,
    pref: SharedPrefsManager,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.bout),
            contentDescription =getString(context,R.string.empty,pref.getLanguage()),
            tint = Color(100, 100, 100),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = text,
            color = Color(150, 150, 150),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun ErrorStateBouts(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    context: Context,
    pref: SharedPrefsManager,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.error),
            contentDescription = getString(context,R.string.error_e,pref.getLanguage()),
            tint = Color(0xFFF44336),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            color = Color(0xFFF44336),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(139, 0, 0),
                contentColor = Color.White
            ),
            modifier = Modifier.height(36.dp)
        ) {
            Text(getString(context,R.string.retry,pref.getLanguage()), fontSize = 14.sp)
        }
    }
}

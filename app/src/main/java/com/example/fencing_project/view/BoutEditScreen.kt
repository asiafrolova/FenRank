
package com.example.fencing_project.view

import SimpleDatePickerButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fencing_project.R
import com.example.fencing_project.data.local.LocalBout
import com.example.fencing_project.data.local.LocalOpponent
import com.example.fencing_project.getString
import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.viewmodel.BoutViewModel
import com.example.fencing_project.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoutEditScreen(
    navController: NavController,
    pref: SharedPrefsManager,
    boutId: Long? = null,
    startOpponentId: Long? =0,
    boutViewModel: BoutViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userId = pref.getUserId()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedOpponentId by remember { mutableStateOf(startOpponentId?: 0) }
    var userScore by remember { mutableStateOf("") }
    var opponentScore by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    val opponentsState by homeViewModel.opponents.collectAsState()
    val boutState by boutViewModel.boutState.collectAsState()
    val saveBoutState by boutViewModel.saveBoutState.collectAsState()
    val deleteBoutState by boutViewModel.deleteBoutState.collectAsState()
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var hasShownSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = userId) {
        if (userId != null) {
            homeViewModel.loadUserData(userId)
            if (boutId != null) {
                boutViewModel.getBout(boutId)
            }
        }
    }

    LaunchedEffect(boutState) {
        if (boutState is UIState.Success && boutId != null) {
            val bout = (boutState as UIState.Success<LocalBout>).data
            selectedOpponentId = bout.opponentId
            userScore = bout.userScore.toString()
            opponentScore = bout.opponentScore.toString()
            comment = bout.comment
            selectedDate = bout.date
        }
    }

    LaunchedEffect(saveBoutState) {
        when (saveBoutState) {
            is UIState.Success -> {
                if (!hasShownSuccessDialog) {
                    successMessage = if (boutId == null) getString(context,R.string.bout_add_successfull,pref.getLanguage()) else getString(
                        context,
                        R.string.bout_update_successfull,
                        pref.getLanguage()
                    )
                    showSuccessDialog = true
                    hasShownSuccessDialog = true
                    coroutineScope.launch {
                        delay(100)
                        boutViewModel.resetSaveState()
                    }
                }
            }
            is UIState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Ошибка: ${(saveBoutState as UIState.Error).message}",
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short
                    )
                    boutViewModel.resetSaveState()
                }
            }
            else -> {}
        }
    }

    LaunchedEffect(deleteBoutState) {
        when (deleteBoutState) {
            is UIState.Success -> {
                showDeleteConfirmationDialog = false
                boutViewModel.resetDeleteState()
                successMessage = getString(context,R.string.delete_successfull,pref.getLanguage())
                showSuccessDialog = true
            }
            is UIState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = getString(
                            context,
                            R.string.error_delete,
                            pref.getLanguage()
                        ),
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short
                    )
                    boutViewModel.resetDeleteState()
                }
            }
            else -> {}
        }
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                if (deleteBoutState !is UIState.Loading) {
                    showDeleteConfirmationDialog = false
                }
            },
            title = {
                Text(
                    getString(context,R.string.delete_bout_qs,pref.getLanguage()),
                    color = Color.White
                )
            },
            text = {
                Text(
                    getString(context,R.string.sure_delete_bout,pref.getLanguage()),
                    color = Color.White
                )
            },
            containerColor = Color(61, 61, 70),
            confirmButton = {
                TextButton(
                    onClick = {
                        if (boutId != null && deleteBoutState !is UIState.Loading) {
                            boutViewModel.deleteBout(boutId)
                            showDeleteConfirmationDialog = false
                        }
                    },
                    enabled = deleteBoutState !is UIState.Loading,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color(139,0,0),
                        contentColor = Color.White
                    )
                ) {
                    Text(getString(context,R.string.delete_btn,pref.getLanguage()))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        boutViewModel.resetDeleteState()
                    },
                    enabled = deleteBoutState !is UIState.Loading,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color(44,44,51),
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

    val resultText = remember(userScore, opponentScore) {
        val user = userScore.toIntOrNull() ?: 0
        val opponent = opponentScore.toIntOrNull() ?: 0

        when {
            user == 0 && opponent == 0 -> ""
            user > opponent -> getString(context,R.string.victory,pref.getLanguage())
            user < opponent -> getString(context,R.string.defeat,pref.getLanguage())
            user == opponent -> getString(context,R.string.draw,pref.getLanguage())
            else -> ""
        }
    }

    val resultColor = remember(userScore, opponentScore) {
        val user = userScore.toIntOrNull() ?: 0
        val opponent = opponentScore.toIntOrNull() ?: 0

        when {
            user == 0 && opponent == 0 -> Color.White
            user > opponent -> Color(0xFF4CAF50)
            user < opponent -> Color(0xFFF44336)
            user == opponent -> Color(0xFFFF9800)
            else -> Color.White
        }
    }

    val screenTitle = if (boutId == null) getString(context,R.string.new_bout,pref.getLanguage()) else getString(context,R.string.change_bout, pref.getLanguage())
    val buttonText = if (boutId == null) getString(context,R.string.add_bout,pref.getLanguage()) else getString(context,R.string.save_changes,pref.getLanguage())
    val isLoading = saveBoutState is UIState.Loading || deleteBoutState is UIState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(screenTitle, color = Color.White)

                },
                navigationIcon = {
                    IconButton(
                            onClick = { navController.popBackStack() },
                            enabled = !isLoading
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
                val opponentList = remember(opponentsState) {
                    when (opponentsState) {
                        is UIState.Success -> (opponentsState as UIState.Success<List<LocalOpponent>>).data
                        else -> emptyList()
                    }
                }

                val selectedOpponent = opponentList.find { it.id == selectedOpponentId }
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isLoading) expanded = !expanded },
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    OutlinedTextField(
                        value = selectedOpponent?.name ?: getString(context,R.string.choose_opponent,pref.getLanguage()),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(getString(context,R.string.opponent,pref.getLanguage())) },
                        leadingIcon = {
                            AsyncImage(
                                model = selectedOpponent?.avatarPath,
                                contentDescription = "Аватар ${selectedOpponent?.name}",
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color.White, CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.avatar)
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
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
                        enabled = !isLoading
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(44, 44, 51))
                    ) {
                        if (opponentList.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(getString(context,R.string.no_opponents,pref.getLanguage())) },
                                onClick = { expanded = false }
                            )
                        } else {
                            opponentList.forEach { opponent ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            AsyncImage(
                                                model = opponent.avatarPath,
                                                contentDescription = "Аватар ${opponent.name}",
                                                modifier = Modifier
                                                    .size(35.dp)
                                                    .clip(CircleShape)
                                                    .border(1.dp, Color.White, CircleShape),
                                                contentScale = ContentScale.Crop,
                                                error = painterResource(id = R.drawable.avatar)
                                            )
                                            Text(
                                                text = opponent.name,
                                                color = Color.White
                                            )
                                        }
                                    },
                                    onClick = {
                                        if (!isLoading) {
                                            selectedOpponentId = opponent.id
                                            expanded = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedOpponentId!=0L) {
                    TextButton(
                        onClick = {
                            if (!isLoading) {
                                navController.navigate("edit_opponent/${selectedOpponentId}")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(),

                        enabled = !isLoading,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = getString(context,R.string.more_about_the_opponent,pref.getLanguage()),
                            fontSize = 14.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (resultText.isNotEmpty()) {
                        Text(
                            text = resultText,
                            color = resultColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        Text(
                            text = getString(context,R.string.enter_account,pref.getLanguage()),
                            color = Color(126, 126, 126, 255),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        value = userScore,
                        onValueChange = { newValue ->
                            if (!isLoading) {
                                val filteredValue = newValue.filter { it.isDigit() }
                                userScore = filteredValue
                            }
                        },
                        label = { Text(getString(context,R.string.your_injections,pref.getLanguage()), fontSize = 11.sp) },
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
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        enabled = !isLoading
                    )
                    Text(
                        " : ",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    OutlinedTextField(
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        value = opponentScore,
                        onValueChange = { newValue ->
                            if (!isLoading) {
                                val filteredValue = newValue.filter { it.isDigit() }
                                opponentScore = filteredValue
                            }
                        },
                        label = { Text(getString(context,R.string.opponent_thrusts,pref.getLanguage()), fontSize = 11.sp) },
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
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        enabled = !isLoading
                    )
                }
                SimpleDatePickerButton(
                    selectedDate = selectedDate,
                    onDateSelected = { newSelectedDate ->
                        if (!isLoading) selectedDate = newSelectedDate
                    },
                    context=context,
                    pref=pref
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (!isLoading) comment = it },
                    label = { Text(getString(context,R.string.comment,pref.getLanguage())) },
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
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (!isLoading && selectedOpponentId!=0L &&
                            userScore.isNotBlank() && opponentScore.isNotBlank()) {
                            val bout = LocalBout(
                                id = boutId?:0,
                                opponentId = selectedOpponentId,
                                authorId = userId ?: "",
                                userScore = userScore.toIntOrNull() ?: 0,
                                opponentScore = opponentScore.toIntOrNull() ?: 0,
                                date = selectedDate,
                                comment = comment
                            )

                            if (boutId == null) {
                                boutViewModel.addBout(bout)
                            } else {
                                boutViewModel.updateBout(bout)
                            }
                        } else if (!isLoading) {
                            coroutineScope.launch {
                                when {
                                    selectedOpponentId==0L ->
                                        snackbarHostState.showSnackbar(
                                            message = getString(context,R.string.choose_an_opponent,pref.getLanguage()),
                                            duration = SnackbarDuration.Short
                                        )
                                    userScore.isBlank() ->
                                        snackbarHostState.showSnackbar(
                                            message = getString(context,R.string.enter_the_number_of_injections_administered,pref.getLanguage()),
                                            duration = SnackbarDuration.Short
                                        )
                                    opponentScore.isBlank() ->
                                        snackbarHostState.showSnackbar(
                                            message = getString(context,R.string.enter_the_number_of_injections_received,pref.getLanguage()),
                                            duration = SnackbarDuration.Short
                                        )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    enabled = !isLoading && saveBoutState !is UIState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(139, 0, 0),
                        contentColor = Color.White,
                        disabledContainerColor = Color(139, 0, 0).copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                        Text(buttonText, fontSize = 15.sp, modifier = Modifier.padding(5.dp))

                }

                if (boutId != null) {
                    Button(
                        onClick = {
                            if (!isLoading) {
                                showDeleteConfirmationDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        enabled = !isLoading && deleteBoutState !is UIState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF757575),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF757575).copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(getString(context,R.string.delete_bout,pref.getLanguage()), fontSize = 15.sp, modifier = Modifier.padding(5.dp))
                    }
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
                            text = if (deleteBoutState is UIState.Loading) getString(context,R.string.delete,pref.getLanguage()) else getString(context,R.string.save,pref.getLanguage()),
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
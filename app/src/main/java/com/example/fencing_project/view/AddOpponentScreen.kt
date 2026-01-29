//package com.example.fencing_project.view
//
//import android.annotation.SuppressLint
//import android.net.Uri
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.PickVisualMediaRequest
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonColors
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.DropdownMenu
//import androidx.compose.material3.DropdownMenuItem
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.ExposedDropdownMenuBox
//import androidx.compose.material3.ExposedDropdownMenuDefaults
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.IconButtonColors
//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.OutlinedTextFieldDefaults
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.SnackbarHost
//import androidx.compose.material3.SnackbarHostState
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextFieldColors
//import androidx.compose.material3.TextFieldDefaults
//import androidx.compose.material3.TopAppBar
//import androidx.compose.material3.TopAppBarColors
//import androidx.compose.material3.TopAppBarDefaults
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.MutableState
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.navigation.NavController
//import coil.compose.AsyncImage
//import com.example.fencing_project.utils.SharedPrefsManager
//import com.example.fencing_project.utils.UIState
//import com.example.fencing_project.viewmodel.HomeViewModel
//import kotlinx.coroutines.coroutineScope
//import kotlinx.coroutines.launch
//import com.example.fencing_project.R
//
//@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AddOpponentScreen(modifier: Modifier = Modifier, navController: NavController,
//                      pref: SharedPrefsManager,
//                      homeViewModel: HomeViewModel = hiltViewModel()
//) {
//    val coroutineScope = rememberCoroutineScope()
//    val snackbarHostState = remember { SnackbarHostState() }
//    var opponentName by remember { mutableStateOf("") }
//    var opponentWeaponHand by remember { mutableStateOf("") }
//    var opponentWeaponType by remember { mutableStateOf("") }
//    var opponentComment by remember { mutableStateOf("") }
//
//    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
//
//    var expandedWeaponHand by remember { mutableStateOf(false) }
//    var expandedWeaponType by remember { mutableStateOf(false) }
//
//    val menuItemsWeaponHand = listOf("Правая", "Левая")
//    val menuItemsWeaponType = listOf("Шпага", "Рапира", "Сабля")
//
//    val pickMedia = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.PickVisualMedia()
//    ) { uri ->
//        if (uri != null) {
//            selectedImageUri = uri
//            println("DEBUG: Выбрано изображение: $uri")
//        }
//    }
//
//
//    // Состояние добавления
//    val addState by homeViewModel.addOpponentState.collectAsState()
//
//    LaunchedEffect(key1 = addState) {
//
//        when (addState) {
//            is UIState.Success -> {
//                navController.popBackStack() // Возвращаемся назад после успеха
//                homeViewModel.resetAddOpponentState()
//            }
//            else -> {}
//        }
//    }
//
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Новый соперник", color = Color.White) },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(painter = painterResource(com.example.fencing_project.R.drawable.back),
//                            contentDescription = "back",
//                            tint = Color.White,
//                            modifier = Modifier.padding(10.dp))
//                    }
//                },
//                colors = TopAppBarColors(
//                    containerColor = Color(25,25,33),
//                    scrolledContainerColor = Color(25,25,33),
//                    navigationIconContentColor = Color.White,
//                    titleContentColor = Color.White,
//                    actionIconContentColor = Color.White,
//                    subtitleContentColor = Color.White
//                )
//            )
//        },
//        snackbarHost = { SnackbarHost(snackbarHostState) }
//    ) {innerPadding->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//                .background(Color(25,25,33))
//                .verticalScroll(rememberScrollState())
//            ,
//            verticalArrangement = Arrangement.spacedBy(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Box(modifier = Modifier.padding(10.dp).size(230.dp)) {
//                if (selectedImageUri != null) {
//                    AsyncImage(
//                        model = selectedImageUri,
//                        contentDescription = "Аватар соперника",
//                        modifier = Modifier
//                            .padding(
//                                top = 0.dp,
//                                end = 0.dp,
//                                start = 15.dp,
//                                bottom = 0.dp
//                            )
//                            .size(200.dp)
//                            .clip(CircleShape)
//                            .border(3.dp, Color.White, shape = CircleShape),
//                        placeholder = painterResource(R.drawable.avatar),
//                        error = painterResource(R.drawable.avatar)
//                    )
//                } else {
//                    Image(
//                        painter = painterResource(R.drawable.avatar),
//                        contentDescription = "Аватар по умолчанию",
//                        modifier = Modifier
//                            .padding(
//                                top = 0.dp,
//                                end = 0.dp,
//                                start = 15.dp,
//                                bottom = 0.dp
//                            )
//                            .size(200.dp)
//                            .clip(CircleShape)
//                            .border(3.dp, Color.White, shape = CircleShape)
//                    )
//                }
////                Image(
////                    painter = painterResource(com.example.fencing_project.R.drawable.avatar), contentDescription = "avatar",
////                    modifier = Modifier.padding(
////                        top = 0.dp,
////                        end = 0.dp,
////                        start = 15.dp,
////                        bottom = 0.dp
////                    ).size(200.dp).clip(CircleShape)
////                        .border(3.dp, Color.White, shape = CircleShape),
////                )
//
//                IconButton(
//                    onClick = {pickMedia.launch(
//                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
//                    )},
//                    modifier = Modifier.padding(top = 160.dp, start = 160.dp).size(45.dp),
//                    shape = CircleShape,
//                    colors = IconButtonColors(
//                        containerColor = Color.White,
//                        contentColor = Color.Black,
//                        disabledContainerColor = Color(130, 130, 130),
//                        disabledContentColor = Color.Black
//                    ),
//                    content = {
//                        Icon(
//                            painter = painterResource(com.example.fencing_project.R.drawable.edit_image),
//                            contentDescription = "editImage",
//                            modifier = Modifier.size(30.dp)
//                        )
//                    })
//
//            }
//            // Поле для имени
//            OutlinedTextField(
//                value = opponentName,
//                onValueChange = { opponentName = it },
//                label = { Text(text = "Имя соперника*") },
//                colors = OutlinedTextFieldDefaults.colors(
//                    focusedTextColor = Color.White,
//                    unfocusedTextColor = Color.White,
//                    cursorColor = Color.White,
//                    focusedBorderColor = Color.Transparent,
//                    unfocusedBorderColor = Color.Transparent,
//                    focusedLabelColor = Color.White,
//                    unfocusedLabelColor = Color.White,
//                    focusedContainerColor = Color(44,44,51),
//                    unfocusedContainerColor = Color(44,44,51)
//                ),
//                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
//                shape = RoundedCornerShape(20.dp)
//            )
//            // Поле для вооруженной руки с DropdownMenu
//            ExposedDropdownMenuBox(
//                expanded = expandedWeaponHand,
//                onExpandedChange = { expandedWeaponHand = !expandedWeaponHand },
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                OutlinedTextField(
//                    value = opponentWeaponHand,
//                    onValueChange = { opponentWeaponHand = it },
//                    readOnly = true,
//                    label = { Text("Вооруженная рука*") },
//                    trailingIcon = {
//                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWeaponHand)
//                    },
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedTextColor = Color.White,
//                        unfocusedTextColor = Color.White,
//                        cursorColor = Color.White,
//                        focusedBorderColor = Color.Transparent,
//                        unfocusedBorderColor = Color.Transparent,
//                        focusedLabelColor = Color.White,
//                        unfocusedLabelColor = Color.White,
//                        focusedContainerColor = Color(44,44,51),
//                        unfocusedContainerColor = Color(44,44,51)
//                    ),
//                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).menuAnchor(),
//                    shape = RoundedCornerShape(20.dp)
//                )
//
//                ExposedDropdownMenu(
//                    expanded = expandedWeaponHand,
//                    onDismissRequest = { expandedWeaponHand = false }
//                ) {
//                    menuItemsWeaponHand.forEach { item ->
//                        DropdownMenuItem(
//                            text = { Text(item) },
//                            onClick = {
//                                opponentWeaponHand = item
//                                expandedWeaponHand = false
//                            }
//                        )
//                    }
//                }
//            }
//            // Поле для вида оружия с DropdownMenu
//            ExposedDropdownMenuBox(
//                expanded = expandedWeaponType,
//                onExpandedChange = { expandedWeaponType = !expandedWeaponType },
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                OutlinedTextField(
//                    value = opponentWeaponType,
//                    onValueChange = { opponentWeaponType = it },
//                    readOnly = true,
//                    label = { Text("Вид оружия*") },
//                    trailingIcon = {
//                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWeaponType)
//                    },
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedTextColor = Color.White,
//                        unfocusedTextColor = Color.White,
//                        cursorColor = Color.White,
//                        focusedBorderColor = Color.Transparent,
//                        unfocusedBorderColor = Color.Transparent,
//                        focusedLabelColor = Color.White,
//                        unfocusedLabelColor = Color.White,
//                        focusedContainerColor = Color(44,44,51),
//                        unfocusedContainerColor = Color(44,44,51)
//                    ),
//                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).menuAnchor(),
//                    shape = RoundedCornerShape(20.dp)
//                )
//
//                ExposedDropdownMenu(
//                    expanded = expandedWeaponType,
//                    onDismissRequest = { expandedWeaponType = false }
//                ) {
//                    menuItemsWeaponType.forEach { item ->
//                        DropdownMenuItem(
//                            text = { Text(item) },
//                            onClick = {
//                                opponentWeaponType = item
//                                expandedWeaponType = false
//                            }
//                        )
//                    }
//                }
//            }
//            // Поле комментария
//            OutlinedTextField(
//                value = opponentComment,
//                onValueChange = { opponentComment = it },
//                label = { Text("Комментарий") },
//                colors = OutlinedTextFieldDefaults.colors(
//                    focusedTextColor = Color.White,
//                unfocusedTextColor = Color.White,
//                cursorColor = Color.White,
//                focusedBorderColor = Color.Transparent,
//                unfocusedBorderColor = Color.Transparent,
//                focusedLabelColor = Color.White,
//                unfocusedLabelColor = Color.White,
//                focusedContainerColor = Color(44,44,51),
//                unfocusedContainerColor = Color(44,44,51)
//            ),
//            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
//            shape = RoundedCornerShape(20.dp)
//            )
//
//            // Кнопка сохранения
//            Button(
//                onClick = {
//
//                    if (opponentName.isNotBlank() && opponentWeaponHand.isNotBlank() && opponentWeaponType.isNotBlank()) {
//                        homeViewModel.addOpponent(
//                            createdBy = pref.getUserId()!!,
//                            name = opponentName,
//                            weaponHand = opponentWeaponHand,
//                            weaponType = opponentWeaponType,
//                            comment = opponentComment,
//                            avatarUri = selectedImageUri
//                        )
//                    } else {
//
//                        // Показываем Snackbar через coroutineScope
//                        coroutineScope.launch {
//                            when {
//                                opponentName.isBlank() ->
//                                    snackbarHostState.showSnackbar("Введите имя соперника")
//                                opponentWeaponHand.isBlank() ->
//                                    snackbarHostState.showSnackbar("Выберите вооруженную руку")
//                                opponentWeaponType.isBlank() ->
//                                    snackbarHostState.showSnackbar("Выберите вид оружия")
//                            }
//                        }
//                    }
//                },
//                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 10.dp, end = 10.dp, bottom = 10.dp),
//                enabled = addState !is UIState.Loading && opponentName.isNotBlank(),
//                colors = ButtonColors(
//                    containerColor = Color(139,0,0),
//                    contentColor = Color.White,
//                    disabledContainerColor = Color(139,0,0),
//                    disabledContentColor = Color.White
//                ),
//                shape = RoundedCornerShape(20.dp)
//            ) {
//                if (addState is UIState.Loading) {
//                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
//                } else {
//                    Text("Добавить соперника", fontSize = 15.sp, modifier = Modifier.padding(5.dp))
//                }
//            }
//        }
//    }
//}
//
//

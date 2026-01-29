// OpponentEditScreen.kt
package com.example.fencing_project.view

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.fencing_project.R
import com.example.fencing_project.data.model.Opponent
import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.viewmodel.OpponentViewModel
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpponentEditScreen(
    navController: NavController,
    pref: SharedPrefsManager,
    opponentId: String? = null,
    opponentViewModel: OpponentViewModel = hiltViewModel()
) {
    val userId = pref.getUserId()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Состояния формы
    var opponentName by remember { mutableStateOf("") }
    var opponentWeaponHand by remember { mutableStateOf("") }
    var opponentWeaponType by remember { mutableStateOf("") }
    var opponentComment by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var expandedWeaponHand by remember { mutableStateOf(false) }
    var expandedWeaponType by remember { mutableStateOf(false) }

    val menuItemsWeaponHand = listOf("Правая", "Левая")
    val menuItemsWeaponType = listOf("Шпага", "Рапира", "Сабля")


    // Состояния ViewModel
    val opponentState by opponentViewModel.opponentState.collectAsState()
    val saveOpponentState by opponentViewModel.saveOpponentState.collectAsState()
    val deleteOpponentState by opponentViewModel.deleteOpponentState.collectAsState()

    // Состояния для диалогов
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    val currentOpponent = remember(opponentState) {
        when (opponentState) {
            is UIState.Success -> (opponentState as UIState.Success<Opponent>).data
            else -> null
        }
    }

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                selectedImageUri = uri
            }
        }
    }

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Запускаем обрезку после выбора фото
            val options = CropImageContractOptions(
                uri,
                CropImageOptions(
                    activityTitle = "Обрезать фото",
                    aspectRatioX = 1,
                    aspectRatioY = 1,
                    cropShape = CropImageView.CropShape.OVAL, // Круглая форма
                    fixAspectRatio = true,
                    guidelines = CropImageView.Guidelines.ON,
                    outputCompressFormat = Bitmap.CompressFormat.JPEG,
                    outputCompressQuality = 90
                )
            )
            cropLauncher.launch(options)
        }
    }

    // Загружаем данные при открытии экрана
    LaunchedEffect(key1 = opponentId) {
        if (opponentId != null) {
            opponentViewModel.getOpponent(opponentId)
        }
    }

    // Заполняем форму данными соперника при загрузке
    LaunchedEffect(opponentState) {
        if (opponentState is UIState.Success && opponentId != null) {
            val opponent = (opponentState as UIState.Success<Opponent>).data
            opponentName = opponent.name
            opponentWeaponHand = opponent.weaponHand
            opponentWeaponType = opponent.weaponType
            opponentComment = opponent.comment
        }
    }

    // Обработка состояния сохранения
    LaunchedEffect(saveOpponentState) {
        when (saveOpponentState) {
            is UIState.Success -> {
                successMessage = if (opponentId == null) "Соперник успешно добавлен" else "Соперник успешно обновлен"
                showSuccessDialog = true
                opponentViewModel.resetSaveState()
            }
            is UIState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Ошибка: ${(saveOpponentState as UIState.Error).message}",
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short
                    )
                    opponentViewModel.resetSaveState()
                }
            }
            else -> {}
        }
    }

    // Обработка состояния удаления
    LaunchedEffect(deleteOpponentState) {
        when (deleteOpponentState) {
            is UIState.Success -> {
                showDeleteConfirmationDialog = false
                opponentViewModel.resetDeleteState()
                successMessage = "Соперник успешно удален"
                showSuccessDialog = true
            }
            is UIState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Ошибка удаления: ${(deleteOpponentState as UIState.Error).message}",
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short
                    )
                    opponentViewModel.resetDeleteState()
                }
            }
            else -> {}
        }
    }

    // Диалог подтверждения удаления
    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                if (deleteOpponentState !is UIState.Loading) {
                    showDeleteConfirmationDialog = false
                }
            },
            title = {
                Text(
                    "Удалить соперника?",
                    color = Color.White
                )
            },
            text = {
                Text(
                    "Вы уверены, что хотите удалить этого соперника? Все связанные бои также будут удалены.",
                    color = Color.White
                )
            },
            containerColor = Color(61, 61, 70),
            confirmButton = {
                TextButton(
                    onClick = {
                        if (opponentId != null && deleteOpponentState !is UIState.Loading) {
                            opponentViewModel.deleteOpponentWithAvatar(opponentId, opponentAvatarUrl  = currentOpponent?.avatarUrl)
                            showDeleteConfirmationDialog = false
                        }
                    },
                    enabled = deleteOpponentState !is UIState.Loading,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color(139, 0, 0),
                        contentColor =Color.White
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        opponentViewModel.resetDeleteState()
                    },
                    enabled = deleteOpponentState !is UIState.Loading,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color(44, 44, 51),
                        contentColor = Color.White
                    )
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог успешного действия
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                navController.popBackStack()
            },
            title = {
                Text(
                    "Успешно",
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

    // Заголовок экрана
    val screenTitle = if (opponentId == null) "Новый соперник" else "Редактировать соперника"
    val buttonText = if (opponentId == null) "Добавить соперника" else "Сохранить изменения"

    // Проверяем, идет ли загрузка
    val isLoading = saveOpponentState is UIState.Loading || deleteOpponentState is UIState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle, color = Color.White) },
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.padding(10.dp).size(230.dp)) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Аватар соперника",
                            modifier = Modifier
                                .padding(
                                    top = 0.dp,
                                    end = 0.dp,
                                    start = 15.dp,
                                    bottom = 0.dp
                                )
                                .size(200.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color.White, shape = CircleShape),
                            placeholder = painterResource(R.drawable.avatar),
                            error = painterResource(R.drawable.avatar)
                        )
                    } else {
                        // Показываем загруженную аватарку из БД или аватар по умолчанию
                        AsyncImage(
                            model = (opponentState as? UIState.Success<Opponent>)?.data?.avatarUrl,
                            contentDescription = "Аватар соперника",
                            modifier = Modifier
                                .padding(
                                    top = 0.dp,
                                    end = 0.dp,
                                    start = 15.dp,
                                    bottom = 0.dp
                                )
                                .size(200.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color.White, shape = CircleShape),
                            placeholder = painterResource(R.drawable.avatar),
                            error = painterResource(R.drawable.avatar)
                        )
                    }

                    IconButton(
                        onClick = {pickMedia.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )},
                        modifier = Modifier.padding(top = 160.dp, start = 160.dp).size(45.dp),
                        shape = CircleShape,
                        colors = IconButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(130, 130, 130),
                            disabledContentColor = Color.Black
                        ),
                        content = {
                            Icon(
                                painter = painterResource(com.example.fencing_project.R.drawable.edit_image),
                                contentDescription = "editImage",
                                modifier = Modifier.size(30.dp)
                            )
                        })


                }

                if (opponentId != null && currentOpponent != null) {
                    TextButton(
                        onClick = {
                            navController.navigate("edit_bout_with/$opponentId")
                        },
                        modifier = Modifier
                            .fillMaxWidth(),

                        enabled = !isLoading,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )

                    ) {
                        Text(
                            "Добавить бой с этим соперником  →",
                            fontSize = 14.sp,
                        )
                    }
                }

                // Поле для имени
                OutlinedTextField(
                    value = opponentName,
                    onValueChange = { if (!isLoading) opponentName = it },
                    label = { Text("Имя соперника*") },
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !isLoading
                )

                // Поле для вооруженной руки с DropdownMenu
                ExposedDropdownMenuBox(
                    expanded = expandedWeaponHand,
                    onExpandedChange = { if (!isLoading) expandedWeaponHand = !expandedWeaponHand },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                ) {
                    OutlinedTextField(
                        value = opponentWeaponHand,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Вооруженная рука*") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWeaponHand)
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
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(20.dp),
                        enabled = !isLoading
                    )

                    ExposedDropdownMenu(
                        expanded = expandedWeaponHand,
                        onDismissRequest = { expandedWeaponHand = false }
                    ) {
                        menuItemsWeaponHand.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    opponentWeaponHand = item
                                    expandedWeaponHand = false
                                }
                            )
                        }
                    }
                }

                // Поле для вида оружия с DropdownMenu
                ExposedDropdownMenuBox(
                    expanded = expandedWeaponType,
                    onExpandedChange = { if (!isLoading) expandedWeaponType = !expandedWeaponType },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                ) {
                    OutlinedTextField(
                        value = opponentWeaponType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Вид оружия*") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWeaponType)
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
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(20.dp),
                        enabled = !isLoading
                    )

                    ExposedDropdownMenu(
                        expanded = expandedWeaponType,
                        onDismissRequest = { expandedWeaponType = false }
                    ) {
                        menuItemsWeaponType.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    opponentWeaponType = item
                                    expandedWeaponType = false
                                }
                            )
                        }
                    }
                }

                // Поле комментария
                OutlinedTextField(
                    value = opponentComment,
                    onValueChange = { if (!isLoading) opponentComment = it },
                    label = { Text("Комментарий") },
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Кнопка сохранения/добавления
                Button(
                    onClick = {
                        if (!isLoading && opponentName.isNotBlank() &&
                            opponentWeaponHand.isNotBlank() && opponentWeaponType.isNotBlank()) {

                            if (userId != null) {
                                if (opponentId == null) {
                                    // Добавление
                                    opponentViewModel.addOpponent(
                                        createdBy = userId,
                                        name = opponentName,
                                        weaponHand = opponentWeaponHand,
                                        weaponType = opponentWeaponType,
                                        comment = opponentComment,
                                        avatarUri = selectedImageUri
                                    )
                                } else {
                                    // Обновление
                                    opponentViewModel.updateOpponent(
                                        opponentId = opponentId,
                                        name = opponentName,
                                        weaponHand = opponentWeaponHand,
                                        weaponType = opponentWeaponType,
                                        comment = opponentComment,
                                        avatarUri = selectedImageUri
                                    )
                                }
                            }
                        } else if (!isLoading) {
                            coroutineScope.launch {
                                when {
                                    opponentName.isBlank() ->
                                        snackbarHostState.showSnackbar(
                                            message = "Введите имя соперника",
                                            duration = SnackbarDuration.Short
                                        )
                                    opponentWeaponHand.isBlank() ->
                                        snackbarHostState.showSnackbar(
                                            message = "Выберите вооруженную руку",
                                            duration = SnackbarDuration.Short
                                        )
                                    opponentWeaponType.isBlank() ->
                                        snackbarHostState.showSnackbar(
                                            message = "Выберите вид оружия",
                                            duration = SnackbarDuration.Short
                                        )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 0.dp, top=10.dp, start=10.dp,end=10.dp),
                    enabled = !isLoading && saveOpponentState !is UIState.Loading,
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

                // Кнопка удаления (только при редактировании)
                if (opponentId != null) {
                    Button(
                        onClick = {
                            if (!isLoading) {
                                showDeleteConfirmationDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start=10.dp, end=10.dp, bottom=10.dp,top=10.dp),
                        enabled = !isLoading && deleteOpponentState !is UIState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF757575),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF757575).copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Удалить соперника", fontSize = 15.sp, modifier = Modifier.padding(5.dp))
                    }
                }
            }

            // Полупрозрачный оверлей с индикатором загрузки
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
                            text = when {
                                deleteOpponentState is UIState.Loading -> "Удаление..."
                                saveOpponentState is UIState.Loading ->
                                    if (opponentId == null) "Добавление..." else "Сохранение..."
                                else -> "Загрузка..."
                            },
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
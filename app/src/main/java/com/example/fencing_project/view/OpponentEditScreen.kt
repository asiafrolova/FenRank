package com.example.fencing_project.view

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.fencing_project.data.local.LocalOpponent
import com.example.fencing_project.getString

import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.viewmodel.OpponentViewModel
import io.ktor.http.websocket.websocketServerAccept
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpponentEditScreen(
    navController: NavController,
    pref: SharedPrefsManager,
    opponentId: Long? = null,
    opponentViewModel: OpponentViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val userId = pref.getUserId()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var opponentName by remember { mutableStateOf("") }
    var opponentWeaponHand by remember { mutableStateOf("") }
    var opponentWeaponType by remember { mutableStateOf("") }
    var opponentComment by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var expandedWeaponHand by remember { mutableStateOf(false) }
    var expandedWeaponType by remember { mutableStateOf(false) }

    val menuItemsWeaponHand = listOf(getString(context,R.string.right,pref.getLanguage()),
       getString(context,R.string.left,pref.getLanguage()))
    val menuItemsWeaponType = listOf(getString(context,R.string.epee,pref.getLanguage()),
        getString(context,R.string.foil,pref.getLanguage()), getString(context,R.string.sabre,pref.getLanguage())
    )

    val opponentState by opponentViewModel.opponentState.collectAsState()
    val saveOpponentState by opponentViewModel.saveOpponentState.collectAsState()
    val deleteOpponentState by opponentViewModel.deleteOpponentState.collectAsState()

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    val currentOpponent = remember(opponentState) {
        when (opponentState) {
            is UIState.Success -> (opponentState as UIState.Success<LocalOpponent>).data
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
            val options = CropImageContractOptions(
                uri,
                CropImageOptions(
                    activityTitle = getString(context
                            ,R.string.crop_image, pref.getLanguage()),
                    aspectRatioX = 1,
                    aspectRatioY = 1,
                    cropShape = CropImageView.CropShape.OVAL,
                    fixAspectRatio = true,
                    guidelines = CropImageView.Guidelines.ON,
                    outputCompressFormat = Bitmap.CompressFormat.JPEG,
                    outputCompressQuality = 90
                )
            )
            cropLauncher.launch(options)
        }
    }

    LaunchedEffect(key1 = opponentId) {
        if (opponentId != null) {
            opponentViewModel.getOpponent(opponentId)
        }
    }

    LaunchedEffect(opponentState) {
        if (opponentState is UIState.Success && opponentId != null) {
            val opponent = (opponentState as UIState.Success<LocalOpponent>).data

            opponentName = opponent.name
            opponentWeaponHand = if(opponent.weaponHand=="right"){getString(context,R.string.right,pref.getLanguage())}else{getString(context,R.string.left,pref.getLanguage())}
            opponentWeaponType = if(opponent.weaponType=="epee"){getString(context,R.string.epee,pref.getLanguage())}else if(opponent.weaponType=="foil"){getString(context,R.string.foil,pref.getLanguage())}else{getString(context,R.string.sabre,pref.getLanguage())}
            opponentComment = opponent.comment
        }
    }

    LaunchedEffect(saveOpponentState) {
        when (saveOpponentState) {
            is UIState.Success -> {
                successMessage = if (opponentId == null) getString(context,R.string.add_opponent_successfull, pref.getLanguage()) else getString(
                    context,
                    R.string.change_opponent_successfull,
                    pref.getLanguage()
                )
                showSuccessDialog = true
                opponentViewModel.resetSaveState()
            }
            is UIState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = getString(context,R.string.error,pref.getLanguage()),
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short
                    )
                    opponentViewModel.resetSaveState()
                }
            }
            else -> {}
        }
    }


    LaunchedEffect(deleteOpponentState) {
        when (deleteOpponentState) {
            is UIState.Success -> {
                showDeleteConfirmationDialog = false
                opponentViewModel.resetDeleteState()
                successMessage = getString(context,R.string.delete_opponent_successfull,pref.getLanguage())
                showSuccessDialog = true
            }
            is UIState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = getString(context,
                            R.string.error_delete,
                            pref.getLanguage()
                        ),
                        actionLabel = "OK",
                        duration = SnackbarDuration.Short
                    )
                    opponentViewModel.resetDeleteState()
                }
            }
            else -> {}
        }
    }


    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                if (deleteOpponentState !is UIState.Loading) {
                    showDeleteConfirmationDialog = false
                }
            },
            title = {
                Text(
                    getString(context,R.string.delete_opponent_qs,pref.getLanguage()),
                    color = Color.White
                )
            },
            text = {
                Text(
                    getString(context,R.string.you_sure_delete_opponent,pref.getLanguage()),
                    color = Color.White
                )
            },
            containerColor = Color(61, 61, 70),
            confirmButton = {
                TextButton(
                    onClick = {
                        if (opponentId != null && deleteOpponentState !is UIState.Loading) {
                            opponentViewModel.deleteOpponentWithAvatar(opponentId, opponentAvatarUrl  = currentOpponent?.avatarPath)
                            showDeleteConfirmationDialog = false
                        }
                    },
                    enabled = deleteOpponentState !is UIState.Loading,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color(139, 0, 0),
                        contentColor =Color.White
                    )
                ) {
                    Text(getString(context,R.string.delete_btn,pref.getLanguage()))
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

    val screenTitle = if (opponentId == null) getString(context,R.string.new_opponent, pref.getLanguage()) else getString(
        context,
        R.string.change_opponent,
        pref.getLanguage()
    )
    val buttonText = if (opponentId == null) getString(context,R.string.add_opponent,pref.getLanguage()) else getString(context,R.string.save_changes,pref.getLanguage())
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
                Box(modifier = Modifier
                    .padding(10.dp)
                    .size(230.dp)) {
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

                        AsyncImage(
                            model = (opponentState as? UIState.Success<LocalOpponent>)?.data?.avatarPath,
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
                        modifier = Modifier
                            .padding(top = 160.dp, start = 160.dp)
                            .size(45.dp),
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
                            getString(context,R.string.add_bout_with_opponent,pref.getLanguage()),
                            fontSize = 14.sp,
                        )
                    }
                }

                OutlinedTextField(
                    value = opponentName,
                    onValueChange = { if (!isLoading) opponentName = it },
                    label = { Text(getString(context,R.string.name_opponent,pref.getLanguage())) },
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

                ExposedDropdownMenuBox(
                    expanded = expandedWeaponHand,
                    onExpandedChange = { if (!isLoading) expandedWeaponHand = !expandedWeaponHand },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                ) {
                    OutlinedTextField(
                        value = opponentWeaponHand,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(getString(context,R.string.weapon_hand,pref.getLanguage())) },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
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

                ExposedDropdownMenuBox(
                    expanded = expandedWeaponType,
                    onExpandedChange = { if (!isLoading) expandedWeaponType = !expandedWeaponType },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                ) {
                    OutlinedTextField(
                        value = opponentWeaponType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(getString(context,R.string.weapon_type,pref.getLanguage())) },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
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

                OutlinedTextField(
                    value = opponentComment,
                    onValueChange = { if (!isLoading) opponentComment = it },
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
                        if (!isLoading && opponentName.isNotBlank() &&
                            opponentWeaponHand.isNotBlank() && opponentWeaponType.isNotBlank()) {
                            if (userId != null) {
                                val opponentHand = if(opponentWeaponHand==getString(context,R.string.right,pref.getLanguage())){"right"}else{"left"}
                                val opponentType = if(opponentWeaponType==getString(context,R.string.epee,pref.getLanguage())){"epee"}else if(opponentWeaponType==getString(context,R.string.foil,pref.getLanguage())){"foil"}else{"sabre"}
                                if (opponentId == null) {
                                    opponentViewModel.addOpponent(
                                        createdBy = userId,
                                        name = opponentName,
                                        weaponHand = opponentHand,
                                        weaponType = opponentType,
                                        comment = opponentComment,
                                        avatarUri = selectedImageUri
                                    )
                                } else {
                                    opponentViewModel.updateOpponent(
                                        opponentId = opponentId,
                                        name = opponentName,
                                        weaponHand = opponentHand,
                                        weaponType = opponentType,
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
                                            message = getString(context,R.string.enter_name_opponent,pref.getLanguage()),
                                            duration = SnackbarDuration.Short
                                        )
                                    opponentWeaponHand.isBlank() ->
                                        snackbarHostState.showSnackbar(
                                            message = getString(context,R.string.enter_weapon_hand,pref.getLanguage()),
                                            duration = SnackbarDuration.Short
                                        )
                                    opponentWeaponType.isBlank() ->
                                        snackbarHostState.showSnackbar(
                                            message = getString(context,R.string.enter_weapon_type,pref.getLanguage()),
                                            duration = SnackbarDuration.Short
                                        )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 0.dp, top = 10.dp, start = 10.dp, end = 10.dp),
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

                if (opponentId != null) {
                    Button(
                        onClick = {
                            if (!isLoading) {
                                showDeleteConfirmationDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = 10.dp),
                        enabled = !isLoading && deleteOpponentState !is UIState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF757575),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF757575).copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(getString(context,R.string.delete_opponent,pref.getLanguage()), fontSize = 15.sp, modifier = Modifier.padding(5.dp))
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
                            text = when {
                                deleteOpponentState is UIState.Loading -> getString(context,R.string.delete,pref.getLanguage())
                                saveOpponentState is UIState.Loading ->
                                    getString(context,R.string.save,pref.getLanguage())
                                else -> getString(context,R.string.save,pref.getLanguage())
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
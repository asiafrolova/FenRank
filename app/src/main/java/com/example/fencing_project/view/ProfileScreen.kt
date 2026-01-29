package com.example.fencing_project.view

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
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
import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.view.components.BottomNavigationBar
import com.example.fencing_project.viewmodel.ProfileViewModel


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ProfileScreen(modifier: Modifier = Modifier, navController: NavController,
                  pref: SharedPrefsManager, viewModel: ProfileViewModel = hiltViewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val updateState by viewModel.updateState.collectAsState()
    val currentUser = remember { viewModel.getCurrentUser() }
    val userAvatarUrl by viewModel.userAvatarUrl.collectAsState()
    println("DEBUG: userAvatarUrl = ${userAvatarUrl}")

    var showImagePicker by remember { mutableStateOf(false) }

    // Image Picker with Crop (как раньше)
    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                viewModel.updateUserAvatar(uri)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val options = CropImageContractOptions(
                it,
                CropImageOptions().apply {
                    cropShape = CropImageView.CropShape.OVAL
                    fixAspectRatio = true
                    aspectRatioX = 1
                    aspectRatioY = 1
                    activityTitle = "Обрезать фото"
                }
            )
            cropLauncher.launch(options)
        }
    }

    // Обработка состояния
    LaunchedEffect(updateState) {
        when (updateState) {
            is UIState.Success -> {
                // Snackbar или toast
                viewModel.resetState()
            }
            is UIState.Error -> {
                // Ошибка
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(

        bottomBar = { BottomNavigationBar(navController = navController) }
    ){
            innerPadding ->
        Box(
    modifier = Modifier
        .fillMaxSize()
        .background(Color(25,25,33))
        .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.5f)
                    .fillMaxWidth()
                    .background(Color(139, 0, 0))
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.padding(10.dp).size(230.dp)) {
                    AsyncImage(
                        model = "https://"+userAvatarUrl,
                        contentDescription = "Аватар",
                        modifier = Modifier.padding(
                            top = 0.dp,
                            end = 0.dp,
                            start = 15.dp,
                            bottom = 0.dp
                        ).size(200.dp).clip(CircleShape)
                            .border(3.dp, Color.White, shape = CircleShape),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.avatar),
                        placeholder = painterResource(R.drawable.avatar)
                    )


                    IconButton(
                        onClick = {galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
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
                                painter = painterResource(R.drawable.edit_image),
                                contentDescription = "editImage",
                                modifier = Modifier.size(30.dp)
                            )
                        })

                }
            }

            Box(
                modifier = Modifier.padding(end = 20.dp, start = 20.dp, bottom = 20.dp)
                    .fillMaxWidth().fillMaxHeight(0.55f)
                    .align(Alignment.BottomCenter)
                    .dropShadow(
                        shape = RoundedCornerShape(20.dp),
                        shadow = androidx.compose.ui.graphics.shadow.Shadow(
                            radius = 10.dp,
                            spread = 6.dp,
                            color = Color(0x40000000),
                            offset = DpOffset(x = 4.dp, 4.dp)
                        )
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(31, 34, 43)),
                contentAlignment = Alignment.TopCenter
            ) {


                @Composable
                fun OptionItem(
                    icon: Painter,
                    title: String,
                    onClick: () -> Unit = {},
                ) {

                    Button(
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonColors(
                            containerColor = Color(31,34,43),
                            contentColor = Color.White,
                            disabledContainerColor =  Color(25,25,33),
                            disabledContentColor = Color.White
                        ),
                        onClick={onClick()},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp)

                    ) {

                        Icon(
                            painter = icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    OptionItem(
                        icon = painterResource(R.drawable.change_profile),
                        title = "Редактировать профиль",

                        )
                    OptionItem(
                        icon = painterResource(R.drawable.change_password),
                        title = "Сменить пароль",

                        )
                    OptionItem(
                        icon = painterResource(R.drawable.statistics), title = "Статистика",

                        )
                    OptionItem(
                        icon = painterResource(R.drawable.settings), title = "Настройки",

                        )
                }
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    OptionItem(
                        icon = painterResource(R.drawable.logout), title = "Выйти из аккаунта",
                        onClick = {

                            pref.logout()
                            navController.navigate("login")}
                        )
                }
            }
        }

    }
}
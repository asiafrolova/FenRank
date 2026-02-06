package com.example.fencing_project.view

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.example.fencing_project.getString
import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.utils.UIState
import com.example.fencing_project.view.components.BottomNavigationBar
import com.example.fencing_project.view.components.SyncDataDialog
import com.example.fencing_project.viewmodel.ProfileViewModel
import com.example.fencing_project.viewmodel.SyncViewModel



@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ProfileScreen(modifier: Modifier = Modifier, navController: NavController,
                  pref: SharedPrefsManager, viewModel: ProfileViewModel = hiltViewModel(),
                  syncViewModel: SyncViewModel = hiltViewModel()) {
    val updateState by viewModel.updateState.collectAsState()
    val userAvatarUrl by viewModel.userAvatarUrl.collectAsState()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var forceRefresh by remember { mutableStateOf(0) }
    var showExitConfirmationDialog by remember { mutableStateOf(false) }
    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                selectedImageUri = uri
                viewModel.updateUserAvatar(uri)

            }
        }
    }
    val context = LocalContext.current




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

    LaunchedEffect(updateState) {
        when (updateState) {
            is UIState.Success -> {
                forceRefresh++
                viewModel.resetState()
            }
            is UIState.Error -> {
                viewModel.resetState()
            }
            else -> {}
        }
    }

    if (showExitConfirmationDialog) {
        SyncDataDialog(onDismiss = {showExitConfirmationDialog = false
            viewModel.logout()
            pref.logout()
            navController.navigate("login")},
            text= getString(context,R.string.you_want_sync_before_exit,pref.getLanguage()),
            dismissText = getString(context,R.string.exit,pref.getLanguage()),
            context=context,
            pref=pref
            )

    }

        Scaffold(

            bottomBar = { BottomNavigationBar(navController = navController) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(25, 25, 33))
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
                    Box(modifier = Modifier
                        .padding(10.dp)
                        .size(230.dp)) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Аватар",
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
                                contentScale = ContentScale.Crop,
                                error = painterResource(R.drawable.avatar),
                                placeholder = painterResource(R.drawable.avatar),


                                )
                        } else {
                            AsyncImage(
                                model = userAvatarUrl,
                                contentDescription = "Аватар",
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
                                contentScale = ContentScale.Crop,
                                error = painterResource(R.drawable.avatar),
                                placeholder = painterResource(R.drawable.avatar),


                                )
                        }


                        IconButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
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
                                    painter = painterResource(R.drawable.edit_image),
                                    contentDescription = "editImage",
                                    modifier = Modifier.size(30.dp)
                                )
                            })

                    }
                }

                Box(
                    modifier = Modifier
                        .padding(end = 20.dp, start = 20.dp, bottom = 20.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(0.55f)
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
                                containerColor = Color(31, 34, 43),
                                contentColor = Color.White,
                                disabledContainerColor = Color(25, 25, 33),
                                disabledContentColor = Color.White
                            ),
                            onClick = { onClick() },
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        OptionItem(
                            icon = painterResource(R.drawable.change_profile),
                            title = getString(context,R.string.change_profile,pref.getLanguage()),
                            onClick = { navController.navigate("profile_edit") }
                        )

                        OptionItem(
                            icon = painterResource(R.drawable.statistics), title = getString(context,R.string.statistics,pref.getLanguage()),
                            onClick = { navController.navigate("statistics") }
                        )
                        OptionItem(
                            icon = painterResource(R.drawable.settings), title = getString(context,R.string.settings,pref.getLanguage()),
                            onClick = { navController.navigate("settings") }
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        OptionItem(
                            icon = painterResource(R.drawable.logout), title = getString(context,R.string.exit_from_profile,pref.getLanguage()),
                            onClick = {
                                if(!pref.isOffline()) {
                                    showExitConfirmationDialog = true
                                }else{
                                    viewModel.logout()
                                    pref.logout()
                                    navController.navigate("login")
                                }
                            }
                        )
                    }
                }
            }

        }
    }
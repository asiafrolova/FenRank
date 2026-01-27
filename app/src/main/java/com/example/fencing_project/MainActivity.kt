package com.example.fencing_project

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import androidx.navigation.NavController
import androidx.navigation.NavHost
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fencing_project.ui.theme.Fencing_projectTheme
import com.example.fencing_project.utils.SharedPrefsManager
import com.example.fencing_project.view.HomeScreen
import com.example.fencing_project.view.LoginScreen
import com.example.fencing_project.view.OpponentsScreen
import com.example.fencing_project.view.ProfileScreen
import com.example.fencing_project.view.RegisterScreen
import dagger.hilt.android.AndroidEntryPoint

sealed class Routes(val route: String) {

    object Home : Routes("home")
    object Login : Routes("login")
    object Register : Routes("register")
    object Opponents : Routes("opponents")
    object Profile : Routes("profile")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = SharedPrefsManager(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color(139,0,0).toArgb(),
                Color(139,0,0).toArgb()
            )
        )
        setContent {
            var startRout:String = Routes.Login.route
            if (sharedPrefs.isLoggedIn()){
                startRout = Routes.Home.route
            }
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = startRout) {
                composable(Routes.Login.route) {
                    LoginScreen(navController = navController, pref = sharedPrefs)
                }
                composable(Routes.Register.route) {
                    RegisterScreen(navController = navController)
                }
                composable(Routes.Home.route) {
                    HomeScreen(
                        navController = navController,

                    )
                }
                composable(Routes.Profile.route) {
                    ProfileScreen(navController = navController, pref = sharedPrefs)
                }
                composable(Routes.Opponents.route) {
                    OpponentsScreen(navController = navController, pref = sharedPrefs)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Fencing_projectTheme {
        Greeting("Android")
    }
}


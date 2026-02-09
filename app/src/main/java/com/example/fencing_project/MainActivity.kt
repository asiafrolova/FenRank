package com.example.fencing_project

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fencing_project.ui.theme.Fencing_projectTheme
import com.example.fencing_project.utils.SharedPrefsManager

import com.example.fencing_project.view.BoutEditScreen

import com.example.fencing_project.view.ChoiceAddScreen
import com.example.fencing_project.view.HomeScreen
import com.example.fencing_project.view.LoginScreen
import com.example.fencing_project.view.OpponentEditScreen
import com.example.fencing_project.view.OpponentsScreen
import com.example.fencing_project.view.ProfileEditScreen
import com.example.fencing_project.view.ProfileScreen
import com.example.fencing_project.view.RegisterScreen
import com.example.fencing_project.view.SettingsScreen
import com.example.fencing_project.view.StatisticsScreen
import com.example.fencing_project.view.SyncScreen
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.client.plugins.HttpRequestRetry
import java.util.Locale


sealed class Routes(val route: String) {

    object Home : Routes("home")
    object Login : Routes("login")
    object Register : Routes("register")
    object Opponents : Routes("opponents")
    object Profile : Routes("profile")
    object ChoiceAdd : Routes("choice_add")
    object AddBoutScreen : Routes("add_bout")
    object AddOpponentScreen : Routes("add_opponent")
    object EditBout : Routes("edit_bout/{boutId}")
    object EditOpponent : Routes("edit_opponent/{opponentId}")
    object AddBoutWithOpponent : Routes("edit_bout_with/{opponentId}")
    object ProfileEdit : Routes("profile_edit")
    object Settings: Routes("settings")
    object Statistics : Routes("statistics")
    object Sync : Routes("sync")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val REQUEST_CODE_NOTIFICATION = 1001
        private const val REQUEST_CODE_BACKGROUND = 1002
        private const val REQUEST_CODE_ALARM = 1003
    }
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestAllPermissions()
        }

        installSplashScreen()

        val sharedPrefs = SharedPrefsManager(this)
        val lang = sharedPrefs.getLanguage()
        val locale: Locale = Locale(lang)
        Locale.setDefault(locale)
        val config: Configuration = Configuration()
        config.locale = locale
        getResources().updateConfiguration(
            config,
            getResources().getDisplayMetrics()
        )
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
                    RegisterScreen(navController = navController, pref = sharedPrefs)
                }
                composable(Routes.Home.route) {

                    HomeScreen(
                        navController = navController,
                        pref = sharedPrefs
                    )
                }

                composable(Routes.Profile.route) {
                    ProfileScreen(navController = navController, pref = sharedPrefs)
                }
                composable(Routes.Opponents.route) {
                    OpponentsScreen(navController = navController, pref = sharedPrefs)
                }
                composable (Routes.ChoiceAdd.route){
                    ChoiceAddScreen(navController = navController, pref = sharedPrefs)
                }
                composable(Routes.AddBoutScreen.route) {
                    BoutEditScreen(navController =navController, pref = sharedPrefs)
                }
                composable (Routes.AddOpponentScreen.route){
                    OpponentEditScreen(navController=navController, pref = sharedPrefs)
                }
                composable(Routes.EditBout.route) { backStackEntry ->
                    val boutId = backStackEntry.arguments?.getString("boutId")
                    BoutEditScreen(
                        navController = navController,
                        pref = sharedPrefs,
                        boutId = (boutId?:"0").toLong()
                    )
                }
                composable(Routes.EditOpponent.route) { backStackEntry ->
                    val opponentId = backStackEntry.arguments?.getString("opponentId")
                    OpponentEditScreen(
                        navController = navController,
                        pref = sharedPrefs,
                        opponentId = ((opponentId)?:"0").toLong()
                    )
                }
                composable(Routes.AddBoutWithOpponent.route) { backStackEntry ->
                    val opponentId = backStackEntry.arguments?.getString("opponentId")
                    BoutEditScreen(
                        navController = navController,
                        pref = sharedPrefs,
                        startOpponentId = (opponentId?:"0").toLong() as Long?
                    )
                }
                composable (Routes.ProfileEdit.route){
                    ProfileEditScreen(navController = navController, pref = sharedPrefs)
                }

                composable (Routes.Settings.route){
                    SettingsScreen(navController = navController, pref = sharedPrefs)
                }
                composable (Routes.Statistics.route){
                    StatisticsScreen(navController = navController, pref = sharedPrefs)
                }
                composable (Routes.Sync.route){
                    SyncScreen(navController = navController, pref = sharedPrefs)
                }
            }
        }
    }
    private fun requestAllPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.SCHEDULE_EXACT_ALARM
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.SCHEDULE_EXACT_ALARM)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RUN_USER_INITIATED_JOBS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.RUN_USER_INITIATED_JOBS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_CODE_BACKGROUND
            )
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

public fun getString(context: Context, resId:Int, locale:String):String{
    val config = Configuration(context.resources.configuration)
    config.setLocale(Locale(locale))
    return context.createConfigurationContext(config).resources.getString(resId)
}


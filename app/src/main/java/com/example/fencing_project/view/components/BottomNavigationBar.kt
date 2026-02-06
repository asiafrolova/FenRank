package com.example.fencing_project.view.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fencing_project.R

sealed class BottomNavItem(
    val route: String,
    val titleResId: Int,
    val iconResId: Int
) {
    object Home : BottomNavItem("home", R.string.home, iconResId = R.drawable.home_ic)
    object Opponents : BottomNavItem("opponents", R.string.opponents, iconResId = R.drawable.folder_ic)
    object Profile : BottomNavItem("profile", R.string.profile, iconResId = R.drawable.profile_ic)
}
@SuppressLint("SuspiciousIndentation")
@Composable
fun BottomNavigationBar(navController: NavController = rememberNavController(),
                        containerColor:Color = Color(139,0,0)) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Opponents,
        BottomNavItem.Profile
    )

        NavigationBar(containerColor = containerColor) {
            val navBackStackEntry = navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry.value?.destination?.route

            items.forEach { item ->
                NavigationBarItem(
                    colors = NavigationBarItemColors(
                        selectedIconColor = Color(166, 41, 41),
                        selectedTextColor = Color(166, 41, 41),
                        selectedIndicatorColor =  Color(166, 41, 41),
                        unselectedIconColor = Color.White,
                        unselectedTextColor = Color.White,
                        disabledIconColor = Color.White,
                        disabledTextColor = Color.White,
                    ),
                    modifier = Modifier.size(25.dp, 25.dp),
                    icon = {
                        Icon(
                            painter = painterResource(id = item.iconResId),
                            contentDescription = stringResource(item.titleResId),
                            tint = Color.White
                        )
                    },
                    selected = currentRoute == item.route,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }

}
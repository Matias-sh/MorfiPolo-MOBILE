package com.cocido.morfipolo.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cocido.morfipolo.R
import com.cocido.morfipolo.ui.menu.daily.DailyMenuRoute
import com.cocido.morfipolo.ui.menu.weekly.WeeklyMenuRoute
import com.cocido.morfipolo.ui.notifications.NotificationSettingsRoute
import com.cocido.morfipolo.ui.profile.ProfileRoute
import com.cocido.morfipolo.ui.theme.FoodBackground

object AppRoutes {
    const val DailyMenu = "daily_menu"
    const val WeeklyMenu = "weekly_menu"
    const val Profile = "profile"
    const val Notifications = "notifications"
}

private data class BottomDestination(
    val route: String,
    val title: String,
    val icon: @Composable () -> Unit
)

@Composable
fun MorfiPoloApp(
    onNavigateToLogin: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val bottomDestinations = listOf(
        BottomDestination(
            route = AppRoutes.DailyMenu,
            title = "Hoy",
            icon = { Icon(painterResource(R.drawable.ic_restaurant), contentDescription = "Hoy") }
        ),
        BottomDestination(
            route = AppRoutes.WeeklyMenu,
            title = "Semana",
            icon = { Icon(painterResource(R.drawable.ic_nav_calendar), contentDescription = "Semana") }
        ),
        BottomDestination(
            route = AppRoutes.Profile,
            title = "Perfil",
            icon = { Icon(painterResource(R.drawable.ic_nav_profile), contentDescription = "Perfil") }
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != AppRoutes.Notifications

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FoodBackground),
        containerColor = FoodBackground,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White
                ) {
                    bottomDestinations.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = destination.icon,
                            label = { Text(destination.title) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.DailyMenu,
            modifier = Modifier.padding(padding)
        ) {
            composable(AppRoutes.DailyMenu) {
                DailyMenuRoute(
                    onSessionExpired = onNavigateToLogin
                )
            }
            composable(AppRoutes.WeeklyMenu) {
                WeeklyMenuRoute(
                    onOpenDailyForDate = { _ ->
                        navController.navigate(AppRoutes.DailyMenu)
                    },
                    onSessionExpired = onNavigateToLogin
                )
            }
            composable(AppRoutes.Profile) {
                ProfileRoute(
                    onOpenNotifications = { navController.navigate(AppRoutes.Notifications) },
                    onLogout = onNavigateToLogin
                )
            }
            composable(AppRoutes.Notifications) {
                NotificationSettingsRoute(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}


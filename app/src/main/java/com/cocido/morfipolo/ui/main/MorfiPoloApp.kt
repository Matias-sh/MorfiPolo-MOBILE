package com.cocido.morfipolo.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cocido.morfipolo.R
import com.cocido.morfipolo.ui.menu.daily.DailyMenuRoute
import com.cocido.morfipolo.ui.menu.weekly.WeeklyRoute
import com.cocido.morfipolo.ui.notifications.NotificationSettingsRoute
import com.cocido.morfipolo.ui.profile.ProfileRoute
import com.cocido.morfipolo.ui.theme.*

object AppRoutes {
    const val DailyMenu = "daily_menu"
    const val WeeklyMenu = "weekly_menu"
    const val Profile = "profile"
    const val Notifications = "notifications"
}

data class BottomDestination(
    val route: String,
    val title: String,
    val iconRes: Int
)

@Composable
fun MorfiPoloApp(
    onNavigateToLogin: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val bottomDestinations = listOf(
        BottomDestination(AppRoutes.DailyMenu, "HOY", R.drawable.ic_nav_calendar),
        BottomDestination(AppRoutes.WeeklyMenu, "SEMANA", R.drawable.ic_nav_calendar),
        BottomDestination(AppRoutes.Profile, "PERFIL", R.drawable.ic_nav_profile)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != AppRoutes.Notifications

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MorfiBackground,
        bottomBar = {
            if (showBottomBar) {
                CustomBottomBar(
                    destinations = bottomDestinations,
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.DailyMenu,
            modifier = Modifier.padding(padding)
        ) {
            composable(AppRoutes.DailyMenu) {
                DailyMenuRoute(onSessionExpired = onNavigateToLogin)
            }
            composable(AppRoutes.WeeklyMenu) {
                WeeklyRoute(
                    onOpenDailyForDate = { _ -> navController.navigate(AppRoutes.DailyMenu) },
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
                NotificationSettingsRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun CustomBottomBar(
    destinations: List<BottomDestination>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 24.dp, start = 40.dp, end = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            shape = RoundedCornerShape(AppRadius.extraLarge),
            color = MorfiWhite,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                destinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { onNavigate(destination.route) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (selected) MorfiOrange.copy(alpha = 0.1f) else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = null,
                                tint = if (selected) MorfiOrange else MorfiGrayMedium,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = destination.title,
                            style = MorfiTypography.labelMedium.copy(
                                color = if (selected) MorfiOrange else MorfiGrayMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

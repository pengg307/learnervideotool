package com.aigenerator.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.*
import androidx.navigation.compose.*
import com.aigenerator.app.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Chat     : Screen("chat",     "Generate", Icons.Default.AutoAwesome)
    object Gallery  : Screen("gallery",  "Gallery",  Icons.Default.PhotoLibrary)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun AppNavigation(navController: NavHostController) {
    val tabs = listOf(Screen.Chat, Screen.Gallery, Screen.Settings)
    val back by navController.currentBackStackEntryAsState()
    val cur = back?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { s ->
                    NavigationBarItem(
                        selected = cur == s.route,
                        icon = { Icon(s.icon, s.label) },
                        label = { Text(s.label) },
                        onClick = {
                            navController.navigate(s.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = Screen.Chat.route) {
            composable(Screen.Chat.route)     { ChatScreen(padding) }
            composable(Screen.Gallery.route)  { GalleryScreen(padding) }
            composable(Screen.Settings.route) { SettingsScreen(padding) }
        }
    }
}

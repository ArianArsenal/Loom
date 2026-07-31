package com.example.loom.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.loom.data.local.AppDatabase
import com.example.loom.ui.home.HomeScreen
import com.example.loom.ui.home.HomeViewModel
import com.example.loom.ui.notes.DiaryScreen
import com.example.loom.ui.health.HealthScreen
import com.example.loom.ui.settings.SettingsScreen
import com.example.loom.ui.health.HealthViewModel
import com.example.loom.ui.sandbox.SandboxMainScreen
import com.example.loom.ui.sandbox.SandboxViewModel
import com.example.loom.ui.sandbox.SandboxViewModelFactory

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object Health : BottomNavItem("health", "Health", Icons.Default.Favorite)
    object Calendar : BottomNavItem("calendar", "Calendar", Icons.Default.DateRange)
    object Diary : BottomNavItem("diary", "Diary", Icons.Default.Book)
    object Spaces : BottomNavItem("spaces", "Spaces", Icons.Default.Dashboard) // <-- Updated Name and Route
}

@Composable
fun LoomMainScreen(viewModel: HomeViewModel) {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Health,
        BottomNavItem.Calendar,
        BottomNavItem.Diary,
        BottomNavItem.Spaces // <-- Updated
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        ),
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home Route
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }

            // Health Route
            composable(BottomNavItem.Health.route) {
                val healthViewModel: HealthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                HealthScreen(viewModel = healthViewModel)
            }

            // Calendar Route
            composable(BottomNavItem.Calendar.route) {
                val calendarViewModel: com.example.loom.ui.calendar.CalendarViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                com.example.loom.ui.calendar.CalendarScreen(viewModel = calendarViewModel)
            }

            // Diary Route
            composable(BottomNavItem.Diary.route) {
                DiaryScreen(viewModel = viewModel)
            }

            // Spaces Route
            composable(BottomNavItem.Spaces.route) { // <-- Updated
                val context = LocalContext.current
                val database = remember { AppDatabase.getDatabase(context) }
                val sandboxViewModel: SandboxViewModel = viewModel(
                    factory = SandboxViewModelFactory(database.sandboxDao)
                )

                SandboxMainScreen(viewModel = sandboxViewModel)
            }

            // Settings Route
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
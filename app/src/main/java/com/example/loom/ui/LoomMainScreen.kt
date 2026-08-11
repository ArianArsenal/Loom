package com.example.loom.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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


sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object Health : BottomNavItem("health", "Health", Icons.Default.Favorite)
    object Calendar : BottomNavItem("calendar", "Calendar", Icons.Default.DateRange)
    object Diary : BottomNavItem("diary", "Diary", Icons.Default.Book)
    object Spaces : BottomNavItem("spaces", "Spaces", Icons.Default.Dashboard)
}

@Composable
fun LoomMainScreen(viewModel: HomeViewModel) {
    val navController = rememberNavController()

    // --- NEW: Dynamic Bottom Bar Visibility State ---
    var isBottomBarVisible by remember { mutableStateOf(true) }

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Health,
        BottomNavItem.Calendar,
        BottomNavItem.Diary,
        BottomNavItem.Spaces
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // --- NEW: Smoothly animate the bar in and out! ---
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(animationSpec = tween(300)) { height -> height },
                exit = slideOutVertically(animationSpec = tween(300)) { height -> height }
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    items.forEach { item ->
                        val isSelected = currentRoute == item.route ||
                                (item.route == BottomNavItem.Home.route && currentRoute == "settings")

                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            ),
                            onClick = {
                                if (item.route == BottomNavItem.Home.route && currentRoute == "settings") {
                                    navController.popBackStack(BottomNavItem.Home.route, inclusive = false)
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            // Only apply bottom padding if the bar is visible so the canvas can take the full screen
            modifier = Modifier.padding(bottom = if (isBottomBarVisible) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            // Home Route
            composable(BottomNavItem.Home.route) {
                isBottomBarVisible = true
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { navController.navigate("settings")},
                    onNavigateToSpace = { spaceId ->
                        // Note: If you ever implement deep linking to spaces from Home, handle visibility here
                        navController.navigate("spaces")
                    }
                )
            }

            // Health Route
            composable(BottomNavItem.Health.route) {
                isBottomBarVisible = true
                val healthViewModel: HealthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                HealthScreen(viewModel = healthViewModel)
            }

            // Calendar Route
            composable(BottomNavItem.Calendar.route) {
                isBottomBarVisible = true
                val calendarViewModel: com.example.loom.ui.calendar.CalendarViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                com.example.loom.ui.calendar.CalendarScreen(viewModel = calendarViewModel)
            }

            // Diary Route
            composable(BottomNavItem.Diary.route) {
                isBottomBarVisible = true
                DiaryScreen(viewModel = viewModel)
            }

            // Spaces Route
            composable(BottomNavItem.Spaces.route) {
                // THE FIX: Uses Hilt to grab the shared Unified Database Instance
                val sandboxViewModel: SandboxViewModel = androidx.hilt.navigation.compose.hiltViewModel()

                SandboxMainScreen(
                    viewModel = sandboxViewModel,
                    onCanvasVisibilityChanged = { isCanvasOpen ->
                        isBottomBarVisible = !isCanvasOpen
                    }
                )
            }

            // Settings Route
            composable("settings") {
                isBottomBarVisible = true
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
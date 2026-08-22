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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.loom.ui.health.HealthScreen
import com.example.loom.ui.health.HealthViewModel
import com.example.loom.ui.home.HomeScreen
import com.example.loom.ui.home.HomeViewModel
import com.example.loom.ui.notes.DiaryScreen
import com.example.loom.ui.sandbox.SandboxMainScreen
import com.example.loom.ui.sandbox.SandboxViewModel
import com.example.loom.ui.settings.SettingsScreen
import com.example.loom.ui.settings.BugReportScreen

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    data object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    data object Health : BottomNavItem("health", "Health", Icons.Default.Favorite)
    data object Calendar : BottomNavItem("calendar", "Calendar", Icons.Default.DateRange)
    data object Diary : BottomNavItem("diary", "Diary", Icons.Default.Book)
    data object Spaces : BottomNavItem("spaces", "Spaces", Icons.Default.Dashboard)
}

private val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Health,
    BottomNavItem.Calendar,
    BottomNavItem.Diary,
    BottomNavItem.Spaces
)

@Composable
fun LoomMainScreen(viewModel: HomeViewModel) {
    val navController = rememberNavController()
    var isBottomBarVisible by remember { mutableStateOf(true) }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(animationSpec = tween(300)) { height -> height },
                exit = slideOutVertically(animationSpec = tween(300)) { height -> height }
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route ||
                                (item.route == BottomNavItem.Home.route && currentRoute == "settings") ||
                                (item.route == BottomNavItem.Home.route && currentRoute == "bug_report")

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
                                if (item.route == BottomNavItem.Home.route && (currentRoute == "settings" || currentRoute == "bug_report")) {
                                    navController.popBackStack(BottomNavItem.Home.route, inclusive = false)
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
            modifier = Modifier.padding(bottom = if (isBottomBarVisible) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            composable(BottomNavItem.Home.route) {
                isBottomBarVisible = true
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToSpace = { _ -> navController.navigate("spaces") }
                )
            }

            composable(BottomNavItem.Health.route) {
                isBottomBarVisible = true
                val healthViewModel: HealthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                HealthScreen(viewModel = healthViewModel)
            }

            composable(BottomNavItem.Calendar.route) {
                isBottomBarVisible = true
                val calendarViewModel: com.example.loom.ui.calendar.CalendarViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                com.example.loom.ui.calendar.CalendarScreen(viewModel = calendarViewModel)
            }

            composable(BottomNavItem.Diary.route) {
                isBottomBarVisible = true
                DiaryScreen(viewModel = viewModel)
            }

            composable(BottomNavItem.Spaces.route) {
                val sandboxViewModel: SandboxViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                SandboxMainScreen(
                    viewModel = sandboxViewModel,
                    onCanvasVisibilityChanged = { isCanvasOpen -> isBottomBarVisible = !isCanvasOpen }
                )
            }

            composable("settings") {
                isBottomBarVisible = true
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBugReport = { navController.navigate("bug_report") }
                )
            }

            // --- NEW: Bug Report Route ---
            composable("bug_report") {
                isBottomBarVisible = true
                BugReportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
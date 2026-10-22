package com.angel.fitgains.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.angel.fitgains.container
import com.angel.fitgains.core.data.Meal
import com.angel.fitgains.ui.analysis.AnalyzeScreen
import com.angel.fitgains.ui.diary.DiaryScreen
import com.angel.fitgains.ui.onboarding.ProfileScreen
import com.angel.fitgains.ui.progress.ProgressScreen
import com.angel.fitgains.ui.search.SearchScreen

private object Routes {
    const val START = "start"
    const val ONBOARDING = "onboarding"
    const val DIARY = "diary"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val ANALYZE = "analyze/{meal}"
    const val SEARCH = "search/{meal}"
    fun analyze(meal: Meal) = "analyze/${meal.name}"
    fun search(meal: Meal) = "search/${meal.name}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.DIARY, "Diario", Icons.AutoMirrored.Filled.MenuBook),
    Tab(Routes.PROGRESS, "Progreso", Icons.AutoMirrored.Filled.ShowChart),
    Tab(Routes.PROFILE, "Perfil", Icons.Default.Person),
)

@Composable
fun FitGainsNavHost() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val repo = LocalContext.current.container.repository

    Scaffold(
        bottomBar = {
            if (tabs.any { it.route == current }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.START, modifier = Modifier.padding(padding)) {
            composable(Routes.START) {
                LaunchedEffect(Unit) {
                    val next = if (repo.hasProfile()) Routes.DIARY else Routes.ONBOARDING
                    nav.navigate(next) { popUpTo(Routes.START) { inclusive = true } }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            composable(Routes.ONBOARDING) {
                ProfileScreen(isOnboarding = true, onSaved = {
                    nav.navigate(Routes.DIARY) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(isOnboarding = false, onSaved = { nav.switchTab(Routes.DIARY) })
            }
            composable(Routes.DIARY) {
                DiaryScreen(
                    onPhoto = { nav.navigate(Routes.analyze(it)) },
                    onSearch = { nav.navigate(Routes.search(it)) },
                )
            }
            composable(Routes.PROGRESS) { ProgressScreen() }
            composable(
                Routes.ANALYZE,
                arguments = listOf(navArgument("meal") { type = NavType.StringType }),
            ) { entry ->
                AnalyzeScreen(
                    meal = Meal.valueOf(entry.arguments!!.getString("meal")!!),
                    onDone = { nav.popBackStack() },
                    onManualSearch = { meal ->
                        nav.navigate(Routes.search(meal)) { popUpTo(Routes.DIARY) }
                    },
                )
            }
            composable(
                Routes.SEARCH,
                arguments = listOf(navArgument("meal") { type = NavType.StringType }),
            ) { entry ->
                SearchScreen(
                    meal = Meal.valueOf(entry.arguments!!.getString("meal")!!),
                    onDone = { nav.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(Routes.DIARY) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

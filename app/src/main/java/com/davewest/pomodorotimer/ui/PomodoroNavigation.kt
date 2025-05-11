package com.davewest.pomodorotimer.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun PomodoroNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "timer"
    ) {
        composable("timer") {
            PomodoroTimerScreen(
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            // You'll create this screen later
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
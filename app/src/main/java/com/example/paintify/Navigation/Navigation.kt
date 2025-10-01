package com.example.paintify.Navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.paintify.screens.DrawScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = "home"
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") { DrawScreen(navController) }

        // Example for future routes:
        // composable("profile/{username}") { backStackEntry ->
        //     val user = backStackEntry.arguments?.getString("username")
        //     ProfileScreen(navController, user)
        // }
    }
}

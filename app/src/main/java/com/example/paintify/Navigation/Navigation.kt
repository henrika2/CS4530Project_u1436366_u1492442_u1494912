package com.example.paintify.Navigation

import SplashScreen
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.paintify.screens.DrawScreen
import com.example.paintify.R

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = "splash"
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("splash") {
            SplashScreen(
                logoResId = R.drawable.logo,
                onFinished = {
                    navController.navigate("canvas") {
                        popUpTo("splash") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    composable("canvas") { DrawScreen(navController)}
        // Example for future routes:
        // composable("profile/{username}") { backStackEntry ->
        //     val user = backStackEntry.arguments?.getString("username")
        //     ProfileScreen(navController, user)
        // }
    }
}


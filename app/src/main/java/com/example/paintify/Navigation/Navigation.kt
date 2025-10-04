/**
 * Paintify - App Navigation Host
 * -------------------------------
 * This file defines the navigation graph for the Paintify application.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * The `AppNavHost` composable manages navigation between screens using
 * Jetpack Compose's Navigation component. It defines the routes available
 * within the app, including the splash screen and main drawing screen.
 *
 * The navigation flow:
 *  1. Displays a splash screen on startup.
 *  2. Automatically transitions to the drawing canvas after animation completion.
 */

package com.example.paintify.Navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.paintify.screens.DrawScreen
import com.example.paintify.screens.SplashScreen
import com.example.paintify.R

/**
 * AppNavHost composable.
 *
 * Manages navigation between different screens of the Paintify app.
 *
 * @param navController The navigation controller used to handle route transitions.
 * @param startDestination The initial route displayed when the app starts.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = "splash"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash Screen route
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

        // Main Canvas Screen route
        composable("canvas") {
            DrawScreen(navController)
        }

        // Example for future navigation routes:
        // composable("profile/{username}") { backStackEntry ->
        //     val user = backStackEntry.arguments?.getString("username")
        //     ProfileScreen(navController, user)
        // }
    }
}

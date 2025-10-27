package com.example.paintify.Navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.paintify.R
import com.example.paintify.screens.DrawScreen
import com.example.paintify.screens.HomeScreen
import com.example.paintify.screens.HomeViewModelProvider
import com.example.paintify.screens.SplashScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.paintify.screens.DetailScreen
import com.example.paintify.screens.DrawScreenWithBackground


@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = "splash"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash → Home
        composable("splash") {
            SplashScreen(
                logoResId = R.drawable.logo,
                onFinished = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Home list screen
        composable("home") {
            HomeScreen(
                navController = navController,
                vm = HomeViewModelProvider.provide()
            )
        }

        // Drawing canvas
        composable("canvas") {
            DrawScreen(navController)
        }


        composable(
            route = "canvas/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            DrawScreenWithBackground(
                navController = navController,
                drawingId = id
            )
        }

    }
}

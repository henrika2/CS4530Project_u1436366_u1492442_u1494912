//package com.example.paintify.navigation
//
//import androidx.compose.runtime.Composable
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.navArgument
//import androidx.navigation.NavType
//import com.example.paintify.screens.DrawScreen
//import com.example.paintify.screens.SplashScreen
//import com.example.paintify.screens.HomeScreen
//import com.example.paintify.R
//
//@Composable
//fun AppNavHost(
//    navController: NavHostController,
//    startDestination: String = "splash"
//) {
//    NavHost(navController = navController, startDestination = startDestination) {
//
//        composable("splash") {
//            SplashScreen(
//                logoResId = R.drawable.logo,
//                onFinished = {
//                    navController.navigate("home") {
//                        popUpTo("splash") { inclusive = true }
//                        launchSingleTop = true
//                    }
//                }
//            )
//        }
//
//        // Main list of saved drawings + "New" action
//        composable("home") {
//            HomeScreen(
//                onNewDrawing = { navController.navigate("canvas") },
//                onOpenDrawing = { drawingId -> navController.navigate("canvas?drawingId=$drawingId") }
//            )
//        }
//
//        // Canvas (optional param to reopen an existing drawing)
//        composable(
//            route = "canvas?drawingId={drawingId}",
//            arguments = listOf(
//                navArgument("drawingId") {
//                    type = NavType.LongType
//                    nullable = true
//                    defaultValue = null
//                }
//            )
//        ) { backStack ->
//            val drawingId = backStack.arguments?.getLong("drawingId")
//            DrawScreen(
//                navController = navController,
//                reopenDrawingId = drawingId
//            )
//        }
//    }
//}

package com.example.paintify.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.paintify.R
import com.example.paintify.screens.SplashScreen
import com.example.paintify.screens.HomeScreen
import com.example.paintify.screens.DrawScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = "splash"
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable("splash") {
            // Use a guaranteed resource (launcher icon) OR pass null
            SplashScreen(
                logoResId = R.mipmap.ic_launcher, // or null
                onFinished = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onNewDrawing = { navController.navigate("canvas") },
                onOpenDrawing = { id -> navController.navigate("canvas?drawingId=$id") }
            )
        }

        composable(
            route = "canvas?drawingId={drawingId}",
            arguments = listOf(
                navArgument("drawingId") {
                    type = NavType.LongType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val drawingId = entry.arguments?.getLong("drawingId")
            DrawScreen(navController = navController, reopenDrawingId = drawingId)
        }
    }
}


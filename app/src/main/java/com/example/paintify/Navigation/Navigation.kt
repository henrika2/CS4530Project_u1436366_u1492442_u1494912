package com.example.paintify.Navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHost
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController
import com.example.paintify.screens.DrawScreen
//import com.example.navigationdemo.screens.ProfileScreen

@Composable
fun AppNavHost(navController: NavHostController, startDestination: String="home")
{
    NavHost(navController=navController, startDestination=startDestination)
    {
        composable("home") {
            DrawScreen(navController)
        }

//        composable("home/{username}") {
//            val userId = it.arguments?.getString("username")
//            ProfileScreen(navController, userId)
//        }
    }
}
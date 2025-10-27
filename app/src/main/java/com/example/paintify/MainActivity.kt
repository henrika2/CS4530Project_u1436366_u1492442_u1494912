/**
 * Paintify - Main Activity
 * ------------------------
 * This file defines the entry point of the Paintify Android application.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * The `MainActivity` class sets up the Jetpack Compose environment,
 * initializes the navigation controller, and launches the main composable
 * navigation host (`AppNavHost`). This serves as the root of the app’s
 * UI hierarchy and theme structure.
 */
package com.example.paintify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.paintify.Navigation.AppNavHost
import com.example.paintify.ui.theme.PaintifyTheme

/**
 * MainActivity - the main launcher activity for Paintify.
 *
 * Responsible for:
 *  - Enabling edge-to-edge content layout.
 *  - Setting the global Compose theme.
 *  - Initializing the navigation controller.
 *  - Displaying the app’s primary navigation host.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enables drawing behind system bars
        enableEdgeToEdge()

        setContent {
            PaintifyTheme {
                val myNavControl = rememberNavController()
                AppNavHost(navController = myNavControl)
            }
        }
    }
}

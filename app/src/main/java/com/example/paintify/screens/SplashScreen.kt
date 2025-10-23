///**
// * Paintify - Splash Screen
// * -------------------------
// * This file defines the SplashScreen composable function that displays
// * an animated app logo before navigating to the main application.
// *
// * The animation includes both scaling and fading effects, and remains visible
// * for a defined duration (`holdMillis`) before triggering `onFinished()`.
// *
// * Group Members:
// *  - Dustin
// *  - Nolan
// *  - Ian
// *
// * Description:
// * This composable creates a visually smooth splash animation using
// * Jetpack Compose’s animation APIs. It is designed to appear once at app startup,
// * providing a polished transition into the main UI.
// */
//
//package com.example.paintify.screens
//
//import androidx.compose.animation.core.EaseOutBack
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.size
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.alpha
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.testTag
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.unit.dp
//import kotlinx.coroutines.delay
//
///**
// * SplashScreen composable function.
// *
// * Displays an animated splash logo that scales and fades in using smooth
// * transition effects. After holding for a specified duration, it fades out
// * and calls the `onFinished()` callback.
// *
// * @param logoResId Resource ID of the logo image to display.
// * @param modifier Modifier for layout customization (optional).
// * @param background Background color for the splash screen.
// * @param holdMillis Duration in milliseconds to keep the splash visible after animation.
// * @param onFinished Callback executed after the splash screen finishes displaying.
// */
//@Composable
//fun SplashScreen(
//    logoResId: Int,
//    modifier: Modifier = Modifier,
//    background: Color = MaterialTheme.colorScheme.background,
//    holdMillis: Long = 2000,
//    onFinished: () -> Unit = {}
//) {
//    // Internal animation state variables
//    var started by remember { mutableStateOf(false) }
//    var visible by remember { mutableStateOf(true) }
//
//    val scale by animateFloatAsState(
//        targetValue = if (started) 1f else 0.85f,
//        animationSpec = tween(
//            durationMillis = 600,
//            easing = EaseOutBack
//        ),
//        label = "logo-scale"
//    )
//
//    val alpha by animateFloatAsState(
//        targetValue = if (started) 1f else 0f,
//        animationSpec = tween(durationMillis = 450),
//        label = "logo-alpha"
//    )
//
//    LaunchedEffect(Unit) {
//        started = true
//        delay(holdMillis)
//        visible = false
//        onFinished()
//    }
//
//    if (visible) {
//        Box(
//            modifier = modifier
//                .fillMaxSize()
//                .background(background),
//            contentAlignment = Alignment.Center
//        ) {
//            Image(
//                painter = painterResource(id = logoResId),
//                contentDescription = "splashLogo",
//                modifier = Modifier
//                    .size(170.dp)
//                    .scale(scale)
//                    .alpha(alpha)
//                    .testTag("splashLogo")
//            )
//        }
//    }
//}

package com.example.paintify.screens

import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Robust splash: if [logoResId] is null or invalid, we show a text fallback.
 */
@Composable
fun SplashScreen(
    logoResId: Int?,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.background,
    holdMillis: Long = 800,
    onFinished: () -> Unit
) {
    var started by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0.85f,
        animationSpec = tween(600, easing = EaseOutBack),
        label = "splash-scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(450),
        label = "splash-alpha"
    )

    LaunchedEffect(Unit) {
        started = true
        delay(holdMillis)
        onFinished()
    }

    Box(
        modifier = modifier.fillMaxSize().background(background),
        contentAlignment = Alignment.Center
    ) {
        val showLogo = remember(logoResId) { logoResId != null && logoResId != 0 }
        if (showLogo) {
            // If the resource id is wrong, painterResource would throw.
            // So guard with runCatching and fallback to text.
            val painter = runCatching { painterResource(id = logoResId!!) }.getOrNull()
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = "Paintify Logo",
                    modifier = Modifier.size(170.dp).scale(scale).alpha(alpha)
                )
            } else {
                Text("Paintify", style = MaterialTheme.typography.headlineMedium)
            }
        } else {
            Text("Paintify", style = MaterialTheme.typography.headlineMedium)
        }
    }
}


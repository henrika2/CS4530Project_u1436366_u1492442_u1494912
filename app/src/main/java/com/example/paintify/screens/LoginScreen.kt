package com.example.paintify.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.paintify.ui.PaintifyColors
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.TextFieldDefaults

@Composable
fun LoginScreen(navController: NavHostController) {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf(auth.currentUser) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var dataString by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PaintifyColors.Background,
                        PaintifyColors.Surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = cardColors(
                    containerColor = PaintifyColors.SurfaceVariant
                ),
                elevation = cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Paintify Login",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )

                    if (user == null) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = PaintifyColors.Accent,
                                focusedIndicatorColor = PaintifyColors.Accent,
                                unfocusedIndicatorColor = PaintifyColors.Surface,
                                focusedLabelColor = PaintifyColors.Accent,
                                unfocusedLabelColor = Color.LightGray,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = PaintifyColors.Accent,
                                focusedIndicatorColor = PaintifyColors.Accent,
                                unfocusedIndicatorColor = PaintifyColors.Surface,
                                focusedLabelColor = PaintifyColors.Accent,
                                unfocusedLabelColor = Color.LightGray,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )

                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            auth.signInWithEmailAndPassword(email, password).await()
                                            user = auth.currentUser
                                        } catch (e: Exception) {
                                            error = e.message
                                        }
                                    }
                                },
                                modifier = Modifier.width(120.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PaintifyColors.Accent,
                                    contentColor = Color.White
                                )
                            ) { Text("Login") }

                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            auth.createUserWithEmailAndPassword(email, password).await()
                                            user = auth.currentUser
                                        } catch (e: Exception) {
                                            error = e.message
                                        }
                                    }
                                },
                                modifier = Modifier.width(120.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PaintifyColors.Surface,
                                    contentColor = Color.White
                                )
                            ) { Text("Sign Up") }
                        }

                        error?.let {
                            Text(
                                it,
                                color = PaintifyColors.Error
                            )
                        }
                    } else {
                        var dataStringInner by remember { mutableStateOf("") }
                        var message by remember { mutableStateOf("") }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Welcome ${user!!.email}",
                                color = Color.White
                            )

                            Button(
                                onClick = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                modifier = Modifier.width(150.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PaintifyColors.Accent,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Home page")
                            }

                            if (message.isNotEmpty()) {
                                Text(message, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    Firebase.auth.signOut()
                                    user = null
                                },
                                modifier = Modifier.width(150.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PaintifyColors.Surface,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Sign out")
                            }
                        }
                    }
                }
            }
        }
    }
}


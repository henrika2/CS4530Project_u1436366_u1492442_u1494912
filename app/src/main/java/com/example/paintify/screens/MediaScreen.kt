package com.example.paintify.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.paintify.DrawApplication
import com.example.paintify.cloud.CloudDrawing
import com.example.paintify.cloud.CloudSync
import com.example.paintify.data.DrawingRepository
import com.example.paintify.ui.PaintifyColors
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

/**
 * A Firestore representation of a "shared_drawings" document.
 */
data class SharedDrawing(
    val id: String = "",
    val senderId: String = "",
    val receiverEmail: String = "",
    val imageUrl: String = "",
    val title: String = "",
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as DrawApplication
    val repo: DrawingRepository = app.drawingRepository
    val user = Firebase.auth.currentUser
    val scope = rememberCoroutineScope()

    var myCloudImages by remember { mutableStateOf<List<CloudDrawing>>(emptyList()) }
    var sharedByMe by remember { mutableStateOf<List<SharedDrawing>>(emptyList()) }
    var sharedWithMe by remember { mutableStateOf<List<SharedDrawing>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load everything when user changes
    LaunchedEffect(user?.uid, user?.email) {
        if (user == null) {
            myCloudImages = emptyList()
            sharedByMe = emptyList()
            sharedWithMe = emptyList()
            isLoading = false
            errorMessage = "You must be logged in to view media."
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        val db = Firebase.firestore

        try {
            // 1) My Cloud Images (user_drawings)
            myCloudImages = CloudSync.loadDrawingsForUser(user.uid)

            // 2) Shared by Me
            val sentSnap = db.collection("shared_drawings")
                .whereEqualTo("senderId", user.uid)
                .get()
                .await()

            sharedByMe = sentSnap.documents.map { doc ->
                SharedDrawing(
                    id = doc.id,
                    senderId = doc.getString("senderId") ?: "",
                    receiverEmail = doc.getString("receiverEmail") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    title = doc.getString("title") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L
                )
            }

            // 3) Shared with Me
            val email = user.email
            if (email != null) {
                val recvSnap = db.collection("shared_drawings")
                    .whereEqualTo("receiverEmail", email)
                    .get()
                    .await()

                sharedWithMe = recvSnap.documents.map { doc ->
                    SharedDrawing(
                        id = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        receiverEmail = doc.getString("receiverEmail") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        title = doc.getString("title") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }
            } else {
                sharedWithMe = emptyList()
            }

        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load media."
        } finally {
            isLoading = false
        }
    }

    // For "Shared" indicator in "My Cloud Images"
    val sharedUrlsByMe = remember(sharedByMe) {
        sharedByMe.map { it.imageUrl }.toSet()
    }

    Scaffold(
        containerColor = PaintifyColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Media Gallery", color = Color.White) },
                colors = topAppBarColors(
                    containerColor = PaintifyColors.Surface,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PaintifyColors.Background)
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PaintifyColors.Accent
                    )
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        color = PaintifyColors.Error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // -------- MY CLOUD IMAGES --------
                        item {
                            SectionHeader(
                                icon = Icons.Default.Person,
                                title = "My Cloud Images",
                                subtitle = user?.email ?: ""
                            )
                        }

                        if (myCloudImages.isEmpty()) {
                            item {
                                Text(
                                    "No cloud images yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            items(myCloudImages, key = { it.id }) { drawing ->
                                val isShared = sharedUrlsByMe.contains(drawing.imageUrl)
                                CloudImageCard(
                                    drawing = drawing,
                                    isShared = isShared,
                                    creatorLabel = user?.email ?: "Me",
                                    onEdit = {
                                        scope.launch {
                                            try {
                                                val newId = repo.importImageFromUrl(
                                                    imageUrl = drawing.imageUrl,
                                                    title = drawing.title.ifBlank { "Cloud Drawing" }
                                                )
                                                navController.navigate("canvas/$newId")
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        //SHARED BY ME
                        item {
                            Spacer(Modifier.height(8.dp))
                            SectionHeader(
                                icon = Icons.Default.Send,
                                title = "Shared by Me",
                                subtitle = "Images I've shared with others"
                            )
                        }

                        if (sharedByMe.isEmpty()) {
                            item {
                                Text(
                                    "You haven't shared any drawings yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            items(sharedByMe, key = { it.id }) { shared ->
                                SharedImageCard(
                                    drawing = shared,
                                    label = "To: ${shared.receiverEmail}",
                                    onEdit = {
                                        scope.launch {
                                            try {
                                                val newId = repo.importImageFromUrl(
                                                    imageUrl = shared.imageUrl,
                                                    title = shared.title.ifBlank { "Shared by Me" }
                                                )
                                                navController.navigate("canvas/$newId")
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        //SHARED WITH ME
                        item {
                            Spacer(Modifier.height(8.dp))
                            SectionHeader(
                                icon = Icons.Default.Share,
                                title = "Shared with Me",
                                subtitle = "Images others shared with me"
                            )
                        }

                        if (sharedWithMe.isEmpty()) {
                            item {
                                Text(
                                    "No one has shared drawings with you yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            items(sharedWithMe, key = { it.id }) { shared ->
                                SharedImageCard(
                                    drawing = shared,
                                    label = "From: ${shared.senderId}",
                                    onEdit = {
                                        scope.launch {
                                            try {
                                                val newId = repo.importImageFromUrl(
                                                    imageUrl = shared.imageUrl,
                                                    title = shared.title.ifBlank { "Shared with Me" }
                                                )
                                                navController.navigate("canvas/$newId")
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Card for "My Cloud Images" section.
 * Shows thumbnail, title, timestamp, creator, and "Shared" indicator if applicable.
 */
@Composable
fun CloudImageCard(
    drawing: CloudDrawing,
    isShared: Boolean,
    creatorLabel: String,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = cardColors(
            containerColor = PaintifyColors.SurfaceVariant
        ),
        elevation = cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = drawing.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    val formatted = remember(drawing.timestamp) {
                        SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                            .format(Date(drawing.timestamp))
                    }

                    Text(
                        text = "Uploaded: $formatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Creator: $creatorLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                if (isShared) {
                    Text(
                        text = "Shared",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = PaintifyColors.AccentSoft
                    )
                }

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = onEdit) {
                    Text("Edit in Canvas", color = PaintifyColors.AccentSoft)
                }

            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = drawing.imageUrl,
                    contentDescription = drawing.title,
                    modifier = Modifier
                        .width(120.dp)
                        .height(190.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

/**
 * Card for "Shared by Me" and "Shared with Me".
 * Shows thumbnail, title, timestamp, and "To: / From:" label.
 */
@Composable
fun SharedImageCard(
    drawing: SharedDrawing,
    label: String,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = cardColors(
            containerColor = PaintifyColors.Surface
        ),
        elevation = cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = drawing.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    val formatted = remember(drawing.timestamp) {
                        SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                            .format(Date(drawing.timestamp))
                    }

                    Text(
                        text = "Shared: $formatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Text(
                    text = "Shared",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PaintifyColors.AccentSoft
                )

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = onEdit) {
                    Text("Edit in Canvas", color = PaintifyColors.AccentSoft)
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = drawing.imageUrl,
                    contentDescription = drawing.title,
                    modifier = Modifier
                        .width(120.dp)
                        .height(190.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}


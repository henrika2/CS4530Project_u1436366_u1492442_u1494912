package com.example.paintify.screens

/**
 * Paintify - Home Screen
 * ----------------------
 * Shows the gallery of saved drawings and entry points for drawing, import,
 * and image analysis features.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * The `HomeViewModel` exposes a list of `DrawingData` objects from the
 * `DrawingRepository` and provides operations to delete items or import
 * images from the system photo picker. `HomeScreen` renders the list of
 * drawings as cards with preview thumbnails and actions for open, share,
 * and delete, along with floating action buttons to start a new canvas,
 * import from the gallery, or launch the AI analysis flow.
 */

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.paintify.DrawApplication
import com.example.paintify.cloud.CloudDrawing
import com.example.paintify.cloud.CloudSharing
import com.example.paintify.cloud.CloudSync
import com.example.paintify.cloud.SharedDrawing
import com.example.paintify.data.DrawingData
import com.example.paintify.data.DrawingRepository
import com.example.paintify.ui.PaintifyColors
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class HomeViewModel(
    private val repo: DrawingRepository
) : ViewModel() {
    val drawings: StateFlow<List<DrawingData>> = repo.allDrawings.stateIn(
        scope = repo.scope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList()
    )

    fun delete(d: DrawingData) = repo.deleteDrawing(d, deleteFileOnDisk = true)
    fun importFromGallery(uri: Uri) = repo.importImageFromGallery(uri)
}

object HomeViewModelProvider {
    @Composable
    fun provide(): HomeViewModel {
        val app = LocalContext.current.applicationContext as DrawApplication
        return viewModel(factory = viewModelFactory {
            initializer { HomeViewModel(app.drawingRepository) }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    vm: HomeViewModel = HomeViewModelProvider.provide()
) {
    val ctx = LocalContext.current
    val drawings by vm.drawings.collectAsState()

    // NEW: Cloud sync
    val auth = Firebase.auth
    val user = auth.currentUser

    var cloudDrawings by remember { mutableStateOf<List<CloudDrawing>>(emptyList()) }
    var isCloudLoading by remember { mutableStateOf(false) }
    var cloudError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var sharedToMe by remember { mutableStateOf<List<SharedDrawing>>(emptyList()) }
    var sharedToMeError by remember { mutableStateOf<String?>(null) }
    var isSharedToMeLoading by remember { mutableStateOf(false) }

    var shareTargetDrawing by remember { mutableStateOf<DrawingData?>(null) }
    var shareEmail by remember { mutableStateOf("") }
    var shareError by remember { mutableStateOf<String?>(null) }
    var isSharing by remember { mutableStateOf(false) }
    var shareTargetCloud by remember { mutableStateOf<CloudDrawing?>(null) }

    // Load from Firestore whenever the logged-in user changes
    LaunchedEffect(user?.uid) {
        if (user != null) {
            isCloudLoading = true
            try {
                cloudDrawings = CloudSync.loadDrawingsForUser(user.uid)
                cloudError = null
            } catch (e: Exception) {
                cloudError = e.message
            } finally {
                isCloudLoading = false
            }
        } else {
            cloudDrawings = emptyList()
            cloudError = null
        }
    }

    LaunchedEffect(user?.email) {
        val email = user?.email
        if (email != null) {
            isSharedToMeLoading = true
            try {
                sharedToMe = CloudSharing.loadSharedToUser(email)
                sharedToMeError = null
            } catch (e: Exception) {
                sharedToMeError = e.message
            } finally {
                isSharedToMeLoading = false
            }
        } else {
            sharedToMe = emptyList()
            sharedToMeError = null
        }
    }

    // Gallery picker
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { vm.importFromGallery(it) }
    }

    val pickForAnalysis = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            navController.navigate("analyzeImage?uri=${Uri.encode(it.toString())}")
        }
    }

    Scaffold(
        containerColor = PaintifyColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Paintify — Saved Drawings",
                        color = Color.White
                    )
                },
                colors = topAppBarColors(
                    containerColor = PaintifyColors.Surface,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExtendedFloatingActionButton(
                    text = { Text("Analyze") },
                    icon = {
                        Icon(
                            Icons.Default.IosShare,
                            contentDescription = "Analyze"
                        )
                    },
                    onClick = {
                        pickForAnalysis.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    containerColor = PaintifyColors.Accent,
                    contentColor = Color.White
                )

                ExtendedFloatingActionButton(
                    text = { Text("Import") },
                    icon = {
                        Icon(
                            Icons.Default.IosShare,
                            contentDescription = "Import"
                        )
                    },
                    onClick = {
                        pickMedia.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    containerColor = PaintifyColors.SurfaceVariant,
                    contentColor = Color.White
                )

                ExtendedFloatingActionButton(
                    text = { Text("Media") },
                    icon = {
                        Icon(
                            Icons.Default.IosShare,
                            contentDescription = "Media"
                        )
                    },
                    onClick = { navController.navigate("media") },
                    containerColor = PaintifyColors.SurfaceVariant,
                    contentColor = Color.White
                )

                FloatingActionButton(
                    onClick = { navController.navigate("canvas") },
                    containerColor = PaintifyColors.Accent,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PaintifyColors.Background)
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (drawings.isEmpty() && cloudDrawings.isEmpty() && sharedToMe.isEmpty()) {
                Text(
                    "No saved drawings yet.\nTap ➕ to start, or Import from gallery.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(drawings, key = { it.id }) { drawing ->
                        DrawingCard(
                            drawing = drawing,
                            onOpen = { navController.navigate("canvas/${drawing.id}") },
                            onDelete = { vm.delete(drawing) }
                        )
                    }

                    // Cloud drawings section
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Your Cloud Drawings",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }

                    when {
                        isCloudLoading -> {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(color = PaintifyColors.Accent)
                                }
                            }
                        }

                        cloudError != null -> {
                            item {
                                Text(
                                    text = "Error loading cloud drawings: $cloudError",
                                    color = PaintifyColors.Error,
                                    modifier = Modifier.padding(8.dp)
                                )

                                Log.d("CloudSync", "Error loading cloud drawings: $cloudError")
                            }
                        }

                        cloudDrawings.isEmpty() -> {
                            item {
                                Text(
                                    text = "No cloud drawings yet.",
                                    modifier = Modifier.padding(8.dp),
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        else -> {
                            items(cloudDrawings, key = { it.id }) { cloud ->
                                CloudDrawingRow(
                                    drawing = cloud,
                                    onShare = {
                                        shareTargetCloud = cloud
                                        shareError = null
                                        shareEmail = ""
                                    },
                                    onUnshare = {
                                        val sender = user
                                        if (sender == null) {
                                            return@CloudDrawingRow
                                        }

                                        scope.launch {
                                            try {
                                                CloudSharing.unshareDrawing(
                                                    senderId = sender.uid,
                                                    imageUrl = cloud.imageUrl
                                                )
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Shared to me section
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Drawings Shared With You",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }

                    when {
                        isSharedToMeLoading -> {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(color = PaintifyColors.Accent)
                                }
                            }
                        }

                        sharedToMeError != null -> {
                            item {
                                Text(
                                    text = "Error loading shared drawings: $sharedToMeError",
                                    color = PaintifyColors.Error,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        sharedToMe.isEmpty() -> {
                            item {
                                Text(
                                    text = "No one has shared drawings with you yet.",
                                    modifier = Modifier.padding(8.dp),
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        else -> {
                            items(sharedToMe, key = { it.id }) { shared ->
                                SharedDrawingCard(shared)
                            }
                        }
                    }
                }

                if (shareTargetCloud != null) {
                    AlertDialog(
                        onDismissRequest = {
                            shareTargetCloud = null
                            shareError = null
                        },
                        title = { Text("Share Drawing", color = Color.White) },
                        text = {
                            Column {
                                Text(
                                    "Send \"${shareTargetCloud!!.title}\" to:",
                                    color = Color.White
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = shareEmail,
                                    onValueChange = { shareEmail = it },
                                    label = { Text("Recipient email") },
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
                                shareError?.let {
                                    Spacer(Modifier.height(4.dp))
                                    Text(it, color = PaintifyColors.Error)
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                enabled = !isSharing,
                                onClick = {
                                    val sender = user
                                    if (sender == null) {
                                        shareError = "You must be logged in to share."
                                        return@TextButton
                                    }
                                    if (shareEmail.isBlank()) {
                                        shareError = "Please enter an email."
                                        return@TextButton
                                    }

                                    val drawing = shareTargetCloud!!
                                    isSharing = true
                                    shareError = null

                                    scope.launch {
                                        try {
                                            CloudSharing.shareCloudDrawingWithUser(
                                                senderId = sender.uid,
                                                receiverEmail = shareEmail.trim(),
                                                imageUrl = drawing.imageUrl,
                                                title = drawing.title.ifBlank { "Untitled" }
                                            )
                                            shareTargetCloud = null
                                        } catch (e: Exception) {
                                            shareError = e.message ?: "Failed to share"
                                        } finally {
                                            isSharing = false
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    if (isSharing) "Sharing..." else "Share",
                                    color = PaintifyColors.AccentSoft
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    shareTargetCloud = null
                                    shareError = null
                                }
                            ) {
                                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                            }
                        },
                        containerColor = PaintifyColors.SurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawingCard(
    drawing: DrawingData,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
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
                        text = drawing.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Saved: " +
                                java.text.SimpleDateFormat("MMM d, yyyy")
                                    .format(drawing.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = PaintifyColors.Error
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            val file = File(drawing.filePath)
            if (file.exists()) {
                val bmp = remember(file.absolutePath) {
                    BitmapFactory.decodeFile(file.absolutePath)
                }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = drawing.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        "Preview unavailable",
                        color = PaintifyColors.Error
                    )
                }
            } else {
                Text(
                    "Image missing",
                    color = PaintifyColors.Error
                )
            }
        }
    }
}

// Cloud share UI
@Composable
fun SharedDrawingCard(shared: SharedDrawing) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = cardColors(
            containerColor = PaintifyColors.Surface
        ),
        elevation = cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = shared.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            val formatted = remember(shared.timestamp) {
                java.text.SimpleDateFormat("MMM d, yyyy HH:mm")
                    .format(java.util.Date(shared.timestamp))
            }

            Text(
                text = "From: ${shared.senderId.take(8)}… • $formatted",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = shared.imageUrl,
                    contentDescription = shared.title,
                    modifier = Modifier
                        .width(120.dp)
                        .height(190.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun CloudDrawingRow(drawing: CloudDrawing, onShare: () -> Unit, onUnshare: () -> Unit) {
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
                    Text(
                        text = "Uploaded: " +
                                java.text.SimpleDateFormat("MMM d, yyyy HH:mm")
                                    .format(java.util.Date(drawing.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.IosShare,
                        contentDescription = "Share",
                        tint = PaintifyColors.AccentSoft
                    )
                }
                IconButton(onClick = onUnshare) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Unshare cloud drawing",
                        tint = PaintifyColors.Error
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
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


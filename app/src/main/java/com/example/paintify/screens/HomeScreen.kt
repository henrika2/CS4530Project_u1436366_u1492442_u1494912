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
import android.net.Uri
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.example.paintify.DrawApplication
import com.example.paintify.data.DrawingData
import com.example.paintify.data.DrawingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.io.File
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.paintify.cloud.CloudSync
import com.example.paintify.cloud.CloudDrawing
import kotlinx.coroutines.launch

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
    // Auth + coroutine scope
    val auth = Firebase.auth
    val user = auth.currentUser
    val scope = rememberCoroutineScope()

    // Cloud drawings state
    var cloudDrawings by remember { mutableStateOf<List<CloudDrawing>>(emptyList()) }
    var isCloudLoading by remember { mutableStateOf(false) }
    var cloudError by remember { mutableStateOf<String?>(null) }

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
        topBar = { TopAppBar(title = { Text("Paintify — Saved Drawings") }) },
        floatingActionButton = {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Analyze Image button
                ExtendedFloatingActionButton(
                    text = { Text("Analyze Image") },
                    icon = { Icon(Icons.Default.IosShare, contentDescription = "Analyze") },
                    onClick = {
                        pickForAnalysis.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                ExtendedFloatingActionButton(
                    text = { Text("Import") },
                    icon = { Icon(Icons.Default.IosShare, contentDescription = "Import") },
                    onClick = {
                        pickMedia.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                // New drawing (blank canvas)
                FloatingActionButton(onClick = { navController.navigate("canvas") }) {
                    Icon(Icons.Default.Add, contentDescription = "New")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (drawings.isEmpty() && cloudDrawings.isEmpty()) {
                Text("No saved drawings yet. Tap  ➕  to start, or Import from gallery.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(drawings, key = { it.id }) { drawing ->
                        DrawingCard(
                            drawing = drawing,
                            onOpen = { navController.navigate("canvas/${drawing.id}") },
                            onShare = {
                                val file = File(drawing.filePath)
                                if (file.exists()) {
                                    val uri = FileProvider.getUriForFile(
                                        ctx, "${ctx.packageName}.fileprovider", file
                                    )
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    ctx.startActivity(
                                        Intent.createChooser(sendIntent, "Share drawing")
                                    )
                                }
                            },
                            onDelete = { vm.delete(drawing) }
                        )
                    }


                    //NEW: Cloud sync
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Your Cloud Drawings",
                            style = MaterialTheme.typography.titleMedium
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
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        cloudError != null -> {
                            item {
                                Text(
                                    text = "Error loading cloud drawings: $cloudError",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(8.dp)
                                )

                                Log.d("CloudSync", "Error loading cloud drawings: $cloudError")
                            }
                        }

                        cloudDrawings.isEmpty() -> {
                            item {
                                Text(
                                    text = "No cloud drawings yet.",
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        else -> {
                            items(cloudDrawings, key = { it.id }) { cloud ->
                                CloudDrawingRow(cloud)
                            }
                        }
                    }
                }

            }

        }
    }
}

//NEW: Cloud sync
@Composable
fun CloudDrawingRow(drawing: CloudDrawing) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {


            Text(
                text = drawing.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Uploaded: " +
                        java.text.SimpleDateFormat("MMM d, yyyy HH:mm")
                            .format(java.util.Date(drawing.timestamp)),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = drawing.imageUrl,
                    contentDescription = drawing.title,
                    modifier = Modifier
                        .width(100.dp)
                        .height(180.dp),
                    contentScale = ContentScale.FillBounds
                )
            }
        }
    }
}

@Composable
private fun DrawingCard(
    drawing: DrawingData,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = drawing.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Saved: " +
                                java.text.SimpleDateFormat("MMM d, yyyy").format(drawing.createdAt),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.IosShare, contentDescription = "Share")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
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
                            .height(180.dp)
                    )
                } else {
                    Text("Preview unavailable", color = MaterialTheme.colorScheme.error)
                }
            } else {
                Text("Image missing", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

package com.example.paintify.screens

/**
 * Paintify - Detail Screen
 * ------------------------
 * Displays a single saved drawing with metadata and share/delete actions.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * The `DetailViewModel` exposes a single `DrawingData` entry from the
 * repository as a StateFlow. `DetailScreen` shows the full-size bitmap,
 * basic metadata such as pixel dimensions and save date, and provides
 * buttons to share the image via Android's share sheet or delete it from
 * both the database and disk using the `DrawingRepository`.
 */

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.paintify.DrawApplication
import com.example.paintify.data.DrawingData
import com.example.paintify.data.DrawingRepository
import com.example.paintify.ui.PaintifyColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File

class DetailViewModel(
    private val repo: DrawingRepository,
    drawingId: Long
) : ViewModel() {
    val drawing: StateFlow<DrawingData?> = repo.allDrawings
        .map { list -> list.firstOrNull { it.id == drawingId } }
        .stateIn(repo.scope, SharingStarted.WhileSubscribed(5000), null)

    fun delete(d: DrawingData) = repo.deleteDrawing(d, deleteFileOnDisk = true)
}

object DetailViewModelProvider {
    @Composable
    fun provide(drawingId: Long): DetailViewModel {
        val app = LocalContext.current.applicationContext as DrawApplication
        return viewModel(factory = viewModelFactory {
            initializer { DetailViewModel(app.drawingRepository, drawingId) }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    navController: NavHostController,
    drawingId: Long
) {
    val vm = DetailViewModelProvider.provide(drawingId)
    val ctx = LocalContext.current
    val d by vm.drawing.collectAsState()

    Scaffold(
        containerColor = PaintifyColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        d?.name ?: "Drawing",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            d?.let {
                                val file = File(it.filePath)
                                if (file.exists()) {
                                    val uri = FileProvider.getUriForFile(
                                        ctx, "${ctx.packageName}.fileprovider", file
                                    )
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    ctx.startActivity(Intent.createChooser(send, "Share drawing"))
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.IosShare,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            d?.let {
                                vm.delete(it)
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = PaintifyColors.Error
                        )
                    }
                },
                colors = topAppBarColors(
                    containerColor = PaintifyColors.Surface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PaintifyColors.Background)
                .padding(pad)
        ) {
            if (d == null) {
                // Empty state
            } else {
                val file = File(d!!.filePath)
                val bmp = remember(file.absolutePath) {
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = cardColors(
                            containerColor = PaintifyColors.SurfaceVariant
                        ),
                        elevation = cardElevation(8.dp)
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = d!!.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Image not found on disk.",
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = cardColors(
                            containerColor = PaintifyColors.Surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Details",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                "Size: ${d!!.widthPx} × ${d!!.heightPx}px",
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                "Saved: " +
                                        java.text.SimpleDateFormat("MMM d, yyyy")
                                            .format(d!!.createdAt),
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}


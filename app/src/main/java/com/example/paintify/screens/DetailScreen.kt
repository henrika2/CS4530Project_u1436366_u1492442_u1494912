package com.example.paintify.screens

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        topBar = {
            TopAppBar(
                title = { Text(d?.name ?: "Drawing") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    ) { Icon(Icons.Default.IosShare, contentDescription = "Share") }

                    IconButton(
                        onClick = {
                            d?.let {
                                vm.delete(it)
                                navController.popBackStack()
                            }
                        }
                    ) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
            )
        }
    ) { pad ->
        if (d == null) {
            Box(Modifier.fillMaxSize().padding(pad))
        } else {
            val file = File(d!!.filePath)
            val bmp = remember(file.absolutePath) {
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .padding(16.dp)
            ) {
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = d!!.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    Text("Image not found on disk.")
                }
                Spacer(Modifier.height(12.dp))
                Text("Size: ${d!!.widthPx} × ${d!!.heightPx}px")
                Text("Saved: " + java.text.SimpleDateFormat("MMM d, yyyy").format(d!!.createdAt))
            }
        }
    }
}

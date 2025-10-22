//// File: com/example/paintify/screens/HomeScreen.kt
//package com.example.paintify.screens
//
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material3.Card
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.asImageBitmap
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavHostController
//import com.example.paintify.data.DrawingData
//import com.example.paintify.data.DrawingRepository
//import android.graphics.BitmapFactory
//import java.io.File
//
//// DrawingHomeViewModel.kt
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.stateIn
//
//class DrawingHomeViewModel(
//    private val repository: DrawingRepository
//) : ViewModel() {
//    val allDrawings: StateFlow<List<DrawingData>> = repository.allDrawings.stateIn(
//        scope = repository.scope,
//        started = SharingStarted.WhileSubscribed(),
//        initialValue = listOf()
//    )
//}
//
//
//@Composable
//fun HomeScreen(
//    navController: NavHostController,
////    vm: DrawingHomeViewModel = viewModel()
//) {
//    val drawings by vm.allDrawings.collectAsState()
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(MaterialTheme.colorScheme.background)
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text(
//            text = "Saved Drawings",
//            style = MaterialTheme.typography.headlineSmall,
//            modifier = Modifier.padding(vertical = 8.dp)
//        )
//
//        if (drawings.isEmpty()) {
//            Text("No saved drawings yet.")
//        } else {
//            LazyColumn(
//                modifier = Modifier.fillMaxSize(),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                items(drawings) { drawing ->
//                    DrawingCard(drawing = drawing) {
//                        // TODO: open drawing detail, or navigate to DrawScreen with id
//                        // navController.navigate("draw/${drawing.id}")
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun DrawingCard(drawing: DrawingData, onClick: () -> Unit) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable(onClick = onClick)
//            .padding(horizontal = 8.dp)
//    ) {
//        Column(modifier = Modifier.padding(12.dp)) {
//            Text(
//                text = drawing.name,
//                style = MaterialTheme.typography.titleMedium
//            )
//            Text(
//                text = "Saved: ${java.text.SimpleDateFormat("MMM d, yyyy").format(drawing.createdAt)}",
//                style = MaterialTheme.typography.bodySmall
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            val file = File(drawing.filePath)
//            if (file.exists()) {
//                val bmp = BitmapFactory.decodeFile(file.absolutePath)
//                Image(
//                    bitmap = bmp.asImageBitmap(),
//                    contentDescription = drawing.name,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(180.dp)
//                )
//            } else {
//                Text("Image missing", color = MaterialTheme.colorScheme.error)
//            }
//        }
//    }
//}
//
//


//package com.example.paintify.screens
//
//import android.graphics.BitmapFactory
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.asImageBitmap
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.lifecycle.viewmodel.initializer
//import androidx.lifecycle.viewmodel.viewModelFactory
//import com.example.paintify.data.DrawingData
//import com.example.paintify.data.DrawingRepository
//import com.example.paintify.DrawApplication
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.stateIn
//import java.io.File
//import java.text.SimpleDateFormat
//import java.util.*
//import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
//import androidx.lifecycle.viewmodel.viewModelFactory
//import androidx.lifecycle.viewmodel.initializer
//
//
//class DrawingHomeViewModel(private val repository: DrawingRepository) : ViewModel() {
//    class DrawingHomeViewModel(private val repository: DrawingRepository) : ViewModel() {
//        val allDrawings: StateFlow<List<DrawingData>> =
//            repository.allDrawings.stateIn(
//                scope = viewModelScope,
//                started = SharingStarted.WhileSubscribed(5000),
//                initialValue = emptyList()
//            )
//
//        companion object {
//            val Factory = viewModelFactory {
//                initializer {
//                    val app = (this[APPLICATION_KEY] as DrawApplication)
//                    DrawingHomeViewModel(app.drawingRepository)
//                }
//            }
//        }
//    }
//
//}
//
//@Composable
//fun HomeScreen(
//    onNewDrawing: () -> Unit,
//    onOpenDrawing: (Long) -> Unit,
//    vm: DrawingHomeViewModel = viewModel(factory = DrawingHomeViewModel.Factory)
//) {
//    val drawings by vm.allDrawings.collectAsState()
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Paintify — My Drawings") }
//            )
//        },
//        floatingActionButton = {
//            ExtendedFloatingActionButton(
//                onClick = onNewDrawing,
//                text = { Text("New Drawing") }
//            )
//        }
//    ) { padding ->
//        if (drawings.isEmpty()) {
//            Box(
//                Modifier
//                    .fillMaxSize()
//                    .padding(padding)
//                    .background(MaterialTheme.colorScheme.background),
//                contentAlignment = Alignment.Center
//            ) { Text("No saved drawings yet. Tap “New Drawing”.") }
//        } else {
//            LazyColumn(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(padding),
//                verticalArrangement = Arrangement.spacedBy(10.dp),
//                contentPadding = PaddingValues(12.dp)
//            ) {
//                items(drawings, key = { it.id }) { d ->
//                    DrawingListItem(d, onClick = { onOpenDrawing(d.id) })
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun DrawingListItem(d: DrawingData, onClick: () -> Unit) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable(onClick = onClick)
//    ) {
//        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
//            val file = remember(d.filePath) { File(d.filePath) }
//            val thumb = remember(file) {
//                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
//            }
//            if (thumb != null) {
//                Image(
//                    bitmap = thumb.asImageBitmap(),
//                    contentDescription = d.name,
//                    modifier = Modifier
//                        .size(72.dp)
//                        .padding(end = 12.dp)
//                )
//            } else {
//                Box(
//                    Modifier
//                        .size(72.dp)
//                        .padding(end = 12.dp)
//                        .background(MaterialTheme.colorScheme.surfaceVariant)
//                )
//            }
//
//            Column(Modifier.weight(1f)) {
//                Text(d.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
//                val sdf = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }
//                Text("Saved: ${sdf.format(Date(d.createdAt))}", style = MaterialTheme.typography.bodySmall)
//            }
//        }
//    }
//}

package com.example.paintify.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.paintify.DrawApplication
import com.example.paintify.data.DrawingData
import com.example.paintify.data.DrawingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.FloatingActionButton


/**
 * ViewModel for the Home screen: exposes a flow of all drawings.
 */
class DrawingHomeViewModel(
    private val repository: DrawingRepository
) : ViewModel() {

    val allDrawings: StateFlow<List<DrawingData>> =
        repository.allDrawings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DrawApplication
                DrawingHomeViewModel(app.drawingRepository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewDrawing: () -> Unit,
    onOpenDrawing: (Long) -> Unit,
    vm: DrawingHomeViewModel = viewModel(factory = DrawingHomeViewModel.Factory)
) {
    val drawings by vm.allDrawings.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Paintify — My Drawings") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewDrawing) {
                Text("+")
            }
        },
        bottomBar = { BottomAppBar {} }
    ) { padding ->
        if (drawings.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text("No saved drawings yet. Tap “New Drawing”.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(drawings, key = { it.id }) { d ->
                    DrawingListItem(drawing = d, onClick = { onOpenDrawing(d.id) })
                }
            }
        }
    }
}

@Composable
private fun DrawingListItem(drawing: DrawingData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            // thumbnail (if file exists)
            val file = remember(drawing.filePath) { File(drawing.filePath) }
            val thumb = remember(file) {
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }

            if (thumb != null) {
                Image(
                    bitmap = thumb.asImageBitmap(),
                    contentDescription = drawing.name,
                    modifier = Modifier
                        .size(72.dp)
                        .padding(end = 12.dp)
                )
            } else {
                Box(
                    Modifier
                        .size(72.dp)
                        .padding(end = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = drawing.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sdf = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }
                Text(
                    text = "Saved: ${sdf.format(Date(drawing.createdAt))}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

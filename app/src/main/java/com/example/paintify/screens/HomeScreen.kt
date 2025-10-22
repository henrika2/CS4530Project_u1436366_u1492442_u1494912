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

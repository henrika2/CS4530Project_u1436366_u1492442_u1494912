package com.example.paintify.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.paintify.ui.BrushType
import com.example.paintify.ui.DrawingCanvas
import com.example.paintify.ui.Stroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clipToBounds

// ===== ViewModel (persists across rotations) =====
class DrawingViewModel : ViewModel() {
    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes

    private val _selectedBrush = MutableStateFlow(BrushType.CIRCLE)
    val selectedBrush: StateFlow<BrushType> = _selectedBrush

    private val _selectedColor = MutableStateFlow(Color.Black)
    val selectedColor: StateFlow<Color> = _selectedColor

    // active stroke while dragging
    private var currentPoints: List<Offset> = emptyList()
    private var activeBrush: BrushType = _selectedBrush.value
    private var activeColor: Color = _selectedColor.value

    fun setBrush(b: BrushType) { _selectedBrush.value = b }
    fun setColor(c: Color) { _selectedColor.value = c }

    fun onDragStart(offset: Offset) {
        currentPoints = listOf(offset)
        activeBrush = _selectedBrush.value
        activeColor = _selectedColor.value
        _strokes.update { it + Stroke(points = currentPoints, brush = activeBrush, color = activeColor) }
    }

    fun onDragMove(offset: Offset) {
        currentPoints = currentPoints + offset
        _strokes.update { list ->
            list.dropLast(1) + Stroke(points = currentPoints, brush = activeBrush, color = activeColor)
        }
    }

    fun onDragEnd() { currentPoints = emptyList() }
}

@Composable
fun DrawScreen(
    navController: NavHostController,
    vm: DrawingViewModel = viewModel()
) {
    val strokes by vm.strokes.collectAsState()
    val selectedBrush by vm.selectedBrush.collectAsState()
    val selectedColor by vm.selectedColor.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(100.dp))

        // Canvas area (square)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White) // optional
        ) {
            // Stateless canvas: draws strokes and sends drag callbacks
            DrawingCanvas(
                strokes = strokes,
                onStart = vm::onDragStart,
                onMove = vm::onDragMove,
                onEnd = vm::onDragEnd
            )

            // === Six inline buttons, anchored to bottom-right of the canvas ===
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Brush buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.setBrush(BrushType.LINE) },
                        enabled = selectedBrush != BrushType.LINE
                    ) { Text("LINE") }

                    Button(
                        onClick = { vm.setBrush(BrushType.CIRCLE) },
                        enabled = selectedBrush != BrushType.CIRCLE
                    ) { Text("CIRCLE") }

                    Button(
                        onClick = { vm.setBrush(BrushType.RECTANGLE) },
                        enabled = selectedBrush != BrushType.RECTANGLE
                    ) { Text("RECTANGLE") }
                }

                // Color buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    Button(
                        onClick = { vm.setColor(Color.Black) },
                        enabled = selectedColor != Color.Black,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) { Text("BLACK")}

                    Button(
                        onClick = { vm.setColor(Color.Red) },
                        enabled = selectedColor != Color.Red,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("RED") }

                    Button(
                        onClick = { vm.setColor(Color.Blue) },
                        enabled = selectedColor != Color.Blue,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                    ) { Text("BLUE") }
                }
            }
        }
    }
}

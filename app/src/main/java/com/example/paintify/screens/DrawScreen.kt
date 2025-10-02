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
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import androidx.compose.material3.Slider


import com.example.paintify.ui.BrushType
import com.example.paintify.ui.DrawingCanvas
import com.example.paintify.ui.Stroke

import com.example.paintify.models.ToolType
import com.example.paintify.models.ShapeType
import com.example.paintify.models.PaintParams
import com.example.paintify.models.CanvasStroke

class DrawingViewModel : ViewModel() {

    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes

    private val _selectedBrush = MutableStateFlow(BrushType.CIRCLE)
    val selectedBrush: StateFlow<BrushType> = _selectedBrush

    private val _selectedColor = MutableStateFlow(Color.Black)
    val selectedColor: StateFlow<Color> = _selectedColor

    private val tool = MutableStateFlow(ToolType.PEN)
    private val penWidthPx = MutableStateFlow(12f)
    val penWidth: StateFlow<Float> = penWidthPx

    private val domainStrokes = MutableStateFlow<List<CanvasStroke>>(emptyList())

    private var currentPoints: List<Offset> = emptyList()
    private var nextId: Long = 1L

    fun setBrush(b: BrushType) {
        _selectedBrush.value = b
        tool.value = ToolType.PEN
    }

    fun setColor(c: Color) {
        _selectedColor.value = c
        tool.value = if (c == Color.White) ToolType.ERASER else ToolType.PEN
    }
    fun setWidthPx(width: Float) {
        penWidthPx.value = width.coerceIn(1f, 128f)
    }
    fun onDragStart(offset: Offset) {
        currentPoints = listOf(offset)

        val shape = when (_selectedBrush.value) {
            BrushType.LINE      -> ShapeType.LineShape
            BrushType.CIRCLE    -> ShapeType.CircleShape
            BrushType.RECTANGLE -> ShapeType.RectShape
        }

        val params = PaintParams(
            color = _selectedColor.value,
            widthPx = penWidthPx.value,
            isEraser = (tool.value == ToolType.ERASER)
        )

        val domain = CanvasStroke(
            id = nextId++,
            shapeType = shape,
            points = currentPoints,
            paint = params
        )
        domainStrokes.value = domainStrokes.value + domain
        pushUiMirror()
    }
    fun onDragMove(offset: Offset) {
        currentPoints = currentPoints + offset
        domainStrokes.update { list ->
            list.dropLast(1) + list.last().copy(points = currentPoints)
        }
        pushUiMirror()
    }
    fun onDragEnd() {
        currentPoints = emptyList()
    }
    private fun pushUiMirror() {
        _strokes.value = domainStrokes.value.map { s ->
            val uiBrush = when (s.shapeType) {
                ShapeType.LineShape   -> BrushType.LINE
                ShapeType.CircleShape -> BrushType.CIRCLE
                ShapeType.RectShape   -> BrushType.RECTANGLE
                else -> {}
            }
            Stroke(
                points = s.points,
                brush = uiBrush as BrushType,
                color = s.paint.color,
                widthPx = s.paint.widthPx
            )
        }
    }
}

@Composable
fun DrawScreen(
    navController: NavHostController,
    vm: DrawingViewModel = viewModel()
) {
    val strokes by vm.strokes.collectAsState()
    val selectedBrush by vm.selectedBrush.collectAsState()
    val selectedColor by vm.selectedColor.collectAsState()
    val penWidth by vm.penWidth.collectAsState()


    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(100.dp))

        // Canvas area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Stateless canvas: draws strokes and sends drag callbacks
            DrawingCanvas(
                strokes = strokes,
                onStart = vm::onDragStart,
                onMove = vm::onDragMove,
                onEnd = vm::onDragEnd
            )

            // Bottom-right controls (unchanged)
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

                // CHANGE: Pen width slider (affects NEW strokes)
                Column(horizontalAlignment = Alignment.End) {
                    Text("Pen: ${penWidth.toInt()} px")
                    Slider(
                        value = penWidth,
                        onValueChange = vm::setWidthPx,
                        valueRange = 1f..64f
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.setColor(Color.Black) },
                        enabled = selectedColor != Color.Black,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) { Text("BLACK") }

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

                    Button(
                        onClick = { vm.setColor(Color.White) },
                        enabled = selectedColor != Color.White,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                    ) { Text("ERASER") }

                }
            }
        }
    }
}







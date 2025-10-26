///**
// * Paintify - Drawing Screen
// * --------------------------
// * This file defines the `DrawScreen` composable and `DrawingViewModel` class
// * that together handle drawing functionality, including shape selection,
// * color selection, pen width adjustment, and real-time stroke updates.
// *
// * Group Members:
// *  - Dustin
// *  - Nolan
// *  - Ian
// *
// * Description:
// * The screen provides a drawable canvas area using Jetpack Compose's Canvas API.
// * The ViewModel (`DrawingViewModel`) manages state for strokes, brushes, colors,
// * and drawing tools, ensuring UI-state synchronization through Kotlin Flows.
// */
//
//package com.example.paintify.screens
//
//import androidx.activity.viewModels
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.testTag
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.lifecycle.viewmodel.initializer
//import androidx.lifecycle.viewmodel.viewModelFactory
//import androidx.navigation.NavHostController
//import com.example.paintify.data.DrawingData
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.update
//
//import com.example.paintify.ui.DrawingCanvas
//import com.example.paintify.models.Stroke
//import com.example.paintify.models.ToolType
//import com.example.paintify.models.ShapeType
//import com.example.paintify.models.PaintParams
//import com.example.paintify.models.CanvasStroke
//import com.example.paintify.data.DrawingRepository
//import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
//import com.example.paintify.DrawApplication
//import androidx.activity.viewModels
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ColorLens
//import androidx.compose.material.icons.filled.Save
//import androidx.compose.material.icons.filled.Brush
//import androidx.compose.material3.DropdownMenu
//import androidx.compose.material3.DropdownMenuItem
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.geometry.Size as GSize
//import androidx.compose.ui.layout.onSizeChanged
//import androidx.compose.ui.unit.IntSize
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.window.Dialog
//import kotlin.math.roundToInt
//
//
//
///**
// * ViewModel class responsible for managing all drawing-related states
// * such as brush type, color, pen width, and drawn strokes.
// */
//class DrawingViewModel(private val repository: DrawingRepository) : ViewModel() {
//
//    // Stores the strokes currently displayed on the canvas
//    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
//    val strokes: StateFlow<List<Stroke>> = _strokes
//
//    // Stores the selected brush shape (Line, Circle, Rectangle)
//    private val _selectedBrush = MutableStateFlow(ShapeType.CIRCLE)
//    val selectedBrush: StateFlow<ShapeType> = _selectedBrush
//
//    // Stores the selected drawing color
//    private val _selectedColor = MutableStateFlow(Color.Black)
//    val selectedColor: StateFlow<Color> = _selectedColor
//
//    // Tracks the current tool type (Pen or Eraser)
//    private val tool = MutableStateFlow(ToolType.PEN)
//
//    // Controls the current pen width in pixels
//    private val penWidthPx = MutableStateFlow(12f)
//    val penWidth: StateFlow<Float> = penWidthPx
//
//    // Internal list representing the domain strokes (not UI-mirrored yet)
//    private val domainStrokes = MutableStateFlow<List<CanvasStroke>>(emptyList())
//
//    private var currentPoints: List<Offset> = emptyList()
//
//    /**
//     * Updates the selected brush shape.
//     */
//    fun setBrush(b: ShapeType) {
//        _selectedBrush.value = b
//        tool.value = ToolType.PEN
//    }
//
//    /**
//     * Updates the selected color and tool type (switches to eraser if white).
//     */
//    fun setColor(c: Color) {
//        _selectedColor.value = c
//        tool.value = if (c == Color.White) ToolType.ERASER else ToolType.PEN
//    }
//
//    /**
//     * Updates the pen width, clamped between 1 and 128 pixels.
//     */
//    fun setWidthPx(width: Float) {
//        penWidthPx.value = width.coerceIn(1f, 128f)
//    }
//
//    /**
//     * Called when a drag (drawing) starts; initializes a new stroke.
//     */
//    fun onDragStart(offset: Offset) {
//        currentPoints = listOf(offset)
//        val params = PaintParams(
//            color = _selectedColor.value,
//            widthPx = penWidthPx.value,
//            isEraser = (tool.value == ToolType.ERASER)
//        )
//        val domain = CanvasStroke(
//            shapeType = _selectedBrush.value,
//            points = currentPoints,
//            paint = params
//        )
//        domainStrokes.value = domainStrokes.value + domain
//        pushUiMirror()
//    }
//
//    /**
//     * Called while dragging; adds points to the active stroke.
//     */
//    fun onDragMove(offset: Offset) {
//        currentPoints = currentPoints + offset
//        domainStrokes.update { list ->
//            list.dropLast(1) + list.last().copy(points = currentPoints)
//        }
//        pushUiMirror()
//    }
//
//    /**
//     * Called when dragging ends; finalizes the stroke.
//     */
//    fun onDragEnd() {
//        currentPoints = emptyList()
//    }
//
//    /**
//     * Converts domain strokes to UI strokes and updates the UI state.
//     */
//    private fun pushUiMirror() {
//        _strokes.value = domainStrokes.value.map { s ->
//            Stroke(
//                points = s.points,
//                brush = s.shapeType!!,
//                color = s.paint.color,
//                widthPx = s.paint.widthPx
//            )
//        }
//    }
//}
//
///**
// * DrawScreen composable function.
// *
// * This function builds the UI for the drawing screen:
// * - A top app bar titled "Canvas"
// * - A white drawing area that fills the remaining space
// * - A bottom control panel for selecting brush, color, and width
// * - A slim bottom bar for consistent visual design
// */
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun DrawScreen(
//    navController: NavHostController,
//    vm: DrawingViewModel = viewModel(
//        factory = DrawingViewModelProvider.Factory
//    )
//) {
//    val strokes by vm.strokes.collectAsState()
//    val selectedBrush by vm.selectedBrush.collectAsState()
//    val selectedColor by vm.selectedColor.collectAsState()
//    val penWidth by vm.penWidth.collectAsState()
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = Color(0xFFE5E5E5),
//                    titleContentColor = Color.Black
//                ),
//                title = { Text("Canvas") }
//            )
//        },
//        bottomBar = {
//            BottomAppBar(
//                modifier = Modifier.height(10.dp),
//                containerColor = Color(0xFFE5E5E5)
//            ) { /* Decorative bar only */ }
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            // Canvas Area
//            Box(
//                modifier = Modifier
//                    .weight(1f)
//                    .fillMaxWidth()
//                    .background(Color.White)
//            ) {
//                DrawingCanvas(
//                    strokes = strokes,
//                    onStart = vm::onDragStart,
//                    onMove = vm::onDragMove,
//                    onEnd = vm::onDragEnd,
//                    modifier = Modifier.fillMaxSize()
//                )
//            }
//
//            // Tools Section
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .background(Color(0xFFE5E5E5))
//                    .padding(horizontal = 12.dp, vertical = 10.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                // Brush Selection Buttons
//                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                    Button(
//                        onClick = { vm.setBrush(ShapeType.LINE) },
//                        enabled = selectedBrush != ShapeType.LINE
//                    ) { Text("LINE") }
//
//                    Button(
//                        onClick = { vm.setBrush(ShapeType.CIRCLE) },
//                        enabled = selectedBrush != ShapeType.CIRCLE
//                    ) { Text("CIRCLE") }
//
//                    Button(
//                        onClick = { vm.setBrush(ShapeType.RECT) },
//                        enabled = selectedBrush != ShapeType.RECT
//                    ) { Text("RECTANGLE") }
//                }
//
//                // Pen Width Slider
//                Column(Modifier.testTag("penWidthSlider"), horizontalAlignment = Alignment.End) {
//                    Text("Pen: ${penWidth.toInt()} px")
//                    Slider(
//                        value = penWidth,
//                        onValueChange = vm::setWidthPx,
//                        valueRange = 1f..64f
//                    )
//                }
//
//                // Color Buttons
//                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
//                    Button(
//                        onClick = { vm.setColor(Color.Black) },
//                        enabled = selectedColor != Color.Black,
//                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
//                    ) { Text("BLACK") }
//
//                    Button(
//                        onClick = { vm.setColor(Color.Red) },
//                        enabled = selectedColor != Color.Red,
//                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
//                    ) { Text("RED") }
//
//                    Button(
//                        onClick = { vm.setColor(Color.Blue) },
//                        enabled = selectedColor != Color.Blue,
//                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
//                    ) { Text("BLUE") }
//
//                    Button(
//                        onClick = { vm.setColor(Color.White) },
//                        enabled = selectedColor != Color.White,
//                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
//                    ) { Text("ERASER", color = Color.Gray) }
//                }
//            }
//        }
//    }
//}
//
///**
//
//Provides a [ViewModelProvider.Factory] for creating the [CourseViewModel].*
//This factory retrieves the [CourseRepository] from the [CourseApplication]
//singleton and supplies it to the ViewModel constructor.*
//This ensures lifecycle-aware dependency injection across the app.*/
//object DrawingViewModelProvider {
//    val Factory = viewModelFactory {
//        initializer {
//            DrawingViewModel(
//                //fetches the application singleton
//                (this[AndroidViewModelFactory.APPLICATION_KEY]
//                        //and then extracts the repository in it
//                        as DrawApplication).drawingRepository
//            )
//        }
//    }
//}


package com.example.paintify.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.example.paintify.DrawApplication
import com.example.paintify.data.DrawingRepository
import com.example.paintify.models.CanvasStroke
import com.example.paintify.models.PaintParams
import com.example.paintify.models.ShapeType
import com.example.paintify.models.Stroke
import com.example.paintify.models.ToolType
import com.example.paintify.ui.DrawingCanvas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.filled.AutoFixOff   // <— eraser icon



/**
 * ViewModel class responsible for managing all drawing-related states
 * such as brush type, color, pen width, and drawn strokes.
 */
class DrawingViewModel(private val repository: DrawingRepository) : ViewModel() {

    // Stores the strokes currently displayed on the canvas
    private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
    val strokes: StateFlow<List<Stroke>> = _strokes

    // Stores the selected brush shape (Line, Circle, Rectangle)
    private val _selectedBrush = MutableStateFlow(ShapeType.CIRCLE)
    val selectedBrush: StateFlow<ShapeType> = _selectedBrush

    // Stores the selected drawing color
    private val _selectedColor = MutableStateFlow(Color.Black)
    val selectedColor: StateFlow<Color> = _selectedColor

    // Tracks the current tool type (Pen or Eraser)
    private val tool = MutableStateFlow(ToolType.PEN)

    // Controls the current pen width in pixels
    private val penWidthPx = MutableStateFlow(12f)
    val penWidth: StateFlow<Float> = penWidthPx

    // Internal list representing the domain strokes (not UI-mirrored yet)
    private val domainStrokes = MutableStateFlow<List<CanvasStroke>>(emptyList())

    private var currentPoints: List<Offset> = emptyList()

    // ADD: expose tool type so UI can show toggle state
    val toolType: StateFlow<ToolType> = tool

    // ADD: dedicated toggles for toolbar
    fun setEraser() {
        // we use white = eraser to match your white canvas background
        _selectedColor.value = Color.White
        tool.value = ToolType.ERASER
    }
    fun setPen() {
        if (tool.value == ToolType.ERASER) {
            tool.value = ToolType.PEN
            // keep whatever color user had (except white) – do nothing else here
        }
    }

    /**
     * Updates the selected brush shape.
     */
    fun setBrush(b: ShapeType) {
        _selectedBrush.value = b
        tool.value = ToolType.PEN
    }

    /**
     * Updates the selected color and tool type (switches to eraser if white).
     */
    fun setColor(c: Color) {
        _selectedColor.value = c
        tool.value = if (c == Color.White) ToolType.ERASER else ToolType.PEN
    }

    /**
     * Updates the pen width, clamped between 1 and 128 pixels.
     */
    fun setWidthPx(width: Float) {
        penWidthPx.value = width.coerceIn(1f, 128f)
    }

    /**
     * Called when a drag (drawing) starts; initializes a new stroke.
     */
    fun onDragStart(offset: Offset) {
        currentPoints = listOf(offset)
        val params = PaintParams(
            color = _selectedColor.value,
            widthPx = penWidthPx.value,
            isEraser = (tool.value == ToolType.ERASER)
        )
        val domain = CanvasStroke(
            shapeType = _selectedBrush.value,
            points = currentPoints,
            paint = params
        )
        domainStrokes.value = domainStrokes.value + domain
        pushUiMirror()
    }

    /**
     * Called while dragging; adds points to the active stroke.
     */
    fun onDragMove(offset: Offset) {
        currentPoints = currentPoints + offset
        domainStrokes.update { list ->
            list.dropLast(1) + list.last().copy(points = currentPoints)
        }
        pushUiMirror()
    }

    /**
     * Called when dragging ends; finalizes the stroke.
     */
    fun onDragEnd() {
        currentPoints = emptyList()
    }

    /**
     * Converts domain strokes to UI strokes and updates the UI state.
     */
    private fun pushUiMirror() {
        _strokes.value = domainStrokes.value.map { s ->
            Stroke(
                points = s.points,
                brush = s.shapeType!!,
                color = s.paint.color,
                widthPx = s.paint.widthPx
            )
        }
    }

    /** NEW: Save current strokes to PNG + DB via repository */
    fun saveCurrent(name: String, widthPx: Int, heightPx: Int) {
        repository.saveDrawing(
            name = name,
            strokes = _strokes.value,
            canvasWidthPx = widthPx,
            canvasHeightPx = heightPx
        )
    }
}

/**
 * DrawScreen composable with compact controls (dropdown, slider, color picker)
 * and a Save button in the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawScreen(
    navController: NavHostController,
    vm: DrawingViewModel = viewModel(factory = DrawingViewModelProvider.Factory)
) {
    val ctx = LocalContext.current
    val strokes by vm.strokes.collectAsState()
    val selectedBrush by vm.selectedBrush.collectAsState()
    val selectedColor by vm.selectedColor.collectAsState()
    val penWidth by vm.penWidth.collectAsState()
    val toolType by vm.toolType.collectAsState()


    var canvasSize by remember { mutableStateOf(IntSize(0, 0)) }
    var shapeMenuExpanded by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var drawingName by remember { mutableStateOf("My Drawing") }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE5E5E5),
                    titleContentColor = Color.Black
                ),
                title = { Text("Canvas") },
                actions = {
                    IconButton(onClick = { showColorPicker = true }) {
                        Icon(Icons.Default.ColorLens, contentDescription = "Color")
                    }
                    IconButton(onClick = { shapeMenuExpanded = true }) {
                        Icon(Icons.Default.Brush, contentDescription = "Brush shape")
                    }
                    DropdownMenu(
                        expanded = shapeMenuExpanded,
                        onDismissRequest = { shapeMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Line") },
                            onClick = { vm.setBrush(ShapeType.LINE); shapeMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Circle") },
                            onClick = { vm.setBrush(ShapeType.CIRCLE); shapeMenuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Rectangle") },
                            onClick = { vm.setBrush(ShapeType.RECT); shapeMenuExpanded = false }
                        )
                    }

                    IconToggleButton(
                        checked = (toolType == ToolType.ERASER),
                        onCheckedChange = { isOn ->
                            if (isOn) vm.setEraser() else vm.setPen()
                        }
                    ) {
                        // Use a recognizable "eraser" looking icon; AutoFixOff works well as an eraser metaphor
                        Icon(
                            imageVector = Icons.Default.AutoFixOff,
                            contentDescription = if (toolType == ToolType.ERASER) "Eraser on" else "Eraser off",
                            tint = if (toolType == ToolType.ERASER) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }


                    IconButton(
                        onClick = {
                            if (canvasSize.width > 0 && canvasSize.height > 0) {
                                vm.saveCurrent(
                                    name = drawingName,
                                    widthPx = canvasSize.width,
                                    heightPx = canvasSize.height
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(60.dp),
                containerColor = Color(0xFFE5E5E5)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Pen width: ${penWidth.toInt()} px")
                        Slider(
                            value = penWidth,
                            onValueChange = vm::setWidthPx,
                            valueRange = 1f..64f
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
                    .onSizeChanged { canvasSize = it }
            ) {
                DrawingCanvas(
                    strokes = strokes,
                    onStart = vm::onDragStart,
                    onMove = vm::onDragMove,
                    onEnd = vm::onDragEnd,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialogRGB(
            initial = selectedColor,
            onPick = { vm.setColor(it); showColorPicker = false },
            onDismiss = { showColorPicker = false }
        )
    }
}

/** Provider (unchanged pattern) */
object DrawingViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            DrawingViewModel(
                (this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as DrawApplication).drawingRepository
            )
        }
    }
}


@Composable
private fun ColorPreview(color: Color, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.size(56.dp)) {
        Box(Modifier.fillMaxSize().background(color))
    }
}


@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(Modifier.fillMaxWidth()) {
        Text("$label: ${value.toInt()}")
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}



@Composable
fun ColorPickerDialogRGB(
    initial: Color,
    onDismiss: () -> Unit,
    onPick: (Color) -> Unit
) {
    // Convert initial color (0–1 floats) to 0–255 ints
    var r by remember { mutableFloatStateOf(initial.red * 255f) }
    var g by remember { mutableFloatStateOf(initial.green * 255f) }
    var b by remember { mutableFloatStateOf(initial.blue * 255f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(Color(r / 255f, g / 255f, b / 255f)) }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Pick a Color (RGB)") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColorPreview(color = initial)
                    Text("→")
                    ColorPreview(color = Color(r / 255f, g / 255f, b / 255f))
                }

                // RGB sliders
                ColorSlider("RED", r, { r = it }, 0f..255f)
                ColorSlider("GREEN", g, { g = it }, 0f..255f)
                ColorSlider("BLUE", b, { b = it }, 0f..255f)
            }
        }
    )
}

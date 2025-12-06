/**
 * Paintify - Drawing Screen
 * --------------------------
 * This file defines the `DrawScreen` composable and `DrawingViewModel` class
 * that together handle drawing functionality, including shape selection,
 * color selection, pen width adjustment, and real-time stroke updates.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * The screen provides a drawable canvas area using Jetpack Compose's Canvas API.
 * The ViewModel (`DrawingViewModel`) manages state for strokes, brushes, colors,
 * and drawing tools, ensuring UI-state synchronization through Kotlin Flows.
 */
package com.example.paintify.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixOff
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.CardDefaults.cardElevation
//import androidx.compose.material3.ExposedDropdownMenuDefaults.textFieldColors
import androidx.compose.material3.TextFieldDefaults

import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
//import androidx.compose.material3.TextFieldDefaults.textFieldColors
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.example.paintify.DrawApplication
import com.example.paintify.cloud.CloudSync
import com.example.paintify.data.DrawingData
import com.example.paintify.data.DrawingRepository
import com.example.paintify.models.CanvasStroke
import com.example.paintify.models.PaintParams
import com.example.paintify.models.ShapeType
import com.example.paintify.models.Stroke
import com.example.paintify.models.ToolType
import com.example.paintify.ui.DrawingCanvas
import com.example.paintify.ui.PaintifyColors
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * Paintify - Drawing Screen
 * -------------------------
 * Hosts the main canvas UI and drawing logic for freehand shapes, colors,
 * erasing, and pen width control.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * Defines `DrawingViewModel` plus `DrawScreen` / `DrawScreenWithBackground`,
 * which manage strokes, tools, colors, and saving merged drawings through
 * the `DrawingRepository`.
 */
class DrawingViewModel(private val repository: DrawingRepository) : ViewModel() {
    val selectedDrawing: StateFlow<DrawingData?> =
        repository.selectedDrawing.stateIn(
            repository.scope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    fun saveCurrentMerged(name: String, widthPx: Int, heightPx: Int) {
        val strokesNow = strokes.value
        val bgPath = selectedDrawing.value?.filePath
        if (!bgPath.isNullOrBlank()) {
            repository.saveDrawingMerged(
                name = name,
                backgroundFilePath = bgPath,
                strokes = strokesNow,
                canvasWidthPx = widthPx,
                canvasHeightPx = heightPx
            )
        } else {
            repository.saveDrawing(
                name = name,
                strokes = strokesNow,
                canvasWidthPx = widthPx,
                canvasHeightPx = heightPx
            )
        }
    }

    fun setSelectedDrawingId(id: Long) = repository.setSelectedDrawingId(id)

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

    // expose tool type so UI can show toggle state
    val toolType: StateFlow<ToolType> = tool

    fun setEraser() {
        _selectedColor.value = Color.White
        tool.value = ToolType.ERASER
    }

    fun setPen() {
        if (tool.value == ToolType.ERASER) {
            tool.value = ToolType.PEN
            // keep previous color
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

    /** Save current strokes to PNG + DB via repository */
    fun saveCurrent(name: String, widthPx: Int, heightPx: Int) {
        val strokesNow = strokes.value
        val bgPath = selectedDrawing.value?.filePath
        if (!bgPath.isNullOrBlank()) {
            repository.saveDrawingMerged(
                name = name,
                backgroundFilePath = bgPath,
                strokes = strokesNow,
                canvasWidthPx = widthPx,
                canvasHeightPx = heightPx
            )
        } else {
            repository.saveDrawing(
                name = name,
                strokes = strokesNow,
                canvasWidthPx = widthPx,
                canvasHeightPx = heightPx
            )
        }
    }

    fun setSelectedDrawing(drawingId: Long) {
        repository.setSelectedDrawingId(drawingId)
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

/**
 * DrawScreen composable with compact controls (dropdown, slider, color picker)
 * and a Save button in the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawScreen(
    navController: NavHostController,
    vm: DrawingViewModel = viewModel(factory = DrawingViewModelProvider.Factory),
    drawingId: Long = 0
) {
    if (drawingId.toInt() != 0) {
        vm.setSelectedDrawing(drawingId)
    }
    val ctx = LocalContext.current
    val strokes by vm.strokes.collectAsState()
    val selectedBrush by vm.selectedBrush.collectAsState()
    val selectedColor by vm.selectedColor.collectAsState()
    val penWidth by vm.penWidth.collectAsState()
    val toolType by vm.toolType.collectAsState()

    var canvasSize by remember { mutableStateOf(IntSize(0, 0)) }
    var shapeMenuExpanded by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PaintifyColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Canvas",
                        color = Color.White
                    )
                },
                colors = topAppBarColors(
                    containerColor = PaintifyColors.Surface,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showColorPicker = true }) {
                        Icon(
                            Icons.Default.ColorLens,
                            contentDescription = "Color",
                            tint = PaintifyColors.AccentSoft
                        )
                    }
                    IconButton(onClick = { shapeMenuExpanded = true }) {
                        Icon(
                            Icons.Default.Brush,
                            contentDescription = "Brush shape",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = shapeMenuExpanded,
                        onDismissRequest = { shapeMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Line") },
                            onClick = {
                                vm.setBrush(ShapeType.LINE)
                                shapeMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Circle") },
                            onClick = {
                                vm.setBrush(ShapeType.CIRCLE)
                                shapeMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rectangle") },
                            onClick = {
                                vm.setBrush(ShapeType.RECT)
                                shapeMenuExpanded = false
                            }
                        )
                    }

                    IconToggleButton(
                        checked = (toolType == ToolType.ERASER),
                        onCheckedChange = { isOn ->
                            if (isOn) vm.setEraser() else vm.setPen()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixOff,
                            contentDescription = if (toolType == ToolType.ERASER) "Eraser on" else "Eraser off",
                            tint = if (toolType == ToolType.ERASER)
                                PaintifyColors.Accent
                            else
                                Color.White
                        )
                    }

                    val context = LocalContext.current
                    var showSaveDialog by remember { mutableStateOf(false) }

                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save",
                            tint = PaintifyColors.AccentSoft
                        )
                    }

                    if (showSaveDialog) {
                        var tempName by remember { mutableStateOf("") }

                        AlertDialog(
                            onDismissRequest = { showSaveDialog = false },
                            title = {
                                Text(
                                    "Save Drawing",
                                    color = Color.White
                                )
                            },
                            text = {
                                Column {
                                    Text(
                                        "Enter a name for your drawing:",
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
//                                    TextField(
//                                        value = tempName,
//                                        onValueChange = { tempName = it },
//                                        singleLine = true,
//                                        placeholder = { Text("e.g. My masterpiece") },
//                                        colors = TextFieldDefaults.textFieldColors(
//                                            backgroundColor = PaintifyColors.Surface,
//                                            textColor = Color.White,
//                                            cursorColor = PaintifyColors.Accent,
//                                            focusedIndicatorColor = PaintifyColors.Accent,
//                                            unfocusedIndicatorColor = Color.Transparent,
//                                            focusedLabelColor = PaintifyColors.Accent,
//                                            unfocusedLabelColor = Color.LightGray
//                                        )
//                                    )
                                    TextField(
                                        value = tempName,
                                        onValueChange = { tempName = it },
                                        singleLine = true,
                                        placeholder = { Text("e.g. My masterpiece") },
                                        colors = TextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = PaintifyColors.Surface,
                                            unfocusedContainerColor = PaintifyColors.Surface,
                                            cursorColor = PaintifyColors.Accent,
                                            focusedIndicatorColor = PaintifyColors.Accent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        )
                                    )


                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val saveName = if (tempName.isBlank()) "Untitled" else tempName
                                    if (canvasSize.width > 0 && canvasSize.height > 0) {
                                        vm.saveCurrentMerged(
                                            name = saveName,
                                            widthPx = canvasSize.width,
                                            heightPx = canvasSize.height
                                        )

                                        Toast.makeText(
                                            context,
                                            "Saved as \"$saveName\"",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        navController.popBackStack("home", inclusive = false)
                                    }

                                    showSaveDialog = false
                                }) {
                                    Text("Save", color = PaintifyColors.AccentSoft)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSaveDialog = false }) {
                                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                                }
                            },
                            containerColor = PaintifyColors.SurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(64.dp),
                containerColor = PaintifyColors.Surface,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Pen width: ${penWidth.toInt()} px",
                            color = Color.White
                        )
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
                .background(PaintifyColors.Background)
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White) // keep bright canvas
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
            onPick = {
                vm.setColor(it)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

/**
 * DrawScreen that overlays strokes on an existing drawing background.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawScreenWithBackground(
    navController: NavHostController,
    drawingId: Long,
    vm: DrawingViewModel = viewModel(factory = DrawingViewModelProvider.Factory),
) {
    LaunchedEffect(drawingId) { vm.setSelectedDrawingId(drawingId) }
    val drawing by vm.selectedDrawing.collectAsState()

    val bgBitmap = remember(drawing?.filePath) {
        drawing?.filePath?.let { path ->
            val file = File(path)
            if (file.exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
        }
    }

    val strokes by vm.strokes.collectAsState()
    val selectedColor by vm.selectedColor.collectAsState()
    val penWidth by vm.penWidth.collectAsState()
    val toolType by vm.toolType.collectAsState()

    var canvasSize by remember { mutableStateOf(IntSize(0, 0)) }
    var shapeMenuExpanded by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var drawingName by remember { mutableStateOf(drawing?.name ?: "Edited $drawingId") }

    Scaffold(
        containerColor = PaintifyColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        drawing?.name ?: "Edit Drawing",
                        color = Color.White
                    )
                },
                colors = topAppBarColors(
                    containerColor = PaintifyColors.Surface,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showColorPicker = true }) {
                        Icon(
                            Icons.Default.ColorLens,
                            contentDescription = "Color",
                            tint = PaintifyColors.AccentSoft
                        )
                    }
                    IconButton(onClick = { shapeMenuExpanded = true }) {
                        Icon(
                            Icons.Default.Brush,
                            contentDescription = "Brush shape",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = shapeMenuExpanded,
                        onDismissRequest = { shapeMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Line") },
                            onClick = {
                                vm.setBrush(ShapeType.LINE)
                                shapeMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Circle") },
                            onClick = {
                                vm.setBrush(ShapeType.CIRCLE)
                                shapeMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rectangle") },
                            onClick = {
                                vm.setBrush(ShapeType.RECT)
                                shapeMenuExpanded = false
                            }
                        )
                    }

                    IconToggleButton(
                        checked = (toolType == ToolType.ERASER),
                        onCheckedChange = { on -> if (on) vm.setEraser() else vm.setPen() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixOff,
                            contentDescription = if (toolType == ToolType.ERASER) "Eraser on" else "Eraser off",
                            tint = if (toolType == ToolType.ERASER) PaintifyColors.Accent else Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            if (canvasSize.width > 0 && canvasSize.height > 0) {
                                vm.saveCurrentMerged(
                                    name = drawingName,
                                    widthPx = canvasSize.width,
                                    heightPx = canvasSize.height
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = PaintifyColors.AccentSoft)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(60.dp),
                containerColor = PaintifyColors.Surface,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Pen width: ${penWidth.toInt()} px",
                            color = Color.White
                        )
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
                .background(PaintifyColors.Background)
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
                bgBitmap?.let { img ->
                    Image(
                        bitmap = img,
                        contentDescription = "Background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }

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

@Composable
private fun ColorPreview(color: Color, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.size(56.dp),
        colors = cardColors(
            containerColor = color
        )
    ) {}
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "$label: ${value.toInt()}",
            color = Color.White
        )
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
fun ColorPickerDialogRGB(
    initial: Color,
    onDismiss: () -> Unit,
    onPick: (Color) -> Unit
) {
    var r by remember { mutableFloatStateOf(initial.red * 255f) }
    var g by remember { mutableFloatStateOf(initial.green * 255f) }
    var b by remember { mutableFloatStateOf(initial.blue * 255f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(Color(r / 255f, g / 255f, b / 255f)) }) {
                Text("Select", color = PaintifyColors.AccentSoft)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.7f)) }
        },
        title = {
            Text("Pick a Color (RGB)", color = Color.White)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColorPreview(color = initial)
                    Text("→", color = Color.White)
                    ColorPreview(color = Color(r / 255f, g / 255f, b / 255f))
                }

                ColorSlider("RED", r, { r = it }, 0f..255f)
                ColorSlider("GREEN", g, { g = it }, 0f..255f)
                ColorSlider("BLUE", b, { b = it }, 0f..255f)
            }
        },
        containerColor = PaintifyColors.SurfaceVariant
    )
}


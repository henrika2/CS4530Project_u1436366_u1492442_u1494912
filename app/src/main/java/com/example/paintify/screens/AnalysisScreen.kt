
/**
 * Paintify - Image Analysis Screen & ViewModel
 *
 * Provides the UI and logic for analyzing imported images with AI.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * This module defines the data models (DetectedLabel, DetectedObject),
 * the AnalysisUiState sealed interface, and the AnalysisViewModel used
 * to send an image to an AI service, parse the JSON response, and expose
 * detection results as state. The AnalysisScreen composable displays the
 * analyzed image with bounding boxes drawn on top and lists detected objects
 * and labels with confidence scores, including loading and error states.
 */
package com.example.paintify.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import coil.compose.rememberAsyncImagePainter
import com.example.paintify.BuildConfig
import com.example.paintify.ui.PaintifyColors
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class DetectedLabel(
    val name: String,
    val confidence: Float
)

data class DetectedObject(
    val name: String,
    val category: String?,
    val confidence: Float,
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float
)

interface AnalysisUiState {
    object Idle : AnalysisUiState
    object Loading : AnalysisUiState
    data class Success(
        val imageUri: Uri,
        val imageWidth: Int,
        val imageHeight: Int,
        val labels: List<DetectedLabel>,
        val objects: List<DetectedObject>
    ) : AnalysisUiState

    data class Error(val message: String) : AnalysisUiState
}

class AnalysisViewModel : ViewModel() {

    private val _state = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val state: StateFlow<AnalysisUiState> = _state.asStateFlow()

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun analyzeImage(context: Context, uri: Uri) {
        _state.value = AnalysisUiState.Loading

        viewModelScope.launch {
            try {
                val bytes =
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                val width = bmp.width
                val height = bmp.height

                val prompt = """
                    You are an object detection engine.
                    Look at the image and return ONLY valid JSON (no markdown, no explanation).
                    
                    Use this exact structure:
                    {
                      "labels": [
                        { "name": "string", "confidence": 0.0 }
                      ],
                      "objects": [
                        {
                          "name": "string",
                          "category": "string or null",
                          "confidence": 0.0,
                          "box": {
                            "xMin": 0.0,
                            "yMin": 0.0,
                            "xMax": 0.0,
                            "yMax": 0.0
                          }
                        }
                      ]
                    }
                    
                    Rules:
                    - confidence is between 0 and 1
                    - xMin, yMin, xMax, yMax are normalized coordinates in [0,1],
                      relative to image width and height.
                    - If something is unknown, use null.
                    - Output ONLY JSON.
                """.trimIndent()

                val inputContent = content {
                    image(bmp)
                    text(prompt)
                }

                val response = model.generateContent(inputContent)
                val raw = response.text ?: "{}"

                Log.d("GeminiDetection", raw)

                val cleanJson = sanitizeJson(raw)

                val (labels, objects) = parseDetectionJson(cleanJson)

                _state.value = AnalysisUiState.Success(
                    imageUri = uri,
                    imageWidth = width,
                    imageHeight = height,
                    labels = labels,
                    objects = objects
                )

            } catch (e: Exception) {
                Log.e(
                    "AnalysisViewModel",
                    e.message
                        ?: e.localizedMessage
                        ?: e.toString(),
                    e
                )
                _state.value = AnalysisUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun parseDetectionJson(json: String): Pair<List<DetectedLabel>, List<DetectedObject>> {
        val labels = mutableListOf<DetectedLabel>()
        val objects = mutableListOf<DetectedObject>()

        val root = JSONObject(json)

        val labelsArray: JSONArray = root.optJSONArray("labels") ?: JSONArray()
        for (i in 0 until labelsArray.length()) {
            val obj = labelsArray.optJSONObject(i) ?: continue
            val name = obj.optString("name", "")
            val confidence = obj.optDouble("confidence", 0.0).toFloat()
            if (name.isNotBlank()) {
                labels.add(DetectedLabel(name, confidence))
            }
        }

        val objsArray: JSONArray = root.optJSONArray("objects") ?: JSONArray()
        for (i in 0 until objsArray.length()) {
            val o = objsArray.optJSONObject(i) ?: continue
            val name = o.optString("name", "")
            if (name.isBlank()) continue

            val category = if (o.has("category") && !o.isNull("category")) {
                o.optString("category", null)
            } else null

            val confidence = o.optDouble("confidence", 0.0).toFloat()
            val box = o.optJSONObject("box") ?: JSONObject()

            val xMin = box.optDouble("xMin", 0.0).toFloat().coerceIn(0f, 1f)
            val yMin = box.optDouble("yMin", 0.0).toFloat().coerceIn(0f, 1f)
            val xMax = box.optDouble("xMax", 0.0).toFloat().coerceIn(0f, 1f)
            val yMax = box.optDouble("yMax", 0.0).toFloat().coerceIn(0f, 1f)

            objects.add(
                DetectedObject(
                    name = name,
                    category = category,
                    confidence = confidence,
                    xMin = xMin,
                    yMin = yMin,
                    xMax = xMax,
                    yMax = yMax
                )
            )
        }

        return labels to objects
    }

    private fun sanitizeJson(raw: String): String {
        var s = raw.trim()

        if (s.startsWith("```")) {
            val firstNewline = s.indexOf('\n')
            if (firstNewline != -1) {
                s = s.substring(firstNewline + 1)
            }
            if (s.startsWith("json")) {
                val secondNewline = s.indexOf('\n')
                if (secondNewline != -1) {
                    s = s.substring(secondNewline + 1)
                }
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length - 3)
            }
        }

        return s.trim()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    navController: NavHostController,
    imageUri: Uri
) {
    val context = LocalContext.current
    val vm: AnalysisViewModel = viewModel()
    val state by vm.state.collectAsState()

    // trigger analysis once when screen opens
    LaunchedEffect(imageUri) {
        vm.analyzeImage(context, imageUri)
    }

    Scaffold(
        containerColor = PaintifyColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Image Analysis",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
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
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PaintifyColors.Background)
                .padding(padding)
        ) {
            when (val s = state) {
                AnalysisUiState.Idle,
                AnalysisUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PaintifyColors.Accent)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Analyzing image…",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                is AnalysisUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = cardColors(
                                containerColor = PaintifyColors.Surface
                            ),
                            shape = RoundedCornerShape(20.dp),
                            elevation = cardElevation(6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Analysis failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = s.message,
                                    color = PaintifyColors.Error
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { vm.analyzeImage(context, imageUri) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PaintifyColors.Accent,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                is AnalysisUiState.Success -> {
                    AnalysisContent(
                        imageUri = s.imageUri,
                        imageWidth = s.imageWidth,
                        imageHeight = s.imageHeight,
                        labels = s.labels,
                        objects = s.objects
                    )
                }
            }
        }
    }
}

@Composable
internal fun AnalysisContent(
    imageUri: Uri,
    imageWidth: Int,
    imageHeight: Int,
    labels: List<DetectedLabel>,
    objects: List<DetectedObject>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaintifyColors.Background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Overview",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )

        // Image + overlay card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(
                    if (imageHeight == 0) 1f
                    else imageWidth.toFloat() / imageHeight.toFloat()
                ),
            shape = RoundedCornerShape(20.dp),
            colors = cardColors(
                containerColor = PaintifyColors.SurfaceVariant
            ),
            elevation = cardElevation(defaultElevation = 8.dp)
        ) {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Analyzed image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                Canvas(
                    modifier = Modifier.matchParentSize()
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    objects.forEach { obj ->
                        val left = obj.xMin * canvasWidth
                        val top = obj.yMin * canvasHeight
                        val right = obj.xMax * canvasWidth
                        val bottom = obj.yMax * canvasHeight

                        drawRect(
                            color = PaintifyColors.AccentSoft.copy(alpha = 0.9f),
                            topLeft = Offset(left, top),
                            size = androidx.compose.ui.geometry.Size(
                                width = (right - left).coerceAtLeast(1f),
                                height = (bottom - top).coerceAtLeast(1f)
                            ),
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }
        }

        // Detected objects
        Text(
            "Detected Objects",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        if (objects.isEmpty()) {
            Text(
                "No objects detected.",
                color = Color.White.copy(alpha = 0.6f)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                objects.forEach { obj ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = cardColors(
                            containerColor = PaintifyColors.Surface
                        ),
                        elevation = cardElevation(2.dp)
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                headlineColor = Color.White,
                                supportingColor = Color.White.copy(alpha = 0.7f)
                            ),
                            headlineContent = {
                                Text(obj.name)
                            },
                            supportingContent = {
                                val conf = (obj.confidence * 100).toInt()
                                val cat = obj.category ?: "Uncategorized"
                                Text("Confidence: $conf%  •  $cat")
                            }
                        )
                    }
                }
            }
        }

        // Labels
        Text(
            "Labels",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        if (labels.isEmpty()) {
            Text(
                "No labels detected.",
                color = Color.White.copy(alpha = 0.6f)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEach { label ->
                    val conf = (label.confidence * 100).toInt()
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = PaintifyColors.SurfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = PaintifyColors.Accent,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = label.name,
                                color = Color.White
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "$conf%",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}


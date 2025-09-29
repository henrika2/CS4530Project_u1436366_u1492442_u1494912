package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.paintify.ui.theme.Comp1Theme
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size


@Composable
fun DrawingCanvasPoints() {
    var strokes by remember { mutableStateOf(listOf<List<Offset>>()) }
    var currentStroke by remember { mutableStateOf(listOf<Offset>()) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray)
            //We capture touch input with
            // pointerInput and detectDragGestures.
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentStroke = listOf(offset)
                        //if you update current stroke live here not on DragEnd,
                        // then you do not need a second loop
                        strokes = strokes + listOf(currentStroke)
                    },
                    onDrag = { change, x ->
                        change.consume()
                        currentStroke = currentStroke + change.position
                        //if you update current stroke live here not on DragEnd,
                        // then you do not need a second loop
                        strokes = strokes.dropLast(1) + listOf(currentStroke)
                    },
                    onDragEnd = {
                        //strokes = strokes + listOf(currentStroke)
                        currentStroke = emptyList()
                    }
                )
            }
    ) {
        // Draw all completed strokes
        strokes.forEach { stroke ->
            for (i in 0 until stroke.size - 1) {
                drawLine(
                    color = Color.Black,
                    start = stroke[i],
                    end = stroke[i + 1],
                    strokeWidth = 8f
                )
            }
        }
    }
}

enum class BrushType {
    LINE, CIRCLE, RECTANGLE
}

@Composable
fun DrawingCanvas(brushType: BrushType = BrushType.CIRCLE) {
    var strokes by remember { mutableStateOf(listOf<List<Offset>>()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentStroke = listOf(offset)
                        strokes = strokes + listOf(currentStroke)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentStroke = currentStroke + change.position
                        strokes = strokes.dropLast(1) + listOf(currentStroke)
                    },
                    onDragEnd = { currentStroke = emptyList() }
                )
            }
    ) {
        strokes.forEach { stroke ->
            when (brushType) {
                BrushType.LINE -> {
                    for (i in 0 until stroke.size - 1) {
                        drawLine(Color.Black, stroke[i], stroke[i + 1], strokeWidth = 4f)
                    }
                }
                BrushType.CIRCLE -> {
                    stroke.forEach { point ->
                        drawCircle(Color.Red, radius = 15f, center = point)
                    }
                }
                BrushType.RECTANGLE -> {
                    stroke.forEach { point ->
                        drawRect(
                            Color.Black,
                            topLeft = Offset(point.x - 8f, point.y - 8f),
                            size = Size(16f, 16f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrawCirlce() {
    Column (Modifier.fillMaxWidth().padding(16.dp)) {
        Canvas(Modifier.size(100.dp)) {
            drawCircle(
                color = Color.Blue,
                radius = size.minDimension / 2
            )
        }
    }
}







package com.example.paintify.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

// Shared model types
enum class BrushType { LINE, CIRCLE, RECTANGLE }

data class Stroke(
    val points: List<Offset>,
    val brush: BrushType,
    val color: Color,
    val widthPx: Float = 4f // pass pen width
)

/** Stateless canvas: draws strokes and emits drag events to the ViewModel */
@Composable
fun DrawingCanvas(
    strokes: List<Stroke>,
    onStart: (Offset) -> Unit,
    onMove: (Offset) -> Unit,
    onEnd: () -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos -> onStart(pos) },
                    onDrag = { change, _ ->
                        // consume so others don't handle it
                        change.consume()
                        onMove(change.position)
                    },
                    onDragEnd = { onEnd() }
                )
            }
    ) {
        strokes.forEach { stroke ->
            when (stroke.brush) {
                BrushType.LINE -> {
                    val pts = stroke.points
                    for (i in 0 until pts.size - 1) {
                        drawLine(
                            color = stroke.color,
                            start = pts[i],
                            end = pts[i + 1],
                            strokeWidth = stroke.widthPx
                        )
                    }
                }
                BrushType.CIRCLE -> {
                    val radius = maxOf(4f, stroke.widthPx / 1.5f)
                    stroke.points.forEach { p ->
                        drawCircle(
                            color = stroke.color,
                            radius = radius,
                            center = p,
                        )
                    }
                }
                BrushType.RECTANGLE -> {
                    val half = maxOf(4f, stroke.widthPx / 2f) // CHANGE: optional
                    stroke.points.forEach { p ->
                        drawRect(
                            color = stroke.color,
                            topLeft = Offset(p.x - half, p.y - half),
                            size = Size(half, half)
                        )
                    }
                }
            }
        }
    }
}

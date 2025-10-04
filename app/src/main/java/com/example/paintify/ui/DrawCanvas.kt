/**
 * Paintify - Drawing Canvas
 * -------------------------
 * This file defines the `DrawingCanvas` composable and the `Stroke` data class
 * used for rendering user drawings on the canvas surface.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * This module provides a stateless drawing component that receives
 * drawing strokes and gesture callbacks from the ViewModel. The canvas
 * visually renders shapes (Line, Circle, or Rectangle) in real time
 * based on user drag gestures, without storing state internally.
 */

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
import androidx.compose.ui.platform.testTag
import com.example.paintify.models.ShapeType
import com.example.paintify.models.Stroke


/**
 * DrawingCanvas composable.
 *
 * A stateless drawing surface that:
 *  - Renders a list of strokes visually.
 *  - Emits drag gestures (start, move, end) back to the parent (usually a ViewModel).
 *
 * It does not store or mutate any state; all drawing data comes
 * from the ViewModel’s reactive flows.
 *
 * @param strokes The list of strokes to render.
 * @param onStart Callback invoked when the user starts dragging.
 * @param onMove Callback invoked continuously as the drag moves.
 * @param onEnd Callback invoked when the drag ends.
 * @param modifier Optional modifier for layout customization.
 */
@Composable
fun DrawingCanvas(
    strokes: List<Stroke>,
    onStart: (Offset) -> Unit,
    onMove: (Offset) -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos -> onStart(pos) },
                    onDrag = { change, _ ->
                        // Consume gesture to prevent propagation to other components
                        change.consume()
                        onMove(change.position)
                    },
                    onDragEnd = { onEnd() }
                )
            }
            .testTag("drawingCanvas")
    ) {
        // Render each stroke according to its brush type
        strokes.forEach { stroke ->
            when (stroke.brush) {
                ShapeType.LINE -> {
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

                ShapeType.CIRCLE -> {
                    val radius = maxOf(4f, stroke.widthPx / 1.5f)
                    stroke.points.forEach { p ->
                        drawCircle(
                            color = stroke.color,
                            radius = radius,
                            center = p
                        )
                    }
                }

                ShapeType.RECT -> {
                    val half = maxOf(4f, stroke.widthPx / 2f)
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

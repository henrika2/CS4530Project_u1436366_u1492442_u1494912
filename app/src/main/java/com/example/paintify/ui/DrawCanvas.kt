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

/**
 * @param strokes The list of strokes to render.
 * @param onStart Callback invoked when the user starts dragging.
 * @param onMove Callback invoked continuously as the drag moves.
 * @param onEnd Callback invoked when the drag ends.
 * @param modifier Optional modifier for layout customization.
 */
package com.example.paintify.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.paintify.models.ShapeType
import com.example.paintify.models.Stroke


/**
 * Stateless drawing surface that renders strokes and emits drag gestures.
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
                        change.consume()
                        onMove(change.position)
                    },
                    onDragEnd = { onEnd() }
                )
            }
            .testTag("drawingCanvas")
    ) {
        // Render each stroke according to its brush type
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
                    // Match BitmapRenderer logic: first point = center, last = edge
                    if (stroke.points.size >= 2) {
                        val c = stroke.points.first()
                        val e = stroke.points.last()
                        val r = kotlin.math.hypot((e.x - c.x).toDouble(), (e.y - c.y).toDouble())
                            .toFloat()
                        drawCircle(
                            color = stroke.color,
                            radius = r,
                            center = c,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke.widthPx)
                        )
                    }
                }

                ShapeType.RECT -> {
                    // Match BitmapRenderer logic: first = TL, last = BR (any drag)
                    if (stroke.points.size >= 2) {
                        val p0 = stroke.points.first()
                        val p1 = stroke.points.last()
                        val left = minOf(p0.x, p1.x)
                        val top = minOf(p0.y, p1.y)
                        val right = maxOf(p0.x, p1.x)
                        val bottom = maxOf(p0.y, p1.y)
                        drawRect(
                            color = stroke.color,
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke.widthPx)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Paintify - Canvas Stroke Unit Tests
 * -----------------------------------
 * Validates core behavior of the CanvasStroke model including property
 * assignment, copying, and immutability of stroke data.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * Ensures that CanvasStroke objects correctly store provided parameters
 * and that the `copy()` operation produces independent instances without
 * mutating the original stroke.
 */

package com.example.paintify

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import com.example.paintify.models.ShapeType
import com.example.paintify.models.CanvasStroke
import com.example.paintify.models.PaintParams

class CanvasStrokeSimpleTest {

    @Test
    fun createStroke_storesFields() {
        val params = PaintParams(color = Color.Black, widthPx = 4f, isEraser = false)
        val points = listOf(Offset(0f, 0f), Offset(10f, 10f))

        val s = CanvasStroke(
            points = points,
            paint = params,
            shapeType = ShapeType.LINE,
        )
        assertEquals(ShapeType.LINE, s.shapeType)
        assertEquals(points, s.points)
        assertEquals(params, s.paint)
    }

    @Test
    fun copy_canChangePointsWithoutMutatingOriginal() {
        val base = CanvasStroke(
            shapeType = ShapeType.RECT,
            points = listOf(Offset(0f, 0f), Offset(4f, 4f)),
            paint = PaintParams(Color.Green, 9f, isEraser = false)
        )

        val moved = base.copy(points = base.points + Offset(8f, 8f))

        assertEquals(2, base.points.size)
        assertEquals(3, moved.points.size)
        assertNotEquals(base, moved)
    }
}

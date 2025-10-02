package com.example.paintify


import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.paintify.models.ShapeType
import com.example.paintify.models.ToolType
import com.example.paintify.models.CanvasStroke
import com.example.paintify.models.PaintParams
import androidx.compose.ui.graphics.Color

import org.junit.Test
import org.junit.runner.RunWith



import org.junit.Assert.*

class PaintParamsTest {

    @Test
    fun constructStoresFields() {
        val p = PaintParams(color = Color.Red, widthPx = 8f, isEraser = false)
        assertEquals(Color.Red, p.color)
        assertEquals(8f, p.widthPx, 0.0001f)
        assertFalse(p.isEraser)
    }

    @Test
    fun equalsAndHashCodeDependOnAllFields() {
        val a = PaintParams(Color.Black, 12f)
        val b = PaintParams(Color.Black, 12f)
        val c = PaintParams(Color.Black, 10f)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
    }

    @Test
    fun copyCanChangeIndividualFields() {
        val base = PaintParams(Color.Blue, 6f)
        val wider = base.copy(widthPx = 14f)
        val eraser = base.copy(isEraser = true)
        assertEquals(14f, wider.widthPx, 0.0f)
        assertTrue(eraser.isEraser)
        // original unchanged
        assertEquals(6f, base.widthPx,0.0f)
        assertFalse(base.isEraser)
    }
}

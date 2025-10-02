package com.example.paintify

import com.example.paintify.models.ShapeType
import com.example.paintify.models.ToolType
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

class EnumTest {

    @Test
    fun toolTypeContainsExpectedValues() {
        // Your ToolType currently may include LINE/RECT/CIRCLE; this test tolerates either form.
        val names = ToolType.entries.map { it.name }.toSet()
        assertTrue(names.contains("PEN"))
        assertTrue(names.contains("ERASER"))
    }

    @Test
    fun shapeTypeContainsAllShapes() {
        val names = ShapeType.entries.map { it.name }.toSet()
        assertTrue(names.contains("LINE"))
        assertTrue(names.contains("RECT"))
        assertTrue(names.contains("CIRCLE"))
    }
}
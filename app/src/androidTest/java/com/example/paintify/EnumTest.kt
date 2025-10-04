/**
 * Paintify - Enum Unit Tests
 * --------------------------
 * Verifies that all expected enum values are defined within the
 * ToolType and ShapeType enumerations.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * Ensures that the fundamental enums used throughout the Paintify app
 * contain all required constants for drawing tools and brush shapes.
 */

package com.example.paintify

import com.example.paintify.models.ShapeType
import com.example.paintify.models.ToolType
import org.junit.Test
import org.junit.Assert.*

class EnumTest {

    @Test
    fun toolTypeContainsExpectedValues() {
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

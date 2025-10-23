///**
// * Paintify - Paint Parameters Model
// * ---------------------------------
// * Defines the parameters that control how a stroke is rendered,
// * including color, width, and tool behavior (pen or eraser).
// *
// * Group Members:
// *  - Dustin
// *  - Nolan
// *  - Ian
// *
// * Description:
// * The `PaintParams` data class encapsulates the visual and behavioral
// * attributes of a brush used for drawing on the canvas. Each stroke
// * references a `PaintParams` instance to determine how it should appear.
// */
//
//package com.example.paintify.models
//
//import androidx.compose.ui.graphics.Color
//
///**
// * Represents the visual configuration used to draw a stroke.
// *
// * @property color The color of the paint applied to the stroke.
// * @property widthPx The stroke width in pixels.
// * @property isEraser Whether this paint acts as an eraser instead of a pen.
// */
//data class PaintParams(
//    val color: Color,
//    val widthPx: Float,
//    val isEraser: Boolean = false
//)
package com.example.paintify.models

import androidx.compose.ui.graphics.Color

/**
 * Represents the visual configuration used to draw a stroke.
 *
 * @property color The color of the paint applied to the stroke.
 * @property widthPx The stroke width in pixels.
 * @property isEraser Whether this paint acts as an eraser instead of a pen.
 */
data class PaintParams(
    val color: Color,
    val widthPx: Float,
    val isEraser: Boolean = false
)

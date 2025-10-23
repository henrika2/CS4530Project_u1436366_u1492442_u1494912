///**
// * Paintify - Stroke Model
// * -----------------------
// * Defines the data structure for representing individual drawing strokes.
// *
// * Group Members:
// *  - Dustin
// *  - Nolan
// *  - Ian
// *
// * Description:
// * The `Stroke` data class models a single drawn shape on the canvas. It contains
// * information about the stroke’s geometry, appearance, and brush type. Each stroke
// * is a collection of points drawn continuously using the same tool parameters.
// */
//
//package com.example.paintify.models
//
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//
///**
// * Represents a single user stroke (a shape drawn on the canvas).
// *
// * @property points The ordered list of [Offset] coordinates that define the stroke path.
// * @property brush The shape type used for rendering (e.g., line, circle, or rectangle).
// * @property color The color applied to the stroke.
// * @property widthPx The width of the stroke in pixels; default is 4f.
// */
//data class Stroke(
//    val points: List<Offset>,
//    val brush: ShapeType,
//    val color: Color,
//    val widthPx: Float = 4f
//)

package com.example.paintify.models

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Represents a single user stroke (a shape drawn on the canvas).
 *
 * @property points The ordered list of [Offset] coordinates that define the stroke path.
 * @property brush The shape type used for rendering (e.g., line, circle, or rectangle).
 * @property color The color applied to the stroke.
 * @property widthPx The width of the stroke in pixels; default is 4f.
 */
data class Stroke(
    val points: List<Offset>,
    val brush: ShapeType,
    val color: Color,
    val widthPx: Float = 4f
)


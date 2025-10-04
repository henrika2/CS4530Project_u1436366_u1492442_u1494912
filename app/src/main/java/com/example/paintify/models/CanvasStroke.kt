/**
 * Paintify - Canvas Stroke Model
 * ------------------------------
 * Defines the data structure for strokes stored in the ViewModel layer.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * The `CanvasStroke` data class represents a persistent domain-level stroke.
 * It is used internally by the `DrawingViewModel` to manage drawing state
 * independently of the UI layer.
 *
 * Unlike the lightweight UI `Stroke` model, `CanvasStroke` maintains more
 * context about the shape type and drawing parameters, allowing Paintify
 * to restore, modify, and render drawings consistently across recompositions
 * or configuration changes (such as screen rotations).
 */

package com.example.paintify.models

import androidx.compose.ui.geometry.Offset

/**
 * Represents a single domain-level stroke stored in the ViewModel.
 *
 * @property points The ordered list of points representing the stroke path.
 * @property paint The paint configuration defining color, width, and tool type.
 * @property shapeType The type of shape being drawn (null for freehand pen/eraser strokes).
 */
data class CanvasStroke(
    // Optional stroke ID can be added for persistence or database tracking
    // val id: Long,
    val points: List<Offset>,
    val paint: PaintParams,
    val shapeType: ShapeType? = null
)

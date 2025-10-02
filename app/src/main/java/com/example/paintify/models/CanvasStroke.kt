package com.example.paintify.models

import androidx.compose.ui.geometry.Offset

/**
 * Domain stroke: ViewModel owns this, persists across rotations.
 * If shapeType == null -> treat as freehand (PEN/ERASER).
 * If shapeType != null -> points[0] = anchor, points[1] = current end.
 */
data class CanvasStroke(
    val id: Long,
    val points: List<Offset>,
    val paint: PaintParams,
    val shapeType: ShapeType? = null
)

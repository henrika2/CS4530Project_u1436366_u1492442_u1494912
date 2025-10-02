package com.example.paintify.models

import android.graphics.Point
import androidx.compose.foundation.layout.FlowLayoutOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset

enum class ToolType{PEN, LINE, RECT, CIRCLE, ERASER}

enum class Shape{LineShape, RectShape, CircleShape}

data class PaintParams(
    val strokeColor: Color,
    val strokeWidthPixel: Float,
    val isEraser: Boolean
)

data class Stroke(
    val id: Long,
    val shape: Shape,
    val points: List<Offset>,
    val paintParams: PaintParams
)
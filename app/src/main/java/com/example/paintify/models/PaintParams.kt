package com.example.paintify.models

import androidx.compose.ui.graphics.Color

//enum class PaintStyle { Stroke, Fill }

data class PaintParams(
    val color: Color,
    val widthPx: Float,
//    val style: PaintStyle = PaintStyle.Stroke,
    val isEraser: Boolean = false
)

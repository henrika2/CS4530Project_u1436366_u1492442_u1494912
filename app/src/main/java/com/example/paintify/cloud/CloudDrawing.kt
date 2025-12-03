package com.example.paintify.cloud

data class CloudDrawing(
    val id: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val title: String = ""
)

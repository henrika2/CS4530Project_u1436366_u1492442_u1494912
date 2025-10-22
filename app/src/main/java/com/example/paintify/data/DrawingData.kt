package com.example.paintify.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drawings")
data class DrawingData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val filePath: String,
    val widthPx: Int,
    val heightPx: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
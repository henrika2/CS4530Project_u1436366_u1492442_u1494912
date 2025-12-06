package com.example.paintify.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single saved drawing on disk.
 *
 * Each row in the `drawings` table corresponds to one PNG file
 * plus some basic metadata used by the UI (name, size, timestamps).
 */
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

package com.example.paintify.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DrawingData::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun drawingDao(): DrawingDao
}

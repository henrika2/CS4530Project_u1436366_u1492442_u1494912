package com.example.paintify.data
/**
 * Paintify - App Database
 * -----------------------
 * Room database configuration for Paintify’s persistent storage.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * The `AppDatabase` class declares the Room database that stores
 * `DrawingData` entities and exposes the corresponding `DrawingDao`.
 * It is instantiated once in `DrawApplication` and used by the
 * `DrawingRepository` to manage all drawing metadata and file paths
 * consistently across the app.
 */
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DrawingData::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun drawingDao(): DrawingDao
}

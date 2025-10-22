// DrawingDao.kt
package com.example.paintify.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawingDao {
    @Insert
    suspend fun insert(entity: DrawingData): Long

    @Update
    suspend fun update(entity: DrawingData)

    @Delete
    suspend fun delete(entity: DrawingData)

    @Query("SELECT * FROM drawings ORDER BY createdAt DESC")
    fun allDrawings(): Flow<List<DrawingData>>

    @Query("SELECT * FROM drawings WHERE id = :id")
    fun selectDrawingById(id: Long): Flow<DrawingData?>
}
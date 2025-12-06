package com.example.paintify.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
/**
 * Data access object (DAO) for the `drawings` table.
 *
 * This interface defines all database operations related to [DrawingData].
 * Room generates the implementation at compile time.
 */
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

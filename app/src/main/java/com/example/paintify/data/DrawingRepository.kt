//// DrawingRepository.kt
//package com.example.paintify.data
//
//import android.content.Context
//import android.util.Log
//import com.example.paintify.models.Stroke
//import com.example.paintify.models.BitmapRenderer
//import com.example.paintify.models.FileStore
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//
//class DrawingRepository(
//    private val appContext: Context,
//    val scope: CoroutineScope,
//    private val dao: DrawingDao,
//) {
//    /** Live list of all drawings (Flow), mirrors your CourseRepository style. */
//    val allDrawings: Flow<List<DrawingData>> = dao.allDrawings()
//
//    /** Selected drawing id (null = none). */
//    private val selectedId = MutableStateFlow<Long?>(null)
//
//    /** Currently selected drawing (Flow<DrawingEntity?>). */
//    @OptIn(ExperimentalCoroutinesApi::class)
//    val selectedDrawing: Flow<DrawingData?> =
//        selectedId.flatMapLatest { id ->
//            if (id == null) flowOf(null) else dao.selectDrawingById(id)
//        }
//
//    fun setSelectedDrawingId(id: Long) { selectedId.value = id }
//    fun clearSelectedDrawing() { selectedId.value = null }
//
//    /**
//     * Render current strokes -> Bitmap, save as PNG, insert metadata row.
//     * Fire-and-forget (like your addCourse/updateCourse/deleteCourse).
//     */
//    fun saveDrawing(
//        name: String,
//        strokes: List<Stroke>,
//        canvasWidthPx: Int,
//        canvasHeightPx: Int
//    ) {
//        if (strokes.isEmpty() || canvasWidthPx <= 0 || canvasHeightPx <= 0) return
//
//        scope.launch {
//            try {
////                val bitmap = BitmapRenderer.render(
////                    strokes = strokes,
////                    widthPx = canvasWidthPx,
////                    heightPx = canvasHeightPx
////                )
//
//                val renderer = BitmapRenderer()
//                val bitmap = renderer.render(
//                    strokes = strokes,
//                    widthPx = canvasWidthPx,
//                    heightPx = canvasHeightPx
//                )
//                val fileStore = FileStore()
//                val file = fileStore.saveBitmapPng(appContext, bitmap, name)
//
//                val entity = DrawingData(
//                    name = name.ifBlank { "Untitled" },
//                    filePath = file.absolutePath,
//                    widthPx = canvasWidthPx,
//                    heightPx = canvasHeightPx
//                )
//                dao.insert(entity)
//                Log.d("DrawingRepo", "Saved drawing: ${file.absolutePath}")
//            } catch (t: Throwable) {
//                Log.e("DrawingRepo", "Failed to save drawing", t)
//            }
//        }
//    }
//
//    /** Update only metadata (e.g., rename). */
//    fun updateDrawing(entity: DrawingData) {
//        scope.launch {
//            try { dao.update(entity) } catch (t: Throwable) {
//                Log.e("DrawingRepo", "Update failed", t)
//            }
//        }
//    }
//
//    /** Delete DB row (optionally also delete the file—see note below). */
//    fun deleteDrawing(entity: DrawingData, deleteFileOnDisk: Boolean = false) {
//        scope.launch {
//            try {
//                dao.delete(entity)
//                if (deleteFileOnDisk) {
//                    runCatching { java.io.File(entity.filePath).delete() }
//                }
//            } catch (t: Throwable) {
//                Log.e("DrawingRepo", "Delete failed", t)
//            }
//        }
//    }
//}

package com.example.paintify.data

import android.content.Context
import android.util.Log
import com.example.paintify.models.Stroke
import com.example.paintify.models.BitmapRenderer
import com.example.paintify.models.FileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DrawingRepository private constructor(
    private val appContext: Context,
    private val appScope: CoroutineScope,
    private val dao: DrawingDao
) {
    /** Live list of all drawings. */
    val allDrawings: Flow<List<DrawingData>> = dao.allDrawings()

    /** Selected drawing id (null = none). */
    private val selectedDrawingId = MutableStateFlow<Long?>(null)

    /** Currently selected drawing. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDrawing: Flow<DrawingData?> =
        selectedDrawingId.flatMapLatest { id ->
            if (id == null) flowOf(null) else dao.selectDrawingById(id)
        }

    fun setSelectedDrawingId(id: Long?) { selectedDrawingId.value = id }

    /**
     * Render strokes → Bitmap, save PNG to app dir, then insert DB row.
     */
    fun saveDrawing(
        displayName: String,
        strokes: List<Stroke>,
        canvasWidthPx: Int,
        canvasHeightPx: Int
    ) {
        if (strokes.isEmpty() || canvasWidthPx <= 0 || canvasHeightPx <= 0) return

        appScope.launch {
            try {
                val bitmap = BitmapRenderer().render(
                    strokes = strokes,
                    widthPx = canvasWidthPx,
                    heightPx = canvasHeightPx
                )
                val file = FileStore().saveBitmapPng(appContext, bitmap, displayName)
                val now = System.currentTimeMillis()

                val row = DrawingData(
                    name = displayName.ifBlank { "Untitled" },
                    filePath = file.absolutePath,
                    widthPx = canvasWidthPx,
                    heightPx = canvasHeightPx,
                    createdAt = now,
                    updatedAt = now
                )
                dao.insert(row)
                Log.d("DrawingRepository", "Saved drawing at: ${file.absolutePath}")
            } catch (t: Throwable) {
                Log.e("DrawingRepository", "saveDrawing failed", t)
            }
        }
    }

    /** Update metadata (e.g., rename). */
    fun updateDrawingMetadata(updated: DrawingData) {
        appScope.launch {
            try { dao.update(updated.copy(updatedAt = System.currentTimeMillis())) }
            catch (t: Throwable) { Log.e("DrawingRepository", "update failed", t) }
        }
    }

    /** Delete DB row (optionally delete file on disk). */
    fun deleteDrawing(target: DrawingData, alsoDeleteImageFile: Boolean = false) {
        appScope.launch {
            try {
                dao.delete(target)
                if (alsoDeleteImageFile) runCatching { java.io.File(target.filePath).delete() }
            } catch (t: Throwable) {
                Log.e("DrawingRepository", "delete failed", t)
            }
        }
    }

    // ---------------- Singleton ----------------
    companion object {
        @Volatile private var INSTANCE: DrawingRepository? = null

        fun getInstance(
            context: Context,
            scope: CoroutineScope,
            dao: DrawingDao
        ): DrawingRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: DrawingRepository(context.applicationContext, scope, dao).also {
                    INSTANCE = it
                }
            }
    }
}


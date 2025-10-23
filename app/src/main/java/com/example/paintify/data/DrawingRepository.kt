package com.example.paintify.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.paintify.models.Stroke
import com.example.paintify.models.BitmapRenderer
import com.example.paintify.models.FileStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DrawingRepository(
    private val appContext: Context,
    val scope: CoroutineScope,
    private val dao: DrawingDao,
) {
    /** Live list of all drawings (Flow), mirrors your CourseRepository style. */
    val allDrawings: Flow<List<DrawingData>> = dao.allDrawings()

    /** Selected drawing id (null = none). */
    private val selectedId = MutableStateFlow<Long?>(null)

    /** Currently selected drawing (Flow<DrawingEntity?>). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDrawing: Flow<DrawingData?> =
        selectedId.flatMapLatest { id ->
            if (id == null) flowOf(null) else dao.selectDrawingById(id)
        }

    fun setSelectedDrawingId(id: Long) { selectedId.value = id }
    fun clearSelectedDrawing() { selectedId.value = null }

    /**
     * Render current strokes -> Bitmap, save as PNG, insert metadata row.
     * Fire-and-forget (like your addCourse/updateCourse/deleteCourse).
     */
    fun saveDrawing(
        name: String,
        strokes: List<Stroke>,
        canvasWidthPx: Int,
        canvasHeightPx: Int
    ) {
        if (strokes.isEmpty() || canvasWidthPx <= 0 || canvasHeightPx <= 0) return

        scope.launch {
            try {
                val renderer = BitmapRenderer()
                val bitmap = renderer.render(
                    strokes = strokes,
                    widthPx = canvasWidthPx,
                    heightPx = canvasHeightPx
                )
                val fileStore = FileStore()
                val file = fileStore.saveBitmapPng(appContext, bitmap, name)

                val entity = DrawingData(
                    name = name.ifBlank { "Untitled" },
                    filePath = file.absolutePath,
                    widthPx = canvasWidthPx,
                    heightPx = canvasHeightPx
                )
                dao.insert(entity)
                Log.d("DrawingRepo", "Saved drawing: ${file.absolutePath}")
            } catch (t: Throwable) {
                Log.e("DrawingRepo", "Failed to save drawing", t)
            }
        }
    }

    /** Update only metadata (e.g., rename). */
    fun updateDrawing(entity: DrawingData) {
        scope.launch {
            try { dao.update(entity) } catch (t: Throwable) {
                Log.e("DrawingRepo", "Update failed", t)
            }
        }
    }

    /** Delete DB row (optionally also delete the file—see note below). */
    fun deleteDrawing(entity: DrawingData, deleteFileOnDisk: Boolean = false) {
        scope.launch {
            try {
                dao.delete(entity)
                if (deleteFileOnDisk) {
                    runCatching { java.io.File(entity.filePath).delete() }
                }
            } catch (t: Throwable) {
                Log.e("DrawingRepo", "Delete failed", t)
            }
        }
    }

    /** Import an image from the gallery (Photo Picker Uri) into app storage + DB */
    fun importImageFromGallery(uri: Uri) {
        scope.launch {
            try {
                val fileStore = FileStore()
                val file = fileStore.copyFromUri(appContext, uri)

                // Probe size
                val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)

                val entity = DrawingData(
                    name = file.nameWithoutExtension,
                    filePath = file.absolutePath,
                    widthPx = if (opts.outWidth > 0) opts.outWidth else 0,
                    heightPx = if (opts.outHeight > 0) opts.outHeight else 0
                )
                dao.insert(entity)
                Log.d("DrawingRepo", "Imported image: ${file.absolutePath}")
            } catch (t: Throwable) {
                Log.e("DrawingRepo", "Import failed", t)
            }
        }
    }
}

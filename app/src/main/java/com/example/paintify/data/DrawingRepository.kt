package com.example.paintify.data
/**
 * Paintify - Drawing Repository
 * -----------------------------
 * Repository layer that coordinates persistence of drawings and images.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * The `DrawingRepository` mediates access to the Room database and local
 * file storage for all drawing-related operations. It exposes flows of
 * saved drawings, tracks the currently selected drawing, and provides
 * methods to save new drawings, save merged drawings over backgrounds,
 * update metadata, delete entries, and import images from the gallery.
 * All work is performed in a coroutine `scope` to keep the UI responsive.
 */
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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.paintify.cloud.CloudSync



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



                // 🔥 NEW: upload to cloud if user is logged in
                val user = Firebase.auth.currentUser
                if (user != null) {
                    runCatching {
                        CloudSync.uploadDrawingFileAndSaveMetadata(
                            userId = user.uid,
                            title = entity.name,
                            filePath = entity.filePath
                        )
                    }.onFailure {
                        Log.e("DrawingRepo", "Cloud upload failed", it)
                    }
                }
            } catch (t: Throwable) {
                Log.e("DrawingRepo", "Failed to save drawing", t)
            }
        }
    }
    fun saveDrawingMerged(
        name: String,
        backgroundFilePath: String,
        strokes: List<Stroke>,
        canvasWidthPx: Int,
        canvasHeightPx: Int
    ) {
        if (canvasWidthPx <= 0 || canvasHeightPx <= 0) return

        scope.launch {
            try {
                // 1) Final output bitmap (this is what we will save)
                val outBitmap = Bitmap.createBitmap(
                    canvasWidthPx, canvasHeightPx, Bitmap.Config.ARGB_8888
                )
                val outCanvas = Canvas(outBitmap)

                // 2) Draw the background scaled to the canvas bounds
                val bg = BitmapFactory.decodeFile(backgroundFilePath)
                if (bg != null) {
                    val src = Rect(0, 0, bg.width, bg.height)
                    val dst = Rect(0, 0, canvasWidthPx, canvasHeightPx)
                    outCanvas.drawBitmap(bg, src, dst, null)
                } else {
                    Log.w("DrawingRepo", "Background not found at $backgroundFilePath; saving strokes only.")
                }

                // 3) Render strokes onto a transparent bitmap, then draw over background
                if (strokes.isNotEmpty()) {
                    val renderer = BitmapRenderer()
                    // IMPORTANT: Transparent background so don’t cover the bg image
                    val strokesBitmap = renderer.render(
                        strokes = strokes,
                        widthPx = canvasWidthPx,
                        heightPx = canvasHeightPx,
                        background = Color.Transparent
                    )
                    outCanvas.drawBitmap(strokesBitmap, 0f, 0f, null)
                }

                // 4) Save merged bitmap as NEW file + insert new DB row
                val fileStore = FileStore()
                val file = fileStore.saveBitmapPng(
                    appContext,
                    outBitmap,
                    name.ifBlank { "Edited" }
                )

                val entity = DrawingData(
                    name = name.ifBlank { "Edited" },
                    filePath = file.absolutePath,
                    widthPx = canvasWidthPx,
                    heightPx = canvasHeightPx
                )
                dao.insert(entity)
                Log.d("DrawingRepo", "Saved merged drawing: ${file.absolutePath}")

                // NEW: upload merged drawing to cloud if user logged in
                val user = Firebase.auth.currentUser
                if (user != null) {
                    runCatching {
                        CloudSync.uploadDrawingFileAndSaveMetadata(
                            userId = user.uid,
                            title = entity.name,
                            filePath = entity.filePath
                        )
                    }.onFailure {
                        Log.e("DrawingRepo", "Cloud upload failed", it)
                    }
                }
            } catch (t: Throwable) {
                Log.e("DrawingRepo", "Failed to save merged drawing", t)
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

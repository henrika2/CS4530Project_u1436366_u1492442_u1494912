package com.example.paintify.models
/**
 * Paintify - File Storage Utility
 * -------------------------------
 * Handles saving and importing image files used by the drawing features.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * The `FileStore` class provides helper functions to persist bitmaps as
 * PNG files under the app’s external files directory and to copy images
 * selected from the system gallery into app-managed storage. These file
 * paths are stored alongside drawing metadata so that Paintify can later
 * reload, display, share, or further edit saved drawings.
 */
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

class FileStore {
    fun saveBitmapPng(context: Context, bitmap: Bitmap, baseName: String): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?.resolve("paintify")
            ?: File(context.filesDir, "paintify_pics")

        if (!dir.exists()) dir.mkdirs()

        val safe = baseName.replace(Regex("""[^\w\-\.\s]"""), "_")
        val file = File(dir, "${System.currentTimeMillis()}_$safe.png")

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    /** Copy any image Uri from gallery into app storage and return the new File. */
    fun copyFromUri(context: Context, source: Uri): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?.resolve("paintify")
            ?: File(context.filesDir, "paintify_pics")
        if (!dir.exists()) dir.mkdirs()

        val target = File(dir, "${System.currentTimeMillis()}_import.png")
        context.contentResolver.openInputStream(source).use { input ->
            FileOutputStream(target).use { output ->
                if (input != null) input.copyTo(output)
            }
        }
        return target
    }
}

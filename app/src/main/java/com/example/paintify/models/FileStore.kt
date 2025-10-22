package com.example.paintify.models// FileStore.kt
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

class  FileStore {
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
}

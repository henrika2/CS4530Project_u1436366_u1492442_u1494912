package com.example.paintify.models
/**
 * Paintify - Bitmap Renderer
 * --------------------------
 * Renders a list of strokes into a bitmap for saving or compositing.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * The `BitmapRenderer` class converts the logical stroke model into a
 * `Bitmap` by drawing lines, circles, and rectangles onto an Android
 * Canvas. It respects stroke color, width, and shape type, and can draw
 * on either an opaque or transparent background, enabling both standalone
 * drawing exports and layered compositing over imported images.
 */
import android.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.hypot

class BitmapRenderer {
    fun render(
        strokes: List<Stroke>,
        widthPx: Int,
        heightPx: Int,
        background: Color = Color.White
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(background.toArgb())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        strokes.forEach { s ->
            paint.color = s.color.toArgb()
            paint.strokeWidth = s.widthPx

            when (s.brush) {
                ShapeType.LINE -> {
                    for (i in 1 until s.points.size) {
                        val p0 = s.points[i - 1]
                        val p1 = s.points[i]
                        canvas.drawLine(p0.x, p0.y, p1.x, p1.y, paint)
                    }
                }
                ShapeType.CIRCLE -> {
                    if (s.points.size >= 2) {
                        val c = s.points.first()
                        val e = s.points.last()
                        val r = hypot((e.x - c.x).toDouble(), (e.y - c.y).toDouble()).toFloat()
                        canvas.drawCircle(c.x, c.y, r, paint)
                    }
                }
                ShapeType.RECT -> {
                    if (s.points.size >= 2) {
                        val p0 = s.points.first()
                        val p1 = s.points.last()
                        val left = minOf(p0.x, p1.x)
                        val top = minOf(p0.y, p1.y)
                        val right = maxOf(p0.x, p1.x)
                        val bottom = maxOf(p0.y, p1.y)
                        val rect = RectF(left, top, right, bottom)
                        canvas.drawRect(rect, paint)
                    }
                }
                else -> {
                    // no-op for future
                }
            }
        }
        return bitmap
    }
}

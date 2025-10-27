/**
 * Paintify - Shape Type Model
 * ---------------------------
 * Enumerates the available brush/shape types for rendering strokes.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * The `ShapeType` enum is used by the drawing pipeline to choose how each
 * stroke is rendered on the canvas (as a continuous line, stamped circles,
 * or stamped rectangles).
 */

package com.example.paintify.models

/**
 * Shape types.
 */
enum class ShapeType {
    LINE,
    RECT,
    CIRCLE
}

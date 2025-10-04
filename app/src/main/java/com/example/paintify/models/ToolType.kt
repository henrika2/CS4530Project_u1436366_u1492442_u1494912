/**
 * Paintify - Tool Type Model
 * --------------------------
 * Defines the available tool types for drawing interactions.
 *
 * Group Members:
 *  - Dustin
 *  - Nolan
 *  - Ian
 *
 * Description:
 * The ToolType enum distinguishes between normal drawing (PEN) and erasing (ERASER).
 * ViewModels and UI components use this to determine behavior like color application
 * and composable rendering (e.g., switching to white/transparent strokes for erasing).
 */

package com.example.paintify.models

/**
 * Represents the current drawing tool mode.
 *
 * - [PEN]: Standard drawing mode; uses the selected color and width.
 * - [ERASER]: Eraser mode; typically renders over strokes to remove or mask them.
 */
enum class ToolType {
    /** Standard drawing mode (applies currently selected color and width). */
    PEN,

    /** Eraser mode (used to remove or mask existing strokes). */
    ERASER
}

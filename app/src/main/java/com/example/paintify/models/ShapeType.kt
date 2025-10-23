///**
// * Paintify - Shape Type Model
// * ---------------------------
// * Enumerates the available brush/shape types for rendering strokes.
// *
// * Group Members:
// *  - Dustin
// *  - Nolan
// *  - Ian
// *
// * Description:
// * The `ShapeType` enum is used by the drawing pipeline to choose how each
// * stroke is rendered on the canvas (as a continuous line, stamped circles,
// * or stamped rectangles).
// */
//
//package com.example.paintify.models
//
///**
// * Represents the visual style used to render a stroke.
// *
// * - [LINE]: Renders as connected line segments between successive points.
// * - [RECT]: Renders small rectangles centered at each sampled point.
// * - [CIRCLE]: Renders circles centered at each sampled point.
// */
//enum class ShapeType {
//    /** Connected line segments between consecutive points. */
//    LINE,
//
//    /** Rectangular marks centered at the sampled points. */
//    RECT,
//
//    /** Circular marks centered at the sampled points. */
//    CIRCLE
//}

package com.example.paintify.models

/**
 * Shape types.
 */
enum class ShapeType {
    LINE,
    RECT,
    CIRCLE
}

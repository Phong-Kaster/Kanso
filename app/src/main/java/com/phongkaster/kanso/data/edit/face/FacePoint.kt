package com.phongkaster.kanso.data.edit.face

/**
 * One landmark on a face, in pixels of the photo it was found in: [x] from the left edge, [y] from
 * the top edge. Fractions of a pixel are kept, because a face reshape moves pixels by small,
 * smooth amounts and rounding would make the outline step.
 *
 * It is a plain Kotlin class (not `android.graphics.PointF`) so the maths that uses it runs in a
 * JVM test without Android.
 */
data class FacePoint(val x: Float, val y: Float)

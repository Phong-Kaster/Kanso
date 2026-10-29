package com.phongkaster.kanso.data.edit.body

/**
 * One landmark on a body, in pixels of the photo it was found in: [x] from the left edge, [y] from
 * the top edge, and how sure the model is that the point is really visible in the photo
 * ([visibility], 0 to 1).
 *
 * Unlike a face, a body is often only partly in the photo: a portrait cut at the waist has no
 * knees. The pose model still guesses where an unseen point would be, so [visibility] says
 * whether to trust it; a tool skips a body part whose points are hardly visible.
 *
 * It is a plain Kotlin class (not `android.graphics.PointF`) so the maths that uses it runs in a
 * JVM test without Android.
 */
data class BodyPoint(val x: Float, val y: Float, val visibility: Float)

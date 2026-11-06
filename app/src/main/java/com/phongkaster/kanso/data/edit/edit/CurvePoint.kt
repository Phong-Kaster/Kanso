package com.phongkaster.kanso.data.edit.edit

/**
 * One handle on a tone curve: [x] is how bright a pixel is before the curve (0 black, 1 white) and
 * [y] is how bright it becomes after (0 black, 1 white). Both stay in 0..1; [ToneCurve] checks it.
 */
data class CurvePoint(
    /** The input brightness, from 0 (black) to 1 (white). */
    val x: Float,
    /** The output brightness, from 0 (black) to 1 (white). */
    val y: Float,
)

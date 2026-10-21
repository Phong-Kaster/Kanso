package com.phongkaster.kanso.data.settings

/**
 * The limits of Kanso's own text size setting.
 *
 * The setting is a multiplier: 1.0 draws text at the size the design asks for, 1.3 draws it 30%
 * larger. It is deliberately independent of the phone's "font size" setting, which Kanso ignores,
 * so a large system text size can never break Kanso's layout; the person picks the size in
 * Kanso's Settings instead.
 */
object TextScale {
    /** The smallest multiplier the slider offers. */
    const val MIN = 0.85f

    /** The largest multiplier the slider offers: large enough to read easily, small enough to fit. */
    const val MAX = 1.3f

    /** The multiplier a fresh install starts with: the design's own size. */
    const val DEFAULT = 1.0f

    /** The distance between two slider positions. */
    const val STEP = 0.05f

    /** How many in-between stops the slider has, as `Slider(steps = …)` wants it (ends excluded). */
    const val SLIDER_STEPS = ((MAX - MIN) / STEP + 0.5f).toInt() - 1

    /** Returns [value] limited to the range [MIN]..[MAX]. */
    fun clamp(value: Float): Float = value.coerceIn(MIN, MAX)
}

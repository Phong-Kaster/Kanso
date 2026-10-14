package com.phongkaster.kanso.data.edit.edit

/**
 * Everything one run of the Edit tool can do to a photo, as one immutable value: a crop, quarter
 * turns, flips, four colour sliders and a filter. [EditProcessor] applies it, in this order:
 * crop, turn, flip, colour sliders, filter.
 *
 * Immutable means a value never changes after it is made; a change makes a new value with
 * `copy(...)`. That is what Compose wants for state (a new value is a visible change) and what
 * lets a processor hold its settings safely while it runs on another thread.
 *
 * The default value changes nothing: no crop, no turn, no flip, every slider at its middle and no
 * filter. The Edit panel uses the same class twice: once for what its controls show right now,
 * and once, built fresh, for the single change it hands to the processor (see `EditPanel`).
 */
data class EditSettings(
    /** The crop shape, or null for no crop. [CropRatio.FREE] uses [freeCrop]. */
    val cropRatio: CropRatio? = null,
    /** The rectangle a free crop keeps, as fractions of the photo's width and height. */
    val freeCrop: CropRect = CropRect.FULL,
    /**
     * How many quarter turns to turn the photo: positive is clockwise (to the right), negative
     * anticlockwise. Four turns, in either direction, give the photo back as it was.
     */
    val quarterTurns: Int = 0,
    /** True to mirror the photo left to right. */
    val flipHorizontal: Boolean = false,
    /** True to mirror the photo top to bottom. */
    val flipVertical: Boolean = false,
    /** Brightness, from [MIN] (darker) through [MIDDLE] (unchanged) to [MAX] (lighter). */
    val brightness: Float = MIDDLE,
    /** Contrast, from [MIN] (flatter) through [MIDDLE] (unchanged) to [MAX] (punchier). */
    val contrast: Float = MIDDLE,
    /** Saturation, from [MIN] (black and white) through [MIDDLE] (unchanged) to [MAX] (vivid). */
    val saturation: Float = MIDDLE,
    /** Warmth, from [MIN] (cooler, bluer) through [MIDDLE] (unchanged) to [MAX] (warmer, redder). */
    val warmth: Float = MIDDLE,
    /** The colour filter to lay over the photo; [FilterPreset.NONE] for none. */
    val filter: FilterPreset = FilterPreset.NONE,
    /**
     * How strongly the filter is applied, from 0 (not at all: the photo as it was) to 1 (fully).
     * In between, each pixel is mixed from the unfiltered and the fully filtered colour.
     */
    val filterIntensity: Float = 1f,
) {

    /** True when at least one of the four colour sliders is away from its middle. */
    val hasColourChange: Boolean
        get() = brightness != MIDDLE || contrast != MIDDLE || saturation != MIDDLE || warmth != MIDDLE

    /** True when a filter is chosen and its intensity is above 0, so it changes the photo. */
    val hasFilter: Boolean
        get() = filter != FilterPreset.NONE && filterIntensity > 0f

    /**
     * A rectangle inside the photo, each edge given as a fraction from 0 to 1 of the photo's
     * width ([left], [right]) or height ([top], [bottom]), measured from its top-left corner.
     * Fractions, rather than pixels, keep the rectangle right whatever size the photo is worked on.
     */
    data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {

        /** The whole-photo rectangle. */
        companion object {
            /** The rectangle that keeps the whole photo. */
            val FULL = CropRect(0f, 0f, 1f, 1f)
        }
    }

    /** The slider range shared by brightness, contrast, saturation and warmth. */
    companion object {
        /** The lowest value of a colour slider. */
        const val MIN = -1f

        /** The middle of a colour slider: no change at all. */
        const val MIDDLE = 0f

        /** The highest value of a colour slider. */
        const val MAX = 1f
    }
}

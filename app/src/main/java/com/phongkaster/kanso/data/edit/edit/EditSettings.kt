package com.phongkaster.kanso.data.edit.edit

/**
 * Everything the Edit screen has done to the photo so far, as one immutable value: the crop
 * rectangle, quarter turns, flips, four colour sliders and a filter. There is one such value for
 * the whole editing session (held by [EditSession]); every tap or slider move makes a new one.
 *
 * Immutable means a value never changes after it is made; a change makes a new value with
 * `copy(...)`. That is what Compose wants for state (a new value is a visible change) and what
 * lets a processor hold its settings safely while it runs on another thread.
 *
 * **The whole state, never a change.** [EditProcessor] always renders a complete value against the
 * ORIGINAL photo, so a value describes the finished picture, not a step towards it: choosing a
 * second filter replaces the first one (it does not pile on top), and a slider at 70 means 70, not
 * "70 more". That is also why Undo can simply go back to an earlier value (see [EditSession]).
 *
 * The default value changes nothing: the whole photo, no turn, no flip, every slider at its middle
 * and no filter ([isNeutral]).
 */
data class EditSettings(
    /**
     * The rectangle of the original photo to keep, as fractions of its width and height. Every
     * crop is this one rectangle, whatever ratio was chosen: a fixed ratio simply sets a rectangle
     * of that shape (see [EditSession.cropRatioChosen]). [CropRect.FULL] keeps the whole photo.
     */
    val crop: CropRect = CropRect.FULL,
    /**
     * The shape the crop handles are locked to, or null (or [CropRatio.FREE]) when the person may
     * drag them to any shape. It only constrains how [crop] is edited; the processor never reads it.
     */
    val cropRatio: CropRatio? = null,
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
    /** The one colour filter laid over the photo; [FilterPreset.NONE] for none. */
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
     * True when nothing is changed: every tool's controls change nothing right now (see
     * [isNeutral] for one tool). A crop shape that is only locked, four turns that come back
     * round, or a filter at intensity 0 therefore still count as neutral, matching the dots on the
     * tool row. The screen uses it to know whether there is anything to save or to lose.
     */
    val isNeutral: Boolean
        get() = EditTool.entries.all { isNeutral(it) }

    /**
     * True when the controls of [tool]'s sheet change nothing right now, so the tool row can mark
     * the tools that do hold a change:
     *
     * - [EditTool.CROP]: the crop keeps the whole photo;
     * - [EditTool.ROTATE]: the turns come back round to upright and neither flip is on;
     * - [EditTool.ADJUST]: all four sliders sit at their middle;
     * - [EditTool.FILTERS]: no filter, or one at intensity 0.
     */
    fun isNeutral(tool: EditTool): Boolean = when (tool) {
        EditTool.CROP -> crop == CropRect.FULL
        EditTool.ROTATE -> quarterTurns % 4 == 0 && !flipHorizontal && !flipVertical
        EditTool.ADJUST -> !hasColourChange
        EditTool.FILTERS -> !hasFilter
    }

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

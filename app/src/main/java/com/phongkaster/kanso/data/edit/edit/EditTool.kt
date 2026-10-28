package com.phongkaster.kanso.data.edit.edit

/**
 * The seven groups of controls the Edit screen offers, one per entry in the row of tools under the
 * photo. Tapping an entry opens a sheet below the photo holding that group's controls; only one
 * sheet is open at a time.
 *
 * The enum holds no words and no pictures: each tool's name and icon on screen are chosen by the
 * Edit screen's composables in `ui/tool`, so this file stays free of Android resources and can be
 * used by the ViewModel and its tests alone. The order of the cases is the order of the row.
 */
enum class EditTool {

    /** Cutting the photo to a shape: the ratio choices and the free-crop sliders. */
    CROP,

    /** Turning the photo a quarter turn either way, and flipping it across or upside down. */
    ROTATE,

    /** The four colour sliders: brightness, contrast, saturation and warmth. */
    ADJUST,

    /** The ready-made looks (filters) and how strongly the chosen one is applied. */
    FILTERS,

    /** Ready-made pictures (stickers) placed on the photo, then moved, resized and turned. */
    STICKERS,

    /** Words typed onto the photo in a chosen font and colour, then moved, resized and turned. */
    TEXT,

    /** A date and time written onto the photo in a chosen format, font and colour (ADR-006). */
    DATE,
}

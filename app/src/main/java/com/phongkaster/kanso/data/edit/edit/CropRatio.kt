package com.phongkaster.kanso.data.edit.edit

/**
 * The shapes the Edit tool can crop a photo to.
 *
 * Each fixed shape is a width-to-height ratio ([widthPart] : [heightPart]), so 16:9 is a wide
 * picture and 9:16 a tall one. A fixed ratio always cuts the largest centred rectangle of that
 * shape out of the photo: the person only picks the shape, never a position. [FREE] has no ratio
 * of its own: the person sets the rectangle themselves (see [EditSettings.CropRect]).
 *
 * The enum holds only numbers, never words: what each shape is called on screen lives in the
 * panel (`ui/tool/components/toolpanel/editpanel/EditPanel.kt`), so this file stays free of Android resources and can be used
 * by the processor and its tests alone.
 */
enum class CropRatio(val widthPart: Int, val heightPart: Int) {

    /**
     * Back to the whole photo at its own size and shape: choosing it resets the crop rectangle to
     * [EditSettings.CropRect.FULL]. It has no ratio of its own (the parts are 0) and, like
     * [FREE], leaves the handles unlocked.
     */
    ORIGINAL(0, 0),

    /** Any rectangle the person sets; the parts are 0 because there is no fixed ratio. */
    FREE(0, 0),

    /** A square. */
    SQUARE(1, 1),

    /** Landscape 4:3, the shape most phone cameras shoot. */
    FOUR_THREE(4, 3),

    /** Portrait 3:4, the 4:3 shape turned upright. */
    THREE_FOUR(3, 4),

    /** Portrait 4:5, the tallest shape an Instagram feed post keeps without cutting it. */
    FOUR_FIVE(4, 5),

    /** Wide 16:9, the shape of a television or a phone held sideways. */
    SIXTEEN_NINE(16, 9),

    /** Tall 9:16, the shape of a phone screen held upright (stories, wallpapers). */
    NINE_SIXTEEN(9, 16),
}

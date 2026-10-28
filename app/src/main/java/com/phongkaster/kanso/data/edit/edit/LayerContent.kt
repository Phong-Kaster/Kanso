package com.phongkaster.kanso.data.edit.edit

/**
 * What a [Layer] shows. A `sealed interface` is a closed family: every kind is declared right
 * here, so a `when` over a content must name them all and the compiler says when one is missing.
 * The kinds hold only what makes their picture; where the layer sits is the [Layer]'s business.
 */
sealed interface LayerContent {

    /** A ready-made picture from [StickerCatalog], named by its [pictureId]. */
    data class Picture(val pictureId: String) : LayerContent

    /**
     * Words the person typed, drawn by [TextLayerLayout].
     *
     * @param text the words, at most [MAX_LENGTH] characters; a line break is a new line. Empty
     * while a new text waits for its first letter (such a text is never kept, see ADR-005).
     * @param colorArgb the letters' colour as an opaque ARGB number (0xFFRRGGBB).
     * @param font the font family.
     * @param alignment how the lines line up inside the block.
     * @param wrapEms where a line too long wraps, as a width counted in letter heights. It is set
     * once, when the text is added, so that at the starting size a line wraps at 80 percent of the
     * photo's width ([LayerSize.textWrapEms]); because it is counted in letter heights, a bigger
     * or smaller text keeps the same lines and the same shape.
     */
    data class Text(
        val text: String,
        val colorArgb: Int,
        val font: TextFont,
        val alignment: TextAlignment,
        val wrapEms: Float,
    ) : LayerContent {

        /** True when there is nothing to draw: no letters, or only spaces and line breaks. */
        val isBlank: Boolean
            get() = text.isBlank()

        /** The limit of a text's length, and what an empty text measures as. */
        companion object {
            /** The most characters one text layer holds. */
            const val MAX_LENGTH = 200

            /**
             * The words an EMPTY text is measured (and faintly drawn on the photo) as, so a new
             * text has a frame to see and touch before its first letter. It matches the text
             * area's hint (`R.string.text_hint`); it is never drawn into the saved photo.
             */
            const val PLACEHOLDER = "Your text"
        }
    }
}

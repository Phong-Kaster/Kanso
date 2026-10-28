package com.phongkaster.kanso.data.edit.edit

import java.time.LocalDateTime

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
     * @param boxArgb the colour (with its alpha, so it may be see-through) of a rounded box drawn
     * behind the words, or null for none: the sheet's Background row.
     */
    data class Text(
        val text: String,
        val colorArgb: Int,
        val font: TextFont,
        val alignment: TextAlignment,
        val wrapEms: Float,
        val boxArgb: Int? = null,
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

    /**
     * A moment written onto the photo in one of the [DateFormat]s (ADR-006). It keeps the moment
     * and the format, never the words: the words are written from them each time the layer is
     * drawn ([words]), so choosing another day or format rewrites them, and a tap on the selected
     * date can move it to the next format.
     *
     * A date is drawn exactly like a one-line text ([asText]): the same fonts, the same colours,
     * the same background box, the same size slider counted in letter heights.
     *
     * **LocalDateTime.** `java.time.LocalDateTime` is a date and a time of day with no time zone:
     * "28 October 2026, 14:30" wherever the phone is. That is what the person picked, so a photo
     * edited on a trip still says the day they chose.
     *
     * @param moment the date and time, to the minute.
     * @param format how the moment is written.
     * @param colorArgb the letters' colour as an opaque ARGB number (0xFFRRGGBB).
     * @param font the font family.
     * @param boxArgb the colour (with its alpha, so it may be see-through) of a rounded box behind
     * the words, or null for none, as for a text ([Text.boxArgb]).
     */
    data class Date(
        val moment: LocalDateTime,
        val format: DateFormat,
        val colorArgb: Int,
        val font: TextFont,
        val boxArgb: Int? = null,
    ) : LayerContent {

        /** The words this date shows, in English. */
        val words: String
            get() = format.write(moment)

        /**
         * The one-line text that draws this date: its words, colour, font and box, centred, with a
         * wrap width so wide that no format ever breaks onto a second line. Everything that
         * measures or draws a text ([TextLayerLayout]) draws a date through this.
         */
        fun asText(): Text = Text(
            text = words,
            colorArgb = colorArgb,
            font = font,
            alignment = TextAlignment.CENTRE,
            wrapEms = ONE_LINE_EMS,
            boxArgb = boxArgb,
        )

        /** The wrap width that keeps a date on one line. */
        companion object {
            /**
             * A wrap width of 1000 letter heights: far wider than the longest format (about 25
             * letters), so the words never wrap. Only the measuring layout is this wide; the block
             * itself is as wide as the words.
             */
            const val ONE_LINE_EMS = 1000f
        }
    }
}

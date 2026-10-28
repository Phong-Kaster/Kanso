package com.phongkaster.kanso.data.edit.edit

/**
 * The size slider's number, 1 to 100, for each kind of [Layer], and back. A layer stores only its
 * width (a fraction of the original photo's width), the one size the gestures, the frame and the
 * renderers all understand (ADR-005); this object turns that width into the number the sheets
 * show, and a number moved on the slider into a width. Pinching a layer changes the same width,
 * so the slider follows the fingers.
 *
 * - **A sticker:** the number is its width in percent of the photo's width, so 100 is a sticker
 *   as wide as the photo.
 * - **A text:** the number is how tall its letters are: 1 makes a letter 1 percent of the photo's
 *   height and 100 makes it 50 percent, in even steps. A text's width follows from that height
 *   and its words, which are measured elsewhere ([TextLayerLayout]) and passed in here as
 *   `blockWidthEms`, the width of the whole block counted in letter heights.
 *
 * It is plain Kotlin with no Android types, so it can be tested on the JVM.
 */
object LayerSize {

    /** The smallest number the size slider shows. */
    const val MIN = 1f

    /** The largest number the size slider shows. */
    const val MAX = 100f

    /** The size a new text starts at. */
    const val TEXT_DEFAULT = 20f

    /** A text's letter height at size 1, as a fraction of the photo's height. */
    private const val TEXT_SMALLEST = 0.01f

    /** A text's letter height at size 100, as a fraction of the photo's height. */
    private const val TEXT_LARGEST = 0.5f

    /** How much of the photo's width a line of a new text may take before it wraps. */
    private const val TEXT_WRAP_SHARE = 0.8f

    /**
     * The wrap width of a new text, in letter heights ([LayerContent.Text.wrapEms]): at the
     * starting size ([TEXT_DEFAULT]) a line wraps at 80 percent of the photo's width.
     * [photoAspect] is the original photo's width divided by its height.
     */
    fun textWrapEms(photoAspect: Float): Float =
        TEXT_WRAP_SHARE * photoAspect / textLetterHeight(TEXT_DEFAULT)

    /** A sticker's width (fraction of the photo's width) as the slider's number. */
    fun ofPicture(width: Float): Float = (width * MAX).coerceIn(MIN, MAX)

    /** The sticker width (fraction of the photo's width) for the slider's number [size]. */
    fun pictureWidth(size: Float): Float = size.coerceIn(MIN, MAX) / MAX

    /** A text's letter height for the slider's number [size], as a fraction of the photo's height. */
    fun textLetterHeight(size: Float): Float {
        val step = (size.coerceIn(MIN, MAX) - MIN) / (MAX - MIN)
        return TEXT_SMALLEST + (TEXT_LARGEST - TEXT_SMALLEST) * step
    }

    /**
     * The width (fraction of the photo's width) of a text at the slider's number [size]: its
     * letter height in photo pixels times the block's width in letter heights ([blockWidthEms]),
     * over the photo's width. [photoAspect] is the original photo's width divided by its height.
     */
    fun textWidth(size: Float, blockWidthEms: Float, photoAspect: Float): Float =
        textLetterHeight(size) * blockWidthEms / photoAspect

    /**
     * The slider's number for a text that is [width] wide (fraction of the photo's width), the
     * inverse of [textWidth], kept within 1..100.
     */
    fun ofText(width: Float, blockWidthEms: Float, photoAspect: Float): Float {
        if (blockWidthEms <= 0f) return TEXT_DEFAULT
        val letterHeight = width * photoAspect / blockWidthEms
        val step = (letterHeight - TEXT_SMALLEST) / (TEXT_LARGEST - TEXT_SMALLEST)
        return (MIN + step * (MAX - MIN)).coerceIn(MIN, MAX)
    }
}

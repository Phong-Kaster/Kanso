package com.phongkaster.kanso.data.edit.edit

/**
 * One sticker laid on the photo, as an immutable value inside [EditSettings.stickers].
 *
 * Everything about where it sits is a fraction of the ORIGINAL photo (before any crop, turn or
 * flip), not pixels and not screen units. Fractions stay right whatever size the photo is worked
 * on, and they are what lets a crop cut a sticker off and a turn carry it round with the photo:
 * the sticker belongs to the photo, not to the screen.
 *
 * There is deliberately no "selected" field here. Which sticker is selected is only what the
 * screen shows right now, so it lives in the screen's state, and selecting never makes an undo
 * entry.
 */
data class Sticker(
    /**
     * This sticker's identity, used to say which one is selected, deleted or flipped. It is unique
     * for the life of the screen; whoever adds a sticker chooses it (this class never does).
     */
    val key: Long,
    /** Which picture to draw: the id [StickerPictures] understands. */
    val pictureId: String,
    /** The horizontal centre, from 0 (left edge) to 1 (right edge) of the original photo. */
    val centreX: Float,
    /** The vertical centre, from 0 (top edge) to 1 (bottom edge) of the original photo. */
    val centreY: Float,
    /**
     * The sticker's width as a fraction of the original photo's width. Its height follows the
     * picture's own shape, so a sticker is never squashed.
     */
    val width: Float,
    /** How far the sticker is turned, in degrees clockwise, about its centre. */
    val degrees: Float,
    /** True when the picture is mirrored left to right (before it is turned). */
    val mirrored: Boolean,
)

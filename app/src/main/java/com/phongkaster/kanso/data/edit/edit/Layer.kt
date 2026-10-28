package com.phongkaster.kanso.data.edit.edit

/**
 * One thing laid on the photo, as an immutable value inside [EditSettings.layers]: a catalogue
 * sticker or a text (its [content] says which, see ADR-005). Both kinds are moved, pinched,
 * turned, cropped, filtered and undone the same way, because everything about where a layer sits
 * lives here and not in its content.
 *
 * Everything about where it sits is a fraction of the ORIGINAL photo (before any crop, turn or
 * flip), not pixels and not screen units. Fractions stay right whatever size the photo is worked
 * on, and they are what lets a crop cut a layer off and a turn carry it round with the photo:
 * the layer belongs to the photo, not to the screen (ADR-004).
 *
 * There is deliberately no "selected" field here. Which layer is selected is only what the
 * screen shows right now, so it lives in the screen's state, and selecting never makes an undo
 * entry.
 */
data class Layer(
    /**
     * This layer's identity, used to say which one is selected, deleted or flipped. It is unique
     * for the life of the screen; whoever adds a layer chooses it (this class never does).
     */
    val key: Long,
    /** What the layer shows: a catalogue picture or a text. */
    val content: LayerContent,
    /** The horizontal centre, from 0 (left edge) to 1 (right edge) of the original photo. */
    val centreX: Float,
    /** The vertical centre, from 0 (top edge) to 1 (bottom edge) of the original photo. */
    val centreY: Float,
    /**
     * The layer's width as a fraction of the original photo's width. Its height follows the
     * picture's own shape, so a layer is never squashed.
     */
    val width: Float,
    /** How far the layer is turned, in degrees clockwise, about its centre. */
    val degrees: Float,
    /** True when the picture is mirrored left to right (before it is turned). */
    val mirrored: Boolean,
    /**
     * How solid the layer is drawn, from [MIN_OPACITY] (faint) to 1 (fully solid, the default).
     * It never reaches 0: a layer nobody can see could not be found to be selected or deleted.
     */
    val opacity: Float = 1f,
) {

    /** The limits of a layer's opacity. */
    companion object {
        /** The faintest a layer may be made: 10 percent. */
        const val MIN_OPACITY = 0.1f
    }
}

/** The catalogue picture's id when this layer is a sticker, or null when it is not one. */
val Layer.pictureId: String?
    get() = (content as? LayerContent.Picture)?.pictureId

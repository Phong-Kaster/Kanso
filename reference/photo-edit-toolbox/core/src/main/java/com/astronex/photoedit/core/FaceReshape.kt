package com.astronex.photoedit.core

/**
 * The reshapes of the Face tool. New entries are appended, so the order is the order they were added,
 * not an order to show: a layout lists the reshapes it shows, in its own order. Each is one slider from
 * [PhotoEdit.MIN] to [PhotoEdit.MAX], where [PhotoEdit.MIDDLE] changes nothing; read one
 * with [PhotoEdit.amountOf] and set it with [PhotoEditor.setFaceReshape]. Every face found in the
 * photo is reshaped alike.
 *
 * The enum holds no words and no pictures: each reshape's name and icon are the layout's.
 */
enum class FaceReshape {

    /** The cheeks and jaw: wider at the low end, narrower at the high end. */
    WIDTH,

    /** The jowls and the jaw's corners: dropped at the low end, raised and tightened at the high end. */
    LIFT,

    /** The sides of the forehead: hollower at the low end, fuller at the high end. */
    TEMPLE,

    /** The face across the cheekbones, just below the eyes: wider at the low end, slimmer at the high end. */
    CHEEKBONES,

    /** The chin: longer at the low end, shorter at the high end. */
    CHIN_LENGTH,

    /** The hairline: higher (a bigger forehead) at the low end, lower at the high end. */
    HAIRLINE,

    /**
     * Eyes > Size: both eyes, iris included, smaller at the low end and bigger at the high end, each
     * about its own centre; the brows stay where they are.
     */
    EYE_SIZE,

    /** Nose > Size: the nose wider at the low end and narrower at the high end; its length stays. */
    NOSE_SIZE,

    /** Lips > Size: the lips bigger at the low end and smaller at the high end. */
    LIP_SIZE,

    /**
     * Head size: the whole head, hair included, bigger at the low end and smaller at the high end.
     * It is placed from the face alone, so it works on a selfie; a layout may show it outside Face.
     */
    HEAD_SIZE,

    /** The lower jaw only, below the cheekbones: wider at the low end, narrower at the high end. */
    JAWLINE,

    /** The chin's sides and the jaw just above them: a wider chin at the low end, narrower at the high end. */
    CHIN_SHAPE,

    /**
     * Face > Smooth: the outline from the cheeks round to the chin evened out (the jaw's corner drawn
     * in, the dip above it filled) at the high end, the opposite at the low end. The outline only, not
     * the skin.
     */
    SMOOTH,

    /** Face > Chin tip: a rounder, flatter chin at the low end, a more pointed one at the high end. */
    CHIN_TIP,

    /** Face > Jaw angle: a squarer jaw at the low end, its corners raised and drawn in at the high end. */
    JAW_ANGLE,

    /**
     * Face > Double chin: the jaw's lower edge, where it meets the neck, lowered at the low end and
     * lifted at the high end, so a fold under the jaw shows less.
     */
    DOUBLE_CHIN,
}

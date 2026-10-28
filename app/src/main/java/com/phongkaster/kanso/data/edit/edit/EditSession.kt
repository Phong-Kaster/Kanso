package com.phongkaster.kanso.data.edit.edit

/**
 * One Edit screen session: the settings shown right now and the earlier settings Undo can go back
 * to. Pure Kotlin with no Android in it, so the undo rules are tested on the plain JVM.
 *
 * The session is immutable, like [EditSettings]: every method returns a new session and leaves
 * this one as it was. The ViewModel keeps the latest one and publishes its [settings].
 *
 * **What counts as one action.** Undo takes back one action at a time, in reverse order, whichever
 * sheet each came from. An action is a filter chip, a quarter turn, a flip, a crop ratio or a
 * finished crop ([change], [cropRatioChosen]), or one whole slider drag. A drag sends many values
 * while the finger moves ([preview]) and ends with [commit] when it lets go: only the commit makes
 * an undo entry, holding the settings from before the drag began, so a drag of fifty small steps
 * is still one action.
 *
 * Undo needs no pixels: each entry is a whole [EditSettings], and the processor renders any
 * settings from the original photo, so going back is just showing an earlier value again.
 */
data class EditSession(
    /** The settings shown right now (during a drag, the value under the finger). */
    val settings: EditSettings = EditSettings(),
    /** Earlier settings, oldest first: [undo] goes back to the last one. */
    val undoStack: List<EditSettings> = emptyList(),
    /**
     * During a slider drag, the settings from just before it began, which [commit] turns into one
     * undo entry; null when no drag is under way.
     */
    val pending: EditSettings? = null,
) {

    /** True when there is an action to take back. */
    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    /**
     * One action made the settings [new] (a chip, a turn, a flip, a finished crop). The current
     * settings become an undo entry. Settings equal to the current ones change nothing and add no
     * entry, so tapping the chip that is already chosen leaves Undo alone. A drag still under way
     * is committed first, so it keeps its own entry.
     */
    fun change(new: EditSettings): EditSession {
        val settled = commit()
        if (new == settled.settings) return settled
        return EditSession(settings = new, undoStack = settled.undoStack + settled.settings, pending = null)
    }

    /**
     * A slider is being dragged and now stands at [new]: the settings follow it so the photo can
     * be redrawn, but no undo entry is made yet. The first preview of a drag remembers the
     * settings from before it in [pending].
     */
    fun preview(new: EditSettings): EditSession =
        copy(settings = new, pending = pending ?: settings)

    /**
     * The finger let go of a slider: the whole drag becomes one undo entry, holding the settings
     * from before it. A drag that ended where it began adds nothing; with no drag under way this
     * returns the session unchanged.
     */
    fun commit(): EditSession {
        val before = pending ?: return this
        val stack = if (before != settings) undoStack + before else undoStack
        return EditSession(settings = settings, undoStack = stack, pending = null)
    }

    /**
     * Takes back the last action: the settings go back to the newest undo entry, which leaves the
     * stack. A drag still under way is committed first, so Undo takes back the drag itself. With
     * nothing to undo it returns the session unchanged.
     */
    fun undo(): EditSession {
        val settled = commit()
        if (settled.undoStack.isEmpty()) return settled
        return EditSession(
            settings = settled.undoStack.last(),
            undoStack = settled.undoStack.dropLast(1),
            pending = null,
        )
    }

    /**
     * Puts [sticker] on top of the others, as one action. When the photo already holds
     * [EditSettings.MAX_LAYERS] layers nothing is added and the session is returned as it is
     * (no undo entry).
     */
    fun addLayer(layer: Layer): EditSession {
        if (!settings.canAddLayer) return this
        return change(settings.copy(layers = settings.layers + layer))
    }

    /** Takes the sticker with [key] off the photo, as one action; an unknown key changes nothing. */
    fun deleteLayer(key: Long): EditSession =
        change(settings.copy(layers = settings.layers.filter { it.key != key }))

    /**
     * Copies the sticker with [key] as one action: the copy has the key [newKey] (chosen by the
     * caller, since this class never makes keys), sits [offsetX] and [offsetY] away (fractions of
     * the original photo, so the copy does not hide the first) and goes on top. With no such
     * sticker, or a full photo, nothing happens and the session is returned as it is.
     */
    fun duplicateLayer(key: Long, newKey: Long, offsetX: Float, offsetY: Float): EditSession {
        if (!settings.canAddLayer) return this
        val original = settings.layers.firstOrNull { it.key == key } ?: return this
        val copy = original.copy(
            key = newKey,
            centreX = original.centreX + offsetX,
            centreY = original.centreY + offsetY,
        )
        return change(settings.copy(layers = settings.layers + copy))
    }

    /** Mirrors the sticker with [key] left to right (or back), as one action. */
    fun flipLayer(key: Long): EditSession =
        change(
            settings.copy(
                layers = settings.layers.map { if (it.key == key) it.copy(mirrored = !it.mirrored) else it },
            ),
        )

    /**
     * The person chose the crop shape [ratio], as one action.
     *
     * A fixed ratio locks the handles to it ([EditSettings.cropRatio]) and sets the crop to the
     * largest centred rectangle of that shape inside the photo (choosing a ratio must change the
     * rectangle too; only locking it would keep the old shape). [CropRatio.ORIGINAL] resets the
     * rectangle to the whole photo and unlocks the handles. [CropRatio.FREE] or null only
     * unlock the handles and keep the current rectangle.
     *
     * [photoAspect] is the ORIGINAL photo's width divided by its height, before any turn. The
     * crop is cut before the photo is turned, so with an odd number of quarter turns the shape is
     * fitted on its side (3:4 instead of 4:3), and the turned picture comes out in the shape
     * chosen.
     */
    fun cropRatioChosen(ratio: CropRatio?, photoAspect: Float): EditSession {
        if (ratio == CropRatio.ORIGINAL) {
            return change(settings.copy(cropRatio = ratio, crop = EditSettings.CropRect.FULL))
        }
        if (ratio == null || ratio == CropRatio.FREE || photoAspect <= 0f) {
            return change(settings.copy(cropRatio = ratio))
        }
        val sideways = Math.floorMod(settings.quarterTurns, 2) == 1
        // The shape as a width/height number, in the original photo's own orientation.
        val shape = if (sideways) {
            ratio.heightPart.toDouble() / ratio.widthPart
        } else {
            ratio.widthPart.toDouble() / ratio.heightPart
        }
        val crop = centredRect(shape, photoAspect.toDouble())
        return change(settings.copy(cropRatio = ratio, crop = crop))
    }

    /**
     * The largest rectangle of width/height [shape] that fits in a photo of width/height
     * [photoAspect], centred, as fractions of the photo: a shape wider than the photo keeps the
     * full width and trims top and bottom, a narrower one keeps the full height and trims the sides.
     */
    private fun centredRect(shape: Double, photoAspect: Double): EditSettings.CropRect =
        if (shape >= photoAspect) {
            val height = photoAspect / shape
            val top = (1.0 - height) / 2.0
            EditSettings.CropRect(0f, top.toFloat(), 1f, (top + height).toFloat())
        } else {
            val width = shape / photoAspect
            val left = (1.0 - width) / 2.0
            EditSettings.CropRect(left.toFloat(), 0f, (left + width).toFloat(), 1f)
        }
}

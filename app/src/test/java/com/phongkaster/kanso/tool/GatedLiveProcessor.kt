package com.phongkaster.kanso.tool

import android.graphics.Bitmap
import android.graphics.Rect
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.LivePreview
import com.phongkaster.kanso.data.edit.LivePreviewSource
import com.phongkaster.kanso.data.edit.ToolProcessor
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred

/**
 * A slider tool's processor for tests whose live looks can be held back: the n-th live look waits
 * for the n-th gate in [gates] (when there is one) before it answers, so a test can let a full
 * render finish while a live frame is still being made. A full render ([process]) answers at once
 * with a new picture the input's size.
 */
class GatedLiveProcessor(private val gates: List<CompletableDeferred<Unit>> = emptyList()) : ToolProcessor, LivePreviewSource {

    /** Every live look handed out, oldest first. */
    val looks = CopyOnWriteArrayList<LivePreview>()

    /** Answers with a new opaque picture the size of [input]. */
    override suspend fun process(input: Bitmap, onProgress: (Float) -> Unit): EditResult =
        EditResult.Edited(Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888))

    /** Waits for this look's gate, if it has one, then answers with a look over the whole photo. */
    override suspend fun livePreview(photo: Bitmap): LivePreview {
        gates.getOrNull(looks.size)?.await()
        val look = LivePreview(region = Rect(0, 0, photo.width, photo.height), pushes = emptyList(), smoothed = null, smoothing = 0f)
        looks += look
        return look
    }

    /** Nothing is kept between looks. */
    override fun releasePreview() = Unit
}

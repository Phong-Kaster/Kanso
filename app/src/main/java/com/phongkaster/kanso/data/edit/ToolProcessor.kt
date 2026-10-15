package com.phongkaster.kanso.data.edit

import android.graphics.Bitmap

/**
 * One editing step a tool can run on a photo: removing a background, enhancing it, applying a
 * style and so on. It is the seam between the tool screen and the engine that does the work, so
 * the screen's ViewModel runs every tool the same way, and a test can hand it a fake processor
 * instead of the native engine (which cannot load in a JVM test).
 *
 * The contract every processor keeps:
 * - It does its heavy work off the main thread, by switching to a background dispatcher itself
 *   (`withContext(Dispatchers.Default)` for computation, `Dispatchers.IO` for reading files), so
 *   the screen stays responsive while it runs. The caller only launches it in a coroutine.
 * - It reports how far along it is through [process]'s `onProgress`, as a number from 0 (just
 *   started) to 1 (done).
 * - It never throws for a failed edit. It returns [EditResult.Failed] with the reason instead;
 *   an `OutOfMemoryError` becomes [EditFailure.OutOfMemory].
 * - The one exception it lets through is `CancellationException`: when the person leaves the
 *   screen the coroutine is cancelled, and swallowing that would keep work running for nobody.
 *   A cancellation must never be turned into a failure.
 *
 * - An opaque result must have `setHasAlpha(false)` called on it: Save writes a PNG for any bitmap
 *   with an alpha channel and a JPEG otherwise, and `Bitmap.createBitmap(..., ARGB_8888)` starts
 *   with alpha on.
 *
 * Each tool's own processor takes its own settings (a style, a strength, a colour) through its
 * constructor; this interface is only the common way to run one.
 */
interface ToolProcessor {

    /**
     * Edits [input] and returns the edited picture, or the reason it could not. [input] is
     * already scaled down to the tool's working size (see [WorkingSize]) and is not changed: the
     * result is a new bitmap. [onProgress] is called with values from 0 to 1 as the work goes on.
     */
    suspend fun process(input: Bitmap, onProgress: (Float) -> Unit): EditResult
}

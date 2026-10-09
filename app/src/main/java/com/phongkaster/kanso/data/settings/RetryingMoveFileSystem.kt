package com.phongkaster.kanso.data.settings

import java.io.IOException
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path

/**
 * A file system that behaves like [delegate] but tries the "move over the old file" step again
 * when it fails.
 *
 * On Windows a move onto an existing file is refused for a moment while another program, such as
 * an antivirus scanner looking at the freshly written file, still has it open. The block clears
 * within milliseconds, so a few short waits turn a random failure into a success. On Android the
 * first try always works and nothing is waited for.
 */
internal class RetryingMoveFileSystem(delegate: FileSystem) : ForwardingFileSystem(delegate) {
    override fun atomicMove(source: Path, target: Path) {
        var attempt = 0
        while (true) {
            try {
                super.atomicMove(source, target)
                return
            } catch (e: IOException) {
                // Give up after about two seconds and let DataStore report the real error.
                if (++attempt >= MOVE_ATTEMPTS) throw e
                Thread.sleep(MOVE_WAIT_MILLIS)
            }
        }
    }

    /** The retry limits: how many tries a move gets, and how long to wait between them. */
    private companion object {
        const val MOVE_ATTEMPTS = 40
        const val MOVE_WAIT_MILLIS = 50L
    }
}

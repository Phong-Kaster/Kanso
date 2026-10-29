package com.phongkaster.kanso.tool.beautify

import android.graphics.Bitmap
import com.phongkaster.kanso.data.edit.body.BodyFinder
import com.phongkaster.kanso.data.edit.body.BodyLandmarks
import com.phongkaster.kanso.tool.body.SampleBody
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A stand-in for MediaPipe's pose landmarker (which cannot load on the JVM): it records the size
 * of every photo it is handed, then returns the bodies [bodiesFor] makes for that photo; by
 * default one upright figure in the middle of the photo, with no mask.
 */
class FakeBodyFinder(
    private val bodiesFor: (Bitmap) -> List<BodyLandmarks> = { listOf(centre(it)) },
) : BodyFinder {

    /** Width and height of each photo handed to [findBodies], oldest first. */
    val received = CopyOnWriteArrayList<Pair<Int, Int>>()

    /** Records [photo]'s size and returns the fixed bodies. */
    override suspend fun findBodies(photo: Bitmap): List<BodyLandmarks> {
        received += photo.width to photo.height
        return bodiesFor(photo)
    }

    /** Ready-made answers for [bodiesFor]. */
    companion object {

        /**
         * An upright figure on the photo's centre line: shoulders a quarter of the way down and
         * 30% of the width apart, hips 40% of the height lower and 20% of the width apart.
         */
        fun centre(photo: Bitmap): BodyLandmarks = SampleBody.upright(
            centerX = photo.width / 2f,
            top = photo.height * 0.25f,
            length = photo.height * 0.4f,
            shoulderSpread = photo.width * 0.3f,
            hipSpread = photo.width * 0.2f,
        )

        /** A finder that never finds a person. */
        fun none(): FakeBodyFinder = FakeBodyFinder(bodiesFor = { emptyList() })
    }
}

package com.phongkaster.kanso.data.edit.body

import com.phongkaster.kanso.data.edit.face.WarpPush

/**
 * Every body reshape at once, as one list of [WarpPush]es for `LocalWarp` (the saved picture) and
 * `WarpMesh` (the live one): [BodyWaist], [BodyArms], [BodyChest] and [BodyLegs].
 *
 * It is the body's counterpart of `FaceShape`: the processor asks it once per person, so a new body
 * tool is one more line here and one more value, not a new path through the processor.
 */
object BodyShape {

    /**
     * Returns the pushes that reshape [body] by [waist], [arms], [chest] and [legs] (each -1..1,
     * see [BodyWaist], [BodyArms], [BodyChest] and [BodyLegs]). With every amount at 0 the list is
     * empty.
     */
    fun pushesFor(body: BodyLandmarks, waist: Float, arms: Float, chest: Float, legs: Float): List<WarpPush> =
        BodyWaist.pushesFor(body, waist) + BodyArms.pushesFor(body, arms) +
            BodyChest.pushesFor(body, chest) + BodyLegs.pushesFor(body, legs)
}

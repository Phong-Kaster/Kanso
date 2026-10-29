package com.phongkaster.kanso.data.edit.body

import com.phongkaster.kanso.data.edit.face.WarpPush

/**
 * Every body reshape at once, as one list of [WarpPush]es for `LocalWarp` (the saved picture) and
 * `WarpMesh` (the live one): [BodyWaist], [BodyArms] and [BodyChest] for now, Legs as it comes.
 *
 * It is the body's counterpart of `FaceShape`: the processor asks it once per person, so a new body
 * tool is one more line here and one more value, not a new path through the processor.
 */
object BodyShape {

    /**
     * Returns the pushes that reshape [body] by [waist], [arms] and [chest] (each -1..1, see
     * [BodyWaist], [BodyArms] and [BodyChest]). With every amount at 0 the list is empty.
     */
    fun pushesFor(body: BodyLandmarks, waist: Float, arms: Float, chest: Float): List<WarpPush> =
        BodyWaist.pushesFor(body, waist) + BodyArms.pushesFor(body, arms) + BodyChest.pushesFor(body, chest)
}

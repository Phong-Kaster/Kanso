package com.astronex.photoedit.core.reshape

/**
 * Every face reshape at once ([FaceWidthWarp], [FaceLiftWarp], [TempleWarp], [CheekboneWarp],
 * [ChinWarp], [HairlineWarp], [EyeSizeWarp],
 * [NoseSizeWarp],
 * [LipSizeWarp],
 * [HeadSizeWarp],
 * [JawlineWarp],
 * [ChinShapeWarp],
 * [FaceSmoothWarp],
 * [ChinTipWarp],
 * [JawAngleWarp],
 * [DoubleChinWarp]), as one list of [WarpHandle]es for [PixelWarp] (the saved
 * picture) and [GridWarp] (the live one).
 *
 * **Why the pushes are added, not listed side by side.** Width and Lift push the same outline
 * points, and Cheekbones shares Width's top three. Handing [PixelWarp] two pushes on one point
 * would not add their moves: where pushes overlap, [PixelWarp] averages them (it divides by the
 * total weight above 1), so each would be halved. Instead, pushes on the same point become one push whose move is the sum of both and
 * whose reach is the larger of the two, so the point moves as far as Width and Lift ask together.
 */
internal object FaceWarps {

    /**
     * Returns the pushes that reshape [face] by [amounts] (each tool's value, see [FaceWarpAmounts]),
     * merged point by point. With every value at 0 the list is empty; with one tool alone it is
     * exactly that tool's list.
     */
    fun pushesFor(face: FaceMesh, amounts: FaceWarpAmounts): List<WarpHandle> =
        merged(
            FaceWidthWarp.pushesFor(face, amounts.width) +
                FaceLiftWarp.pushesFor(face, amounts.lift) +
                TempleWarp.pushesFor(face, amounts.temple) +
                CheekboneWarp.pushesFor(face, amounts.cheekbones) +
                ChinWarp.pushesFor(face, amounts.chinLength) +
                HairlineWarp.pushesFor(face, amounts.hairline) +
                EyeSizeWarp.pushesFor(face, amounts.eyeSize) +
                NoseSizeWarp.pushesFor(face, amounts.noseSize) +
                LipSizeWarp.pushesFor(face, amounts.lipSize) +
                HeadSizeWarp.pushesFor(face, amounts.headSize) +
                JawlineWarp.pushesFor(face, amounts.jawline) +
                ChinShapeWarp.pushesFor(face, amounts.chinShape) +
                FaceSmoothWarp.pushesFor(face, amounts.smooth) +
                ChinTipWarp.pushesFor(face, amounts.chinTip) +
                JawAngleWarp.pushesFor(face, amounts.jawAngle) +
                DoubleChinWarp.pushesFor(face, amounts.doubleChin),
        )

    /**
     * Returns [pushes] with every group of sliding pushes on the same centre turned into one push:
     * the moves added together, the largest radius kept. The order is that of each centre's first
     * push. Growing pushes are kept as they are, after the sliding ones: [PixelWarp] already adds
     * grows together, and merging one into another would drop its grow.
     */
    fun merged(pushes: List<WarpHandle>): List<WarpHandle> {
        // A LinkedHashMap keeps the centres in the order they first appear.
        val byCentre = LinkedHashMap<Pair<Float, Float>, WarpHandle>()
        val grows = pushes.filter(predicate = { it.grow != 0f })
        pushes.filter(predicate = { it.grow == 0f }).forEach(
            action = { push ->
                val key = push.centerX to push.centerY
                val earlier = byCentre[key]
                byCentre[key] = if (earlier == null) {
                    push
                } else {
                    earlier.copy(
                        moveX = earlier.moveX + push.moveX,
                        moveY = earlier.moveY + push.moveY,
                        radius = maxOf(earlier.radius, push.radius),
                    )
                }
            },
        )
        return byCentre.values.toList() + grows
    }
}

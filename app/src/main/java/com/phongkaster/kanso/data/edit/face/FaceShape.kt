package com.phongkaster.kanso.data.edit.face

/**
 * Every face reshape at once ([FaceWidth], [FaceLift], [FaceTemple]), as one list of [WarpPush]es
 * for [LocalWarp] (the saved picture) and [WarpMesh] (the live one).
 *
 * **Why the pushes are added, not listed side by side.** Width and Lift push the same outline
 * points (and later tools may share points too). Handing [LocalWarp] two pushes on one point would not add their moves: where pushes
 * overlap, [LocalWarp] averages them (it divides by the total weight above 1), so each would be
 * halved. Instead, pushes on the same point become one push whose move is the sum of both and
 * whose reach is the larger of the two, so the point moves as far as Width and Lift ask together.
 */
object FaceShape {

    /**
     * Returns the pushes that reshape [face] by [amounts] (each tool's value, see [FaceAmounts]),
     * merged point by point. With every value at 0 the list is empty; with one tool alone it is
     * exactly that tool's list.
     */
    fun pushesFor(face: FaceLandmarks, amounts: FaceAmounts): List<WarpPush> =
        merged(
            FaceWidth.pushesFor(face, amounts.width) +
                FaceLift.pushesFor(face, amounts.lift) +
                FaceTemple.pushesFor(face, amounts.temple),
        )

    /**
     * Returns [pushes] with every group of pushes on the same centre turned into one push: the
     * moves added together, the largest radius kept. The order is that of each centre's first push.
     */
    fun merged(pushes: List<WarpPush>): List<WarpPush> {
        // A LinkedHashMap keeps the centres in the order they first appear.
        val byCentre = LinkedHashMap<Pair<Float, Float>, WarpPush>()
        pushes.forEach(
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
        return byCentre.values.toList()
    }
}

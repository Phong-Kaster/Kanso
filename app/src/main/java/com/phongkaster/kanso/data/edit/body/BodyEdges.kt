package com.phongkaster.kanso.data.edit.body

/**
 * Finds where a body ends, by walking across its [BodyMask].
 *
 * Pose landmarks sit inside the body, at the joints. To slim a waist, a tool needs the waist's
 * *outline*: it starts on the body's centre line and walks sideways, one pixel at a time, until
 * the mask says "background". That first background pixel is the edge.
 */
object BodyEdges {

    /**
     * Walks from ([fromX], [fromY]) along the direction ([dirX], [dirY]) (a unit vector), one
     * pixel per step, and returns how far it went before [mask] first said "not the person"
     * (below [PERSON]). Returns null when it walked [maxDistance] pixels without leaving the
     * person, or when it started outside the person: then the mask cannot tell where the edge is.
     */
    fun distanceToEdge(mask: BodyMask, fromX: Float, fromY: Float, dirX: Float, dirY: Float, maxDistance: Float): Float? {
        if (mask.at(fromX, fromY) < PERSON) return null
        var step = 1
        while (step <= maxDistance) {
            if (mask.at(fromX + dirX * step, fromY + dirY * step) < PERSON) return step.toFloat()
            step++
        }
        return null
    }

    /** A mask value at or above this counts as the person; below it, as background. */
    const val PERSON = 0.5f
}

package com.apero.pinity.ui.screen.edit_image.decal

import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.apero.pinity.ui.screen.edit_image.decal.enums.adjacent
import com.apero.pinity.ui.screen.edit_image.decal.enums.calculateAngle
import com.apero.pinity.ui.screen.edit_image.decal.enums.rotate
import com.apero.pinity.ui.screen.edit_image.decal.enums.roundToIntOffset
import com.apero.pinity.ui.screen.edit_image.decal.enums.roundToIntSize
import com.apero.pinity.ui.screen.edit_image.decal.model.DecalNumericClockOverlay
import com.apero.pinity.ui.screen.edit_image.decal.model.DecalTextOverlay
import com.apero.pinity.util.Five
import kotlin.math.roundToInt

/**
 * Modifier node to handle interaction with overlay
 */
private class DecalStickerPointerNode(
    var overlayState: DecalOverlayState,
    val density: Density,
    var onActive: (Boolean) -> Unit,
    var onTransforming: (Boolean) -> Unit,
    var onDelete: () -> Unit,
    var onClick: () -> Unit,
) : PointerInputModifierNode, Modifier.Node() {
    // Flag indicating if current sticker is active for edit
    var active = false

    // Current pointer id on sticker
    private var activePointerId: PointerId? = null

    // Flag indicating if editing is in resizing state
    private var resizing = false

    // Flag indicating if editing is in rotating state
    private var rotating = false

    // Click detection variables
    private var pressStartTime = 0L
    private var pressStartPosition = Offset.Zero
    private var hasMoved = false
    private val clickTimeThreshold = 300L // milliseconds
    private val clickDistanceThreshold = with(density) { 10.dp.toPx() }

    override fun onCancelPointerInput() {}

    /**
     * PointerEventPass Documentation
     * =============================
     *
     * Jetpack Compose processes touch events in 3 phases to handle complex UI hierarchies.
     * Think of it like a 3-step conversation between parent and child components:
     *
     * 📋 THE 3 PASSES:
     *
     * 1️⃣ INITIAL PASS (Parent → Child)
     *    - Events travel DOWN the UI tree (from root to leaf)
     *    - Parents get first chance to intercept gestures
     *    - Example: A scrollable container might intercept drag gestures
     *    - Rarely used in typical apps
     *
     * 2️⃣ MAIN PASS (Child → Parent) ⭐ MOST IMPORTANT
     *    - Events travel UP the UI tree (from leaf to root)
     *    - This is where 99% of gesture handling happens
     *    - Children handle gestures first, then parents
     *    - Your onClick, onDrag, etc. should run here
     *
     * 3️⃣ FINAL PASS (Parent → Child)
     *    - Events travel DOWN again
     *    - Components can see what was consumed by others
     *    - Used for cleanup or state updates
     *
     * 🐛 WHY THIS MATTERS:
     * Without filtering by pass, your gesture callbacks run 3 times!
     *
     * Example:
     * - User taps once
     * - onPointerEvent() called 3 times (once per pass)
     * - onClick() gets called 3 times
     * - Result: Confusing behavior and multiple logs
     *
     * 💡 SOLUTION:
     * Always filter to Main pass unless you specifically need another pass:
     * if (pass != PointerEventPass.Main) return
     *
     * 🔄 ANALOGY:
     * Think of it like a meeting discussion:
     * 1. Initial: "Does anyone want to speak first?" (parents can intercept)
     * 2. Main: Normal discussion flow (standard gesture handling)
     * 3. Final: "Any final thoughts?" (cleanup and state updates)
     */
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        // Only handle gestures in Main pass to avoid duplicates
        if (pass != PointerEventPass.Main) return

        // Calculate multiplier between real image size and on device image size
        val adjacentThreshold = with(density) { 12.dp.toPx() }
        var (overlayRect, topLeft, topRight, bottomRight, bottomLeft) = getStickerBounds(bounds)

        // Normalize position after rotation
        if (overlayState.rotationDegrees != 0f) {
            val center = overlayRect.center
            topLeft = topLeft.rotate(overlayState.rotationDegrees, center)
            topRight = topRight.rotate(overlayState.rotationDegrees, center)
            bottomRight = bottomRight.rotate(overlayState.rotationDegrees, center)
            bottomLeft = bottomLeft.rotate(overlayState.rotationDegrees, center)
        }

        // Handle pointer event if current sticker is active for edit
        if (active) {
            // Handle multiple touch gestures
            if (pointerEvent.changes.size > 1) {
                // Detect zoom gestures
                val zoomChange = (pointerEvent.calculateZoom() - 1) / 2 + 1
                val rotationChange = pointerEvent.calculateRotation()
                val panChange = pointerEvent.calculatePan() / 2f

                if (overlayState.overlay is DecalTextOverlay) {
                    val overlay = overlayState.overlay as DecalTextOverlay
                    val newOverlay =
                        overlay.copy(fontSize = (overlay.fontSize * zoomChange).coerceIn(1f, 100f))
                    overlayState.overlay = newOverlay
                } else if (overlayState.overlay is DecalNumericClockOverlay) {
                    val overlay = overlayState.overlay as DecalNumericClockOverlay
                    val newOverlay =
                        overlay.copy(fontSize = (overlay.fontSize * zoomChange).coerceIn(1f, 100f))
                    overlayState.overlay = newOverlay
                } else {
                    overlayState.size = overlayState.size.let {
                        IntSize(
                            width = (it.width * zoomChange).roundToInt(),
                            height = (it.height * zoomChange).roundToInt()
                        )
                    }
                    overlayState.rotationDegrees += rotationChange
                    overlayState.centerOffset = overlayState.centerOffset.let {
                        IntOffset(
                            x = (it.x + panChange.x).roundToInt(),
                            y = (it.y + panChange.y).roundToInt()
                        )
                    }
                }

                pointerEvent.consumeAll()
                return
            }

            // Handle single touch gestures
            if (pointerEvent.type == PointerEventType.Move) {
                // Check if pointer has moved significantly for click detection
                val currentPosition = pointerEvent.changes.first().position
                val distance = (currentPosition - pressStartPosition).getDistance()
                if (distance > clickDistanceThreshold) {
                    hasMoved = true
                }

                if (resizing) {
                    if (overlayState.overlay is DecalTextOverlay) {
                        val overlay = overlayState.overlay as DecalTextOverlay
                        // Calculate new size, then convert it to text size scale
                        val newSize = ((pointerEvent.changes.first().position.rotate(
                            -overlayState.rotationDegrees,
                            overlayRect.center
                        ) -
                                topLeft.rotate(
                                    -overlayState.rotationDegrees,
                                    overlayRect.center
                                ))).roundToIntSize()

                        val scale = newSize.width.toFloat() / overlayState.size.width

                        val newOverlay =
                            overlay.copy(fontSize = (overlay.fontSize * scale).coerceIn(1f, 100f))
                        overlayState.overlay = newOverlay
                    }
                    else if(overlayState.overlay is DecalNumericClockOverlay){
                        val overlay = overlayState.overlay as DecalNumericClockOverlay
                        // Calculate new size, then convert it to text size scale
                        val newSize = ((pointerEvent.changes.first().position.rotate(
                            -overlayState.rotationDegrees,
                            overlayRect.center
                        ) -
                                topLeft.rotate(
                                    -overlayState.rotationDegrees,
                                    overlayRect.center
                                ))).roundToIntSize()

                        val scale = newSize.width.toFloat() / overlayState.size.width

                        val newOverlay =
                            overlay.copy(fontSize = (overlay.fontSize * scale).coerceIn(1f, 100f))
                        overlayState.overlay = newOverlay
                    }
                    else {
                        // Calculate and set new size of sticker
                        overlayState.size = ((pointerEvent.changes.first().position.rotate(
                            -overlayState.rotationDegrees,
                            overlayRect.center
                        ) -
                                topLeft.rotate(
                                    -overlayState.rotationDegrees,
                                    overlayRect.center
                                ))).roundToIntSize()

                        // Override size to match original aspect ratio
                        val originalAspectRatio =
                            overlayState.bitmap.width.toFloat() / overlayState.bitmap.height
                        overlayState.size = IntSize(
                            width = overlayState.size.width,
                            height = (overlayState.size.width / originalAspectRatio).roundToInt()
                        )
                    }
                } else if (rotating) {
                    // Calculate rotated degrees and set new rotation degrees
                    val firstPointer = pointerEvent.changes.first()
                    val angleOffset = calculateAngle(
                        overlayRect.center,
                        firstPointer.previousPosition,
                        firstPointer.position
                    )
                    overlayState.rotationDegrees -= angleOffset / 2f
                } else {
                    // Calculate position change and set new position of sticker
                    val positionChange = pointerEvent.changes.first().positionChange()
                    overlayState.centerOffset += positionChange.roundToIntOffset()
                }

                pointerEvent.consumeAll()
                return
            }

            if (pointerEvent.type == PointerEventType.Release) {
                val currentTime = System.currentTimeMillis()
                val pressDuration = currentTime - pressStartTime

                if (resizing) {
                    resizing = false
                } else if (rotating) {
                    rotating = false
                } else if (!hasMoved && pressDuration <= clickTimeThreshold) {
                    onClick()
                }

                onTransforming(false)
                pointerEvent.consumeAll()
                return
            }

            if (pointerEvent.type == PointerEventType.Press) {
                if (!resizing && !rotating) {
                    val firstPointer = pointerEvent.changes.first()
                    val position = firstPointer.position

                    // Initialize click detection
                    pressStartTime = System.currentTimeMillis()
                    pressStartPosition = position
                    hasMoved = false

                    when {
                        bottomRight.adjacent(position, adjacentThreshold) -> {
                            resizing = true
                            onTransforming(true)
                        }
                        bottomLeft.adjacent(position, adjacentThreshold) -> {
                            rotating = true
                            onTransforming(true)
                        }
                        topRight.adjacent(position, adjacentThreshold) -> onDelete()
                        !overlayRect.contains(position.rotate(-overlayState.rotationDegrees, overlayRect.center)) -> {
                            active = false
                            onActive(false)
                            activePointerId = null
                            return
                        }
                    }
                }

                pointerEvent.consumeAll()
                return
            }
        } else {
            if (pointerEvent.type == PointerEventType.Press) {
                val firstPointer = pointerEvent.changes.firstOrNull { !it.isConsumed }
                if (firstPointer == null) {
                    onActive(false)
                    return
                }

                val active = overlayRect.contains(
                    firstPointer.position.rotate(
                        -overlayState.rotationDegrees,
                        overlayRect.center
                    )
                )
                activePointerId = if (active) firstPointer.id else null
                if (active) firstPointer.consume()
                onActive(active)
            }
        }
    }

    override fun sharePointerInputWithSiblings(): Boolean = true

    private fun getStickerBounds(bounds: IntSize): Five<Rect, Offset, Offset, Offset, Offset> {
        val left = bounds.width / 2f - overlayState.size.width / 2f + overlayState.centerOffset.x
        val top = bounds.height / 2f - overlayState.size.height / 2f + overlayState.centerOffset.y
        val right = left + overlayState.size.width
        val bottom = top + overlayState.size.height

        val topLeft = Offset(left, top)
        val topRight = Offset(right, top)
        val bottomRight = Offset(right, bottom)
        val bottomLeft = Offset(left, bottom)

        return Five(Rect(topLeft, bottomRight), topLeft, topRight, bottomRight, bottomLeft)
    }
}

private data class DecalStickerPointerElement(
    val overlayState: DecalOverlayState,
    val active: Boolean,
    val density: Density,
    val onActive: (Boolean) -> Unit,
    val onTransforming: (Boolean) -> Unit,
    val onDelete: () -> Unit,
    val onClick: () -> Unit,
) : ModifierNodeElement<DecalStickerPointerNode>() {
    override fun create(): DecalStickerPointerNode = DecalStickerPointerNode(
        overlayState = overlayState,
        density = density,
        onActive = onActive,
        onTransforming = onTransforming,
        onDelete = onDelete,
        onClick = onClick,
    ).also { it.active = active }

    override fun update(node: DecalStickerPointerNode) {
        node.overlayState = overlayState
        node.onActive = onActive
        node.onTransforming = onTransforming
        node.onDelete = onDelete
        node.active = active
        node.onClick = onClick
    }

    override fun InspectorInfo.inspectableProperties() {}
}

fun Modifier.stickerPointer(
    overlayState: DecalOverlayState,
    active: Boolean,
    density: Density,
    onActive: (Boolean) -> Unit,
    onTransforming: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) = this then DecalStickerPointerElement(
    overlayState = overlayState,
    active = active,
    density = density,
    onActive = onActive,
    onTransforming = onTransforming,
    onDelete = onDelete,
    onClick = onClick
)

fun IntSize.toOffset(): Offset = Offset(width.toFloat(), height.toFloat())

fun PointerEvent.consumeAll() = changes.forEach { it.consume() }
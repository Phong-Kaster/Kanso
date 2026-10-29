package com.phongkaster.kanso.ui.tool.model

/**
 * Every Beautify slider's value at one moment, handed from the panel to the screen together, so a
 * live frame and a full render always see the same set.
 *
 * One value per tool, each as its slider holds it:
 * @param smoothing Smooth, 0 (none) to 1 (fully smoothed).
 * @param faceWidth Face > Width, -1 (wider) through 0 to 1 (narrower).
 * @param faceLift Face > Lift, -1 (jaw dropped) through 0 to 1 (lifted).
 * @param waist Body > Waist, -1 (wider) through 0 to 1 (slimmer).
 * @param arms Body > Arms, -1 (thicker) through 0 to 1 (slimmer).
 * @param chest Body > Chest, -1 (flatter) through 0 to 1 (fuller).
 * @param legs Body > Legs, -1 (thicker) through 0 to 1 (slimmer).
 * @param faceTemple Face > Temple, -1 (hollower) through 0 to 1 (fuller).
 *
 * It is one object, not loose numbers, because so many values of the same type passed one after
 * another are easy to swap by mistake; each is named here. The defaults are the panel's
 * starting values: nothing changed.
 */
data class BeautifyAmounts(
    val smoothing: Float = 0f,
    val faceWidth: Float = 0f,
    val faceLift: Float = 0f,
    val waist: Float = 0f,
    val arms: Float = 0f,
    val chest: Float = 0f,
    val legs: Float = 0f,
    val faceTemple: Float = 0f,
)

package com.phongkaster.kanso.ui.camera

/**
 * What the flash does when a photo is taken.
 *
 * The flash only fires for the still photo; the live picture never lights it.
 */
enum class FlashMode {
    /** The camera decides: it fires the flash only when the scene is too dark. */
    Auto,

    /** The flash fires for every photo. */
    On,

    /** The flash never fires. */
    Off;

    /** Returns the mode the flash button moves to next: auto, then on, then off, then auto. */
    fun next(): FlashMode = when (this) {
        Auto -> On
        On -> Off
        Off -> Auto
    }
}

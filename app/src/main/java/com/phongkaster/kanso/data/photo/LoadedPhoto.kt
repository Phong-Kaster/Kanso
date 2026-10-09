package com.phongkaster.kanso.data.photo

import android.graphics.Bitmap

/**
 * One photo decoded for display: [bitmap] is already upright and no larger than the screen needs.
 */
data class LoadedPhoto(val bitmap: Bitmap)

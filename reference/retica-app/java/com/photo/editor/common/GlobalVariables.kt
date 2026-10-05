package com.photo.editor.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object GlobalVariables {
    var inAppUpdateChecked = false

    // Show promotion dialog at home
    var showPromotionDialog by mutableStateOf(true)
    var showPromotionDialogFirstTime by mutableStateOf(false)
//    var showGiftStickerDialog by mutableStateOf(false)

    var watchedRewardRemoveBG by mutableStateOf(false)
    var watchedRewardRetouch by mutableStateOf(false)
    var watchedRewardArtistic by mutableStateOf(false)
    var watchedRewardEnhance by mutableStateOf(false)


    fun resetSession() {
        inAppUpdateChecked = false
        showPromotionDialog = true
        showPromotionDialogFirstTime = false
//        showGiftStickerDialog = false


        watchedRewardRemoveBG = false
        watchedRewardRetouch = false
        watchedRewardArtistic = false
        watchedRewardEnhance = false
    }
}
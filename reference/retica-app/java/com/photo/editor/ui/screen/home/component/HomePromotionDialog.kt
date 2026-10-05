package com.photo.editor.ui.screen.home.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.photo.editor.common.GlobalVariables
import com.photo.editor.ui.component.dialog.promotion.FilterPromotionDialog
import com.photo.editor.ui.component.dialog.promotion.RealisticPromotionDialog
import com.photo.editor.ui.component.dialog.promotion.RemoveBackgroundPromotionDialog

@Composable
fun HomePromotionDialog(
    promotionIndexProvider: () -> Int,
    screenViewed: Boolean,
    onTryClick: (Int) -> Unit,
    onDismiss: (Int) -> Unit,
) {
    LaunchedEffect(Unit) {
        if (GlobalVariables.showPromotionDialogFirstTime && !GlobalVariables.showPromotionDialog) {
            GlobalVariables.showPromotionDialog = true
            GlobalVariables.showPromotionDialogFirstTime = false
        }
    }

    if (GlobalVariables.showPromotionDialog) {
        val dialogIndex = rememberSaveable { promotionIndexProvider() }
        if (dialogIndex == 0) {
            LaunchedEffect(Unit) {
                GlobalVariables.showPromotionDialog = false
                GlobalVariables.showPromotionDialogFirstTime = true
            }
        }

        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(screenViewed) {
            if (screenViewed) {
                visible = true
            }
        }

        if (visible) {
            when (dialogIndex) {
                0 -> {}

                1 -> RealisticPromotionDialog(
                    showDialog = true,
                    onDismissRequest = {
                        GlobalVariables.showPromotionDialog = false
                        onDismiss(0)
                    },
                    modifier = Modifier
                        .clipToBounds(),
                    onConfirm = {
                        GlobalVariables.showPromotionDialog = false
                        onTryClick(0)
                    }
                )

                2 -> FilterPromotionDialog(
                    showDialog = true,
                    onDismissRequest = {
                        GlobalVariables.showPromotionDialog = false
                        onDismiss(1)
                    },
                    modifier = Modifier
                        .clipToBounds(),
                    onConfirm = {
                        GlobalVariables.showPromotionDialog = false
                        onTryClick(1)
                    }
                )

                3 -> RemoveBackgroundPromotionDialog(
                    showDialog = true,
                    onDismissRequest = {
                        GlobalVariables.showPromotionDialog = false
                        onDismiss(2)
                    },
                    modifier = Modifier
                        .clipToBounds(),
                    onConfirm = {
                        GlobalVariables.showPromotionDialog = false
                        onTryClick(2)
                    }
                )

                else -> {
                    LaunchedEffect(Unit) {
                        GlobalVariables.showPromotionDialog = false
                    }
                }
            }
        }
    }
}
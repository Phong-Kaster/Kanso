package com.photo.editor.common.ui.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.photo.editor.common.ui.modifier.rounded

@Composable
fun BaseBottomSheet(
    showBottomSheet: Boolean,
    onDismissRequest: () -> Unit,
    showHandler: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable () -> Unit,
) {
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            shape = RoundedCornerShape(16.dp, 16.dp),
            sheetState = sheetState,
            dragHandle = null,
            contentWindowInsets = { WindowInsets(0.dp) },
            modifier = Modifier.statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
            ) {
                if (showHandler) {
                    Spacer(
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 12.dp)
                            .width(80.dp)
                            .height(4.dp)
                            .rounded(10.dp)
                            .background(Color(0xFF284C58).copy(0.1f))
                            .align(Alignment.CenterHorizontally)
                    )
                }

                content()
            }
        }
    }
}
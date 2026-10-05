package com.photo.editor.ui.component.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.common.ui.theme.DarkNeutralTitle
import com.photo.editor.common.ui.theme.extraBold18

@Composable
fun CommonTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    rightContent: @Composable () -> Unit = {},
    showBackButton: Boolean = true,
    positionTitle: Alignment = Alignment.Center,
    textTitleStyle: TextStyle = extraBold18
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        BaseTopBar {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(64.dp)
            ) {

                if (showBackButton) {
                    CenterBox(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 10.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back),
                            tint = DarkNeutralTitle,
                            modifier = Modifier
                                .size(24.dp)
                                .rippleClickable {
                                    onBackClick()
                                }
                        )
                    }
                }

                Text(
                    text = title,
                    style = textTitleStyle,
                    color = Color.Black,
                    modifier = Modifier
                        .padding(horizontal = 50.dp)
                        .align(positionTitle),
                    textAlign = TextAlign.Center
                )

                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    rightContent()
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    CommonTopBar(title = stringResource(R.string.app_name), onBackClick = { })
}
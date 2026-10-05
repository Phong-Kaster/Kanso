package com.photo.editor.ui.screen.artistic.edit.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.BaseSlider
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.medium12
import com.photo.editor.common.ui.theme.regular14

@Composable
fun ArtisticEditValueSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    title: String,
    valueString: String = (value * 100)
        .toInt()
        .toString(),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(62.dp)
            .fillMaxWidth()
            .background(Color.Black.copy(0.7f))
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CenterRow {
            Text(
                text = title,
                style = medium12,
                color = Color.White,
            )
            var showInfoPopup by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.info),
                    modifier = Modifier
                        .size(15.dp)
                        .rippleClickable {
                            showInfoPopup = true
                        },
                    tint = Color.White,
                )

                if (showInfoPopup) {
                    Popup(
                        onDismissRequest = { showInfoPopup = false },
                        properties = PopupProperties(focusable = true),
                        alignment = Alignment.BottomCenter,
                        offset = IntOffset(
                            x = 0,
                            y = with(LocalDensity.current) { (-20.dp).roundToPx() })
                    ) {
                        Text(
                            text = stringResource(R.string.artistic_blend_ratio_info),
                            style = regular14,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .rounded(10.dp)
                                .background(Color.Black)
                                .padding(6.dp)
                        )
                    }
                }
            }

            Text(
                text = valueString,
                style = medium12,
                color = Color.White,
            )
        }

        BaseSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun Preview() {
    ArtisticEditValueSlider(
        value = 0.5f,
        onValueChange = {},
        title = stringResource(R.string.app_name),
        valueString = "50",
    )
}
package com.photo.editor.ui.screen.edit.component.bottombar.edit.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.BuildConfig
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.ui.screen.edit.common.EditImageColorState
import com.photo.editor.ui.screen.edit.enums.ColorCurveType
import com.photo.editor.common.ui.theme.regular12

@Composable
fun ColorCurveEditor(
    colorState: EditImageColorState,
    modifier: Modifier = Modifier,
) {
    val colorData by colorState.colorDataFlow.collectAsState()
    var currentCurveOption by remember { mutableStateOf(ColorCurveType.BlackWhite) }
    val curveState by remember(currentCurveOption) {
        mutableStateOf(
            ColorCurveState(
                initialPoints = when (currentCurveOption) {
                    ColorCurveType.BlackWhite -> colorData.blackWhiteCurvePoints
                    ColorCurveType.Red -> colorData.redCurvePoints
                    ColorCurveType.Green -> colorData.greenCurvePoints
                    ColorCurveType.Blue -> colorData.blueCurvePoints
                }
            )
        )
    }
    var showPointsInfo by remember { mutableStateOf(false) }

    CenterColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(0.7f))
            .padding(vertical = 16.dp, horizontal = 24.dp)
    ) {
        if (showPointsInfo) {
            Text(
                text = curveState.points.joinToString(separator = "; ") { "(%.03f - %.03f)".format(it.first * 255, it.second * 255) },
                style = regular12,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(bottom = 12.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            CenterRow(
                modifier = Modifier
                    .align(Alignment.Center),
                itemSpacing = 10.dp,
            ) {
                ColorCurveType.entries.forEach {
                    Spacer(
                        modifier = Modifier
                            .size(20.dp)
                            .circle()
                            .border(if (currentCurveOption == it) 6.dp else 3.dp, it.brush, CircleShape)
                            .then(
                                if (currentCurveOption == it) Modifier.background(Color.White)
                                else Modifier.clickable { currentCurveOption = it }
                            )
                    )
                }
            }

            if (curveState.activePointIndex > 0 && curveState.activePointIndex < curveState.points.size - 1) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.delete),
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(20.dp)
                        .rippleClickable {
                            curveState.deleteActivePoint()
                            colorState.updateColorCurve(currentCurveOption, curveState.points)
                        }
                )
            }

            if (BuildConfig.dev) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.info),
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(20.dp)
                        .rippleClickable {
                            showPointsInfo = !showPointsInfo
                        }
                )
            }
        }

        BaseColorCurveEditor(
            state = curveState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.5f),
            lineColor = currentCurveOption.lineColor,
            onCurveChange = {
                colorState.updateColorCurve(currentCurveOption, curveState.points)
            }
        )
    }
}

@Preview
@Composable
private fun Preview() {
    ColorCurveEditor(
        colorState = EditImageColorState()
    )
}
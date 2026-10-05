package com.photo.editor.common.ui.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.library.colorpicker.HSVColorPicker
import com.photo.editor.common.R
import com.photo.editor.common.ui.modifier.backgroundColor
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.common.ui.theme.regular14

@Composable
fun ColorPickerBottomSheet(
    showBottomSheet: Boolean,
    onDismissRequest: () -> Unit,
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
) {
    BaseBottomSheet(
        showBottomSheet = showBottomSheet,
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        ColorPickerBottomSheetContent(
            initialColor = initialColor,
            onColorChanged = onColorChanged,
        )
    }
}

@Composable
private fun ColorPickerBottomSheetContent(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
) {
    var color by remember { mutableStateOf(initialColor) }
    var refreshColorPickerKey by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        HSVColorPicker(
            color = color,
            onColorChanged = {
                color = it
                onColorChanged(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            refreshKey = refreshColorPickerKey,
            aspectRatio = 2f,
        )

        Text(
            text = stringResource(R.string.basic_colors),
            style = regular14,
            color = Color.Black,
            modifier = Modifier.padding(top = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf(0xFFEF4444, 0xFFF97316, 0xFFFACC15, 0xFF4ADE80, 0xFF2DD4BF, 0xFF3B82F6, 0xFF6366F1).forEach {
                Spacer(
                    modifier = Modifier
                        .size(24.dp)
                        .circle()
                        .backgroundColor(it)
                        .clickable {
                            color = Color(it)
                            onColorChanged(color)
                            refreshColorPickerKey++
                        }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf(0xFFEC4899, 0xFFF43F5E, 0xFFD946EF, 0xFF8B5CF6, 0xFF0EA5E9, 0xFF10B981, 0xFF84CC16).forEach {
                Spacer(
                    modifier = Modifier
                        .size(24.dp)
                        .circle()
                        .backgroundColor(it)
                        .clickable {
                            color = Color(it)
                            onColorChanged(color)
                            refreshColorPickerKey++
                        }
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ColorPickerBottomSheetContent(
        initialColor = Color.White,
        onColorChanged = {}
    )
}
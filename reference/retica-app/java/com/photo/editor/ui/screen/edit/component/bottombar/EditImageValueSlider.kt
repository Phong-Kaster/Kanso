package com.photo.editor.ui.screen.edit.component.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.BaseSlider
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.theme.medium12

@Composable
fun EditImageValueSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    title: String,
    valueString: String = (value * 100).toInt().toString(),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
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
                modifier = Modifier.weight(1f)
            )

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
    EditImageValueSlider(
        value = 0.5f,
        onValueChange = {},
        title = stringResource(R.string.app_name),
        valueString = "50",
    )
}
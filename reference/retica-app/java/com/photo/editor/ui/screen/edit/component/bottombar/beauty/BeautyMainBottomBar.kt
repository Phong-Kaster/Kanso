package com.photo.editor.ui.screen.edit.component.bottombar.beauty

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.ui.theme.appTextStyle
import com.photo.editor.common.ui.theme.medium12
import com.photo.editor.ui.screen.edit.enums.EditImageBeautyOption

@Composable
fun BeautyMainBottomBar(
    selectedOption: EditImageBeautyOption?,
    onOptionSelected: (EditImageBeautyOption) -> Unit,
    onAdjustClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(bottom = 12.dp, top = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier)

            EditImageBeautyOption.entries.forEach { option ->
                CenterColumn(
                    modifier = Modifier
                        .size(64.dp)
                        .rounded(5.dp)
                        .then(
                            if (selectedOption == option) Modifier.background(Color.Black)
                            else Modifier
                        )
                        .clickable {
                            onOptionSelected(option)
                        }
                ) {
                    Icon(
                        painter = painterResource(option.iconId),
                        contentDescription = stringResource(option.titleId),
                        modifier = Modifier.size(32.dp),
                        tint = if (selectedOption == option) SubColor else Color.Black,
                    )

                    Text(
                        text = stringResource(option.titleId).uppercase(),
                        style = appTextStyle(8, 700),
                        color = if (selectedOption == option) SubColor else Color.Black,
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))
        }


        CenterColumn(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp, 0.dp, 0.dp, 10.dp))
                .background(AlternativeColor)
                .clickable { onAdjustClick() }
                .padding(start = 2.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_adjust),
                contentDescription = stringResource(R.string.adjust),
                modifier = Modifier.size(32.dp),
                tint = Color.Black,
            )

            Text(
                text = stringResource(R.string.adjust),
                style = medium12,
                color = Color.Black,
                modifier =Modifier.basicMarquee(Int.MAX_VALUE)
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    BeautyMainBottomBar(
        selectedOption = EditImageBeautyOption.Brighten,
        onOptionSelected = {},
        onAdjustClick = {}
    )
}
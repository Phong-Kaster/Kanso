package com.photo.editor.ui.component.button

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.BorderColor
import com.photo.editor.common.ui.theme.DarkNeutralTitle
import com.photo.editor.common.ui.theme.PrimaryColor
import com.photo.editor.common.ui.theme.PrimaryYellowLightActive
import com.photo.editor.common.ui.theme.PrimaryYellowLightPrimary
import com.photo.editor.common.ui.theme.SignatureGradient
import com.photo.editor.common.ui.theme.appTextStyle
import com.photo.editor.common.ui.theme.bold16
import com.photo.editor.common.ui.theme.medium16
import com.photo.editor.common.ui.theme.regular16


@Composable
fun SimpleOutlineButton(
    title: String,
    modifier: Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    activated: Boolean = false,
    shape: Shape = RoundedCornerShape(16.dp),
) {
    CenterRow(
        modifier = modifier
            .heightIn(56.dp)
            .clip(shape)
            .clickable(enabled) { onClick() }
            .background(if (activated) Color(0xFFF3EFFF) else Color.White)
            .border(1.5.dp, if (activated) PrimaryColor else BorderColor, shape)
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        Text(
            text = title,
            style = if (activated) medium16 else regular16,
            color = if (activated) PrimaryColor else Color.Black
        )
    }
}

@Composable
fun SimpleOutlineButtonWithIcon(
    title: String,
    @DrawableRes iconId: Int,
    modifier: Modifier,
    onClick: () -> Unit,
    activated: Boolean = false,
    iconSize: Dp = 24.dp,
    rounded: Dp = 16.dp
) {
    CenterRow(
        modifier = modifier
            .heightIn(56.dp)
            .rounded(rounded)
            .clickable { onClick() }
            .background(if (activated) PrimaryYellowLightActive else Color.White)
            .border(
                1.5.dp,
                if (activated) PrimaryYellowLightPrimary else DarkNeutralTitle,
                RoundedCornerShape(rounded)
            )
            .padding(vertical = 16.dp, horizontal = 16.dp),
        itemSpacing = 10.dp
    ) {
        Icon(
            painter = painterResource(iconId),
            contentDescription = title,
            modifier = Modifier.size(iconSize),
            tint = if (activated) DarkNeutralTitle else DarkNeutralTitle
        )

        Text(
            text = title,
            style = if (activated) bold16 else bold16,
            color = if (activated) DarkNeutralTitle else DarkNeutralTitle
        )
    }
}

@Composable
fun SimpleGradientOutlineButton(
    title: String,
    modifier: Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    CenterRow(
        modifier = modifier
            .heightIn(56.dp)
            .rounded(16.dp)
            .clickable(enabled) { onClick() }
            .background(Color.White)
            .border(1.5.dp, brush = SignatureGradient, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        Text(
            text = title,
            style = appTextStyle(size = 18, 400, brush = SignatureGradient),
        )
    }
}


@Preview
@Composable
private fun Preview() {
    Column {
        SimpleOutlineButton(
            title = "Hello World!",
            modifier = Modifier.fillMaxWidth(),
            onClick = {})
        SimpleOutlineButtonWithIcon(
            title = "Hello World!",
            iconId = R.drawable.ic_language_vietnamese,
            modifier = Modifier.fillMaxWidth(),
            onClick = {}
        )
        SimpleOutlineButtonWithIcon(
            title = "Hello World!",
            iconId = R.drawable.ic_language_vietnamese,
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
            activated = true,
        )

        SimpleGradientOutlineButton(
            title = "Hello World!",
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
        )

    }
}
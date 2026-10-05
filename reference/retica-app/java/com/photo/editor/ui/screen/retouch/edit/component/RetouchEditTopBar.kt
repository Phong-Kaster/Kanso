package com.photo.editor.ui.screen.retouch.edit.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.medium12
import com.photo.editor.common.ui.theme.regular18
import com.photo.editor.ui.component.topbar.BaseTopBar
import com.photo.editor.ui.component.topbar.CommonTopBar
import com.photo.editor.ui.screen.retouch.common.RetouchEditInteraction

@Composable
fun RetouchEditTopBar(interaction: RetouchEditInteraction, maximized: Boolean) {
    AnimatedVisibility(
        visible = !maximized,
        enter = slideInVertically() + expandVertically() + fadeIn(),
        exit = slideOutVertically() + shrinkVertically() + fadeOut()
    ) {
        CommonTopBar(
            modifier = Modifier.background(Color.White),
            title = stringResource(R.string.retouch),
            onBackClick = { interaction.onBackClick() },
            rightContent = {
                CenterColumn(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 16.dp)
                        .rounded(10.dp)
                        .clickable { interaction.onSaveClick() }

                        .padding(vertical = 4.dp, horizontal = 10.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_save),
                        contentDescription = stringResource(R.string.save),
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )

                    Text(
                        text = stringResource(R.string.save), style = medium12, color = Color.Black
                    )
                }
            },
            positionTitle = Alignment.CenterStart,
            textTitleStyle = regular18
        )
    }

    AnimatedVisibility(
        visible = maximized,
        enter = slideInVertically() + expandVertically() + fadeIn(),
        exit = slideOutVertically() + shrinkVertically() + fadeOut(),
    ) {
        BaseTopBar { }
    }
}

@Preview
@Composable
private fun Preview() {
    RetouchEditTopBar(interaction = RetouchEditInteraction(), maximized = false)
}
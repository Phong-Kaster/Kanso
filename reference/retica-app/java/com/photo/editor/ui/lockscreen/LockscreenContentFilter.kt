package com.photo.editor.ui.lockscreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.modifier.silentClickable
import com.photo.editor.common.ui.theme.PetitFormalScriptFontFamily
import com.photo.editor.common.ui.theme.appTextStyle
import com.photo.editor.common.ui.theme.bold14
import com.photo.editor.common.ui.theme.bold16
import com.photo.editor.common.ui.theme.regular10
import com.photo.editor.common.ui.theme.regular14
import com.photo.editor.common.util.getAssetAbsoluteFilePath
import com.photo.editor.editimage.model.filter.FilterCategory

@Composable
fun LockscreenContentFilter(
    onContentClick: () -> Unit = {},
    onDismissClick: () -> Unit = {},
    onConfirmClick: () -> Unit = {},
) {
    val filters = remember { FilterCategory.allFilters.shuffled().take(5) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .rounded(10.dp)
            .background(Color.White)
            .silentClickable { onContentClick() }
            .padding(10.dp),
    ) {
        Column {
            CenterRow(
                modifier = Modifier,
                itemSpacing = 10.dp
            ) {
                Image(
                    painter = painterResource(R.drawable.app_icon),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(36.dp)
                        .rounded(10.dp),
                )

                Text(
                    text = stringResource(R.string.app_name),
                    style = appTextStyle(20, 600, Color.Black),
                    fontFamily = PetitFormalScriptFontFamily,
                )
            }

            Spacer(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFF5F5F5))
            )

            Text(
                text = stringResource(R.string.lockscreen_filter_1),
                style = bold14,
                color = Color.Black,
            )

            Text(
                text = stringResource(R.string.lockscreen_filter_2),
                style = regular14,
                color = Color.Black,
                lineHeight = 14.sp,
            )

            Row(
                modifier = Modifier.padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEach { filter ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.7f)
                            .rounded(5.dp)
                    ) {
                        AsyncImage(
                            model = getAssetAbsoluteFilePath(filter.assetPath),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop,
                        )

                        CenterBox(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color(0xFF1F7E81).copy(0.3f))
                                .padding(4.dp)
                        ) {
                            Text(
                                text = stringResource(filter.nameId),
                                style = regular10,
                                color = Color.White,
                                modifier = Modifier.basicMarquee(Int.MAX_VALUE)
                            )
                        }
                    }
                }
            }
        }

        CenterRow(
            modifier = Modifier.fillMaxWidth(),
            itemSpacing = 10.dp
        ) {
            CenterBox(
                modifier = Modifier
                    .height(45.dp)
                    .weight(1f)
                    .rounded(5.dp)
                    .background(Color.Black.copy(0.1f))
                    .clickable { onDismissClick() }
            ) {
                Text(
                    text = stringResource(R.string.later).uppercase(),
                    style = bold14,
                    color = Color(0xFF808080)
                )
            }

            CenterBox(
                modifier = Modifier
                    .weight(1.2f)
                    .height(45.dp)
                    .rounded(5.dp)
                    .clickable { onConfirmClick() }
                    .background(Color.Black)
            ) {
                Text(
                    text = stringResource(R.string.try_now).uppercase(),
                    style = bold14,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    LockscreenContentFilter()
}
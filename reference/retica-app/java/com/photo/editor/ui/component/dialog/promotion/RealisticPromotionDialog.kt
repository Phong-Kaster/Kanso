package com.photo.editor.ui.component.dialog.promotion

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.backgroundColor
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.modifier.silentClickable
import com.photo.editor.common.ui.theme.DarkGreen
import com.photo.editor.common.ui.theme.GoboldHollowFontFamily
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.ui.theme.bold14
import com.photo.editor.common.ui.theme.bold16
import com.photo.editor.common.util.logEvent
import com.photo.editor.common.util.staticStatusBarPadding

@Composable
fun RealisticPromotionDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = showDialog,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        LaunchedEffect(Unit) {
            logEvent("promote_dialog")
        }

        BackHandler {
            onDismissRequest()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .silentClickable { }
                .background(Color.Black.copy(0.7f))
        ) {

            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(0.2f))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .staticStatusBarPadding()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {

                Spacer(modifier = Modifier.weight(1f))

                CenterBox(
                    modifier = Modifier
                        .size(24.dp)
                        .circle()
                        .background(Color.Black.copy(0.7f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .rippleClickable {
                                logEvent("promote_dialog_close_click")
                                onDismissRequest()
                            }
                    )
                }
            }

            CenterColumn(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()

            ) {
                Box {
                    Image(
                        painter = painterResource(R.drawable.promotion_dialog_1_1),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 24.dp, end = 24.dp)
                            .fillMaxWidth()
                            .clip(
                                shape = RoundedCornerShape(
                                    topStart = 10.dp,
                                    topEnd = 10.dp
                                )
                            ),
                        contentScale = ContentScale.FillWidth,
                    )

                        Image(
                            painter = painterResource(R.drawable.promotion_dialog_1_2),
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize(),
                            contentScale = ContentScale.FillWidth,
                        )

                    Column(
                        modifier = Modifier
                            .align(BiasAlignment(-1f, -0.9f))
                            .padding(start = 40.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.promotion_dialog_1_1).uppercase(),
                            lineHeight = 50.sp,
                            fontSize = 40.sp,
                            color = DarkGreen,
                            fontFamily = GoboldHollowFontFamily,
                        )

                        Text(
                            text = stringResource(R.string.promotion_dialog_1_2),
                            style = bold14,
                            color = Color.White,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                CenterRow(
                    modifier = Modifier
                        .height(58.dp)
                        .offset(y = -1.dp)
                        .padding(start = 24.dp, end = 24.dp)
                        .fillMaxWidth()
                        .clip(
                            shape = RoundedCornerShape(
                                bottomStart = 10.dp,
                                bottomEnd = 10.dp
                            )
                        )
                        .background(Color.White)
                        .padding(horizontal = 10.dp),
                    itemSpacing = 5.dp
                ) {
                    CenterBox(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .rounded(5.dp)
                            .clickable {
                                logEvent("promote_dialog_later_click")
                                onDismissRequest()
                            }
                            .backgroundColor(0x0F000000)
                    ) {
                        Text(
                            text = stringResource(R.string.later).uppercase(),
                            style = bold14,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    CenterBox(
                        modifier = Modifier
                            .weight(2f)
                            .height(40.dp)
                            .rounded(5.dp)
                            .clickable {
                                logEvent("promote_dialog_try_click")
                                onConfirm()
                            }
                            .background(Color.Black)
                    ) {
                        Text(
                            text = stringResource(R.string.try_it_out).uppercase(),
                            style = bold16,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                    }
                }
            }

        }
    }
}

@Preview
@Composable
private fun Preview() {
    RealisticPromotionDialog(
        showDialog = true,
        onDismissRequest = {},
        onConfirm = {}
    )
}
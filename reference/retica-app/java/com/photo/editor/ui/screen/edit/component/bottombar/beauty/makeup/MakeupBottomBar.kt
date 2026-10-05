package com.photo.editor.ui.screen.edit.component.bottombar.beauty.makeup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.bold12
import com.photo.editor.common.util.getAssetAbsoluteFilePath
import com.photo.editor.editimage.model.makeup.BeautyMark
import com.photo.editor.editimage.model.makeup.Blush
import com.photo.editor.editimage.model.makeup.EyeLens
import com.photo.editor.editimage.model.makeup.Eyebrows
import com.photo.editor.editimage.model.makeup.Eyelashes
import com.photo.editor.editimage.model.makeup.Eyeliner
import com.photo.editor.editimage.model.makeup.Eyeshadow
import com.photo.editor.editimage.model.makeup.Lipstick
import com.photo.editor.ui.screen.edit.common.EditImageBeautyState
import com.photo.editor.ui.screen.edit.enums.EditImageMakeupOption

@Composable
fun MakeupBottomBar(
    beautyState: EditImageBeautyState,
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val makeupData by beautyState.makeupDataFlow.collectAsState()
    var selectedOption: EditImageMakeupOption by rememberSaveable { mutableStateOf(EditImageMakeupOption.Lipstick) }

    BackHandler { onCancelClick() }

    Column(modifier = modifier) {
        AnimatedContent(
            targetState = selectedOption,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.fillMaxWidth(),
        ) { option ->
            when (option) {
                EditImageMakeupOption.Lipstick -> {
                    MakeupSelector(
                        items = Lipstick.entries,
                        itemName = { lipstick -> stringResource(lipstick.titleId) },
                        selectedItem = makeupData.lipstick,
                        intensity = makeupData.lipstickIntensity,
                        onItemUpdate = { lipstick, intensity ->
                            beautyState.setLipstick(lipstick, intensity)
                        },
                        itemImage = {
                            Image(
                                painter = painterResource(R.drawable.lipstick_sample),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.tint(Color(it.color), blendMode = BlendMode.Color)
                            )
                        }
                    )
                }

                EditImageMakeupOption.Eyelashes -> {
                    MakeupSelector(
                        items = Eyelashes.entries,
                        itemName = { eyelashes -> stringResource(eyelashes.nameId) },
                        selectedItem = makeupData.eyelashes,
                        intensity = makeupData.eyelashesIntensity,
                        onItemUpdate = { eyelashes, intensity ->
                            beautyState.setEyelashes(eyelashes, intensity)
                        },
                        itemImage = { eyelashes ->
                            AsyncImage(
                                model = getAssetAbsoluteFilePath(eyelashes.previewAssetPath),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    )
                }

                EditImageMakeupOption.Eyebrows -> {
                    MakeupSelector(
                        items = Eyebrows.entries,
                        itemName = { eyebrows -> stringResource(eyebrows.nameId) },
                        selectedItem = makeupData.eyebrows,
                        intensity = makeupData.eyebrowsIntensity,
                        onItemUpdate = { eyebrows, intensity ->
                            beautyState.setEyebrows(eyebrows, intensity)
                        },
                        itemImage = { eyebrows ->
                            AsyncImage(
                                model = getAssetAbsoluteFilePath(eyebrows.assetPath),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    )
                }

                EditImageMakeupOption.Blush -> {
                    MakeupSelector(
                        items = Blush.entries,
                        itemName = { blush -> stringResource(blush.nameId) },
                        selectedItem = makeupData.blush,
                        intensity = makeupData.blushIntensity,
                        onItemUpdate = { blush, intensity ->
                            beautyState.setBlush(blush, intensity)
                        },
                        itemImage = { blush ->
                            AsyncImage(
                                model = getAssetAbsoluteFilePath(blush.previewAssetPath),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    )
                }

                EditImageMakeupOption.Eyeshadow -> {
                    MakeupSelector(
                        items = Eyeshadow.entries,
                        itemName = { eyeshadow -> stringResource(eyeshadow.nameId) },
                        selectedItem = makeupData.eyeshadow,
                        intensity = makeupData.eyeshadowIntensity,
                        onItemUpdate = { eyeshadow, intensity ->
                            beautyState.setEyeshadow(eyeshadow, intensity)
                        },
                        itemImage = { eyeshadow ->
                            AsyncImage(
                                model = getAssetAbsoluteFilePath(eyeshadow.assetPath),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    )
                }

                EditImageMakeupOption.Eyeliner -> {
                    MakeupSelector(
                        items = Eyeliner.entries,
                        itemName = { eyeliner -> stringResource(eyeliner.nameId) },
                        selectedItem = makeupData.eyeliner,
                        intensity = makeupData.eyelinerIntensity,
                        onItemUpdate = { eyeliner, intensity ->
                            beautyState.setEyeliner(eyeliner, intensity)
                        },
                        itemImage = { eyeliner ->
                            AsyncImage(
                                model = getAssetAbsoluteFilePath(eyeliner.previewAssetPath),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    )
                }

                EditImageMakeupOption.EyeLen -> {
                    MakeupSelector(
                        items = EyeLens.entries,
                        itemName = { eyeLens -> stringResource(eyeLens.nameId) },
                        selectedItem = makeupData.eyeLens,
                        intensity = makeupData.eyeLensIntensity,
                        onItemUpdate = { eyeLens, intensity ->
                            beautyState.setEyeLens(eyeLens, intensity)
                        },
                        itemImage = { eyeLens ->
                            AsyncImage(
                                model = getAssetAbsoluteFilePath(eyeLens.assetPath),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    )
                }

                EditImageMakeupOption.BeautyMark -> {
                    MakeupSelector(
                        items = BeautyMark.entries,
                        itemName = { freckle -> stringResource(freckle.nameId) },
                        selectedItem = makeupData.beautyMark,
                        intensity = makeupData.freckleIntensity,
                        onItemUpdate = { freckle, intensity ->
                            beautyState.setFreckle(freckle, intensity)
                        },
                        itemImage = { freckle ->
                            AsyncImage(
                                model = getAssetAbsoluteFilePath(freckle.assetPath),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .height(50.dp)
                .background(Color.White),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CenterBox(
                modifier = Modifier
                    .size(50.dp)
                    .rippleClickable(25.dp) {
                        onCancelClick()
                    }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close),
                    tint = Color.Black,
                    modifier = Modifier
                        .size(32.dp),
                )
            }

            LazyRow(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(EditImageMakeupOption.entries) { option ->
                    Column(
                        modifier = Modifier
                            .height(50.dp)
                            .width(IntrinsicSize.Min)
                            .rounded(5.dp)
                            .clickable {
                                selectedOption = option
                            }
                    ) {
                        CenterBox(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = stringResource(option.titleId).uppercase(),
                                style = bold12,
                                color = if (option == selectedOption) Color.Black else Color(0xFF808080),
                                modifier = Modifier.width(IntrinsicSize.Max)
                            )
                        }

                        if (selectedOption == option) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(Color.Black)
                            )
                        }
                    }
                }
            }

            CenterBox(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp, 0.dp))
                    .background(Color(0xFFEBEDF0))
                    .clickable {
                        onConfirmClick()
                    }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.confirm),
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    MakeupBottomBar(
        beautyState = EditImageBeautyState(),
        onConfirmClick = { },
        onCancelClick = { }
    )
}
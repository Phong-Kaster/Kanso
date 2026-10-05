package com.photo.editor.ui.screen.edit.component.bottombar.beauty.makeup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.editimage.model.makeup.Lipstick
import com.photo.editor.ui.screen.edit.component.bottombar.EditImageValueSlider

@Composable
fun <T> MakeupSelector(
    items: List<T>,
    itemName: @Composable (T) -> String,
    selectedItem: T?,
    intensity: Float,
    onItemUpdate: (item: T?, intensity: Float) -> Unit,
    itemImage: @Composable BoxScope.(T) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = selectedItem != null,
        ) {
            EditImageValueSlider(
                value = intensity,
                onValueChange = {
                    onItemUpdate(selectedItem, it)
                },
                valueRange = 0f..1f,
                title = stringResource(R.string.density),
                valueString = "${(intensity * 100).toInt()}",
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 12.dp)
                .padding(start = 8.dp)
        ) {
            MakeupNoneItem(
                onClick = {
                    onItemUpdate(null, 0f)
                },
            )

            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(end = 8.dp),
            ) {
                items(items) { item ->
                    MakeupItemView(
                        image = {
                            itemImage(item)
                        },
                        title = itemName(item),
                        isSelected = selectedItem == item,
                        onClick = {
                            onItemUpdate(item, 0.5f)
                        },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    MakeupSelector(
        items = Lipstick.entries,
        itemName = { lipstick -> stringResource(lipstick.titleId) },
        selectedItem = Lipstick.entries.first(),
        intensity = 0.5f,
        onItemUpdate = { _, _ -> },
        itemImage = {
            Image(
                painter = painterResource(R.drawable.lipstick_sample),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(Color(it.color), blendMode = BlendMode.Color)
            )
        }
    )
}
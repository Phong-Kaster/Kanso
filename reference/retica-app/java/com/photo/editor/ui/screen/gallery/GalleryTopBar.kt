package com.photo.editor.ui.screen.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.DarkNeutralTitle
import com.photo.editor.common.ui.theme.extraBold18
import com.photo.editor.common.ui.theme.medium14
import com.photo.editor.ui.component.BaseDropdownMenu
import com.photo.editor.ui.component.topbar.BaseTopBar
import com.photo.gallery.model.GalleryBucket
import com.photo.gallery.model.SortType

@Composable
fun GalleryTopBar(
    buckets: List<GalleryBucket>,
    selectedBucket: GalleryBucket?,
    onBucketSelected: (GalleryBucket?) -> Unit,
    selectedSortType: SortType,
    onSortTypeSelected: (SortType) -> Unit,
    onBackClick: () -> Unit,
    isSubScreen: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        val content = @Composable {
            var bucketDropdownExpanded by remember { mutableStateOf(false) }
            var sortDropdownExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CenterBox(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(40.dp)
                        .rippleClickable {
                            onBackClick()
                        }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = stringResource(R.string.back),
                        modifier = Modifier.size(24.dp),
                        tint = DarkNeutralTitle,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .rounded(2.dp)
                        .clickable { bucketDropdownExpanded = true }
                        .padding(2.dp),
                ) {
                    Text(
                        text = (selectedBucket?.name ?: stringResource(R.string.all_photos)) + " ▼",
                        style = extraBold18,
                        color = DarkNeutralTitle,
                    )

                    BaseDropdownMenu(
                        expanded = bucketDropdownExpanded,
                        onDismissRequest = { bucketDropdownExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.all_photos), style = medium14, color = Color.Black) },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (selectedBucket == null) Color.Black else Color.Transparent,
                                )
                            },
                            onClick = {
                                onBucketSelected(null)
                                bucketDropdownExpanded = false
                            }
                        )

                        buckets.forEach { bucket ->
                            DropdownMenuItem(
                                text = { Text(text = bucket.name, style = medium14, color = Color.Black) },
                                trailingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (selectedBucket == bucket) Color.Black else Color.Transparent,
                                    )
                                },
                                onClick = {
                                    onBucketSelected(bucket)
                                    bucketDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                CenterBox(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(40.dp)
                        .rippleClickable { sortDropdownExpanded = true }
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_sort),
                        contentDescription = stringResource(R.string.sort),
                        modifier = Modifier.size(24.dp),
                    )

                    BaseDropdownMenu(
                        expanded = sortDropdownExpanded,
                        onDismissRequest = { sortDropdownExpanded = false }
                    ) {
                        SortType.entries.forEach { sortType ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when (sortType) {
                                            SortType.Name -> stringResource(R.string.name)
                                            SortType.LastModified -> stringResource(R.string.last_modified)
                                            SortType.Size -> stringResource(R.string.size)
                                        },
                                        style = medium14,
                                        color = Color.Black
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (sortType == selectedSortType) Color.Black else Color.Transparent,
                                    )
                                },
                                onClick = {
                                    onSortTypeSelected(sortType)
                                    sortDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (isSubScreen) {
            content()
        } else {
            BaseTopBar {
                content()
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    GalleryTopBar(
        buckets = listOf(),
        selectedBucket = null,
        onBucketSelected = {},
        selectedSortType = SortType.LastModified,
        onSortTypeSelected = {},
        onBackClick = {}
    )
}
package com.photo.editor.ui.screen.edit.component.bottombar.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.BaseSlider
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.bold12
import com.photo.editor.common.ui.theme.medium12
import com.photo.editor.common.util.getAssetAbsoluteFilePath
import com.photo.editor.editimage.model.filter.FilterAdjustPreset
import com.photo.editor.editimage.model.filter.FilterCategory
import com.photo.editor.editimage.model.filter.FilterDirectPreset
import com.photo.editor.editimage.model.filter.FilterPreset
import com.photo.editor.ui.component.shimmer.SimpleShimmer
import com.photo.editor.ui.screen.edit.common.EditImageUiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun FilterBottomBar(
    uiState: EditImageUiState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val categoryListState = rememberLazyListState()
    val filterListState = rememberLazyListState()

    val filterData by uiState.filterDataFlow.collectAsState()
    val selectedFilter = filterData.filter
    val filterIntensity = filterData.intensity

    var category by rememberSaveable {
        mutableStateOf(FilterCategory.findCategoryOfFilter(selectedFilter) ?: FilterCategory.entries.first())
    }
    var categoryScrollLock by remember { mutableStateOf(false) }

    // Scroll category when scrolling filter list
    LaunchedEffect(Unit) {
        snapshotFlow { filterListState.firstVisibleItemIndex }.collectLatest {
            if (categoryScrollLock) return@collectLatest
            val indexToConsider = (it + 2).coerceAtMost(FilterCategory.allFilters.size - 1)
            category = FilterCategory.getCategoryByFilterIndex(indexToConsider)
            val categoryIndex = FilterCategory.entries.indexOf(category)
            if (categoryIndex >= 0) {
                coroutineScope.launch {
                    categoryListState.animateScrollToItem((categoryIndex - 1).coerceAtLeast(0))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (selectedFilter != null) {
            filterListState.scrollToItem(FilterCategory.allFilters.indexOf(selectedFilter).coerceAtLeast(0))
        }
    }

    Column(modifier = modifier) {
        AnimatedVisibility(selectedFilter != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(0.7f))
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CenterRow {
                    Text(
                        text = stringResource(R.string.intensity),
                        style = medium12,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = (filterIntensity * 100).toInt().toString(),
                        style = medium12,
                        color = Color.White,
                    )
                }

                BaseSlider(
                    value = filterIntensity,
                    onValueChange = { uiState.setFilterIntensity(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(top = 12.dp)
        ) {
            Row {
                Row(modifier = Modifier.padding(start = 8.dp)) {
                    CenterColumn(
                        modifier = Modifier.width(40.dp),
                        itemSpacing = 4.dp
                    ) {
                        Image(
                            painter = painterResource(R.drawable.empty_slash),
                            contentDescription = null,
                            modifier = Modifier
                                .width(40.dp)
                                .height(60.dp)
                                .rounded(5.dp)
                                .clickable { uiState.setSelectedFilter(null) }
                                .background(AlternativeColor)
                        )

                        Text(
                            text = stringResource(R.string.none),
                            style = medium12,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(Int.MAX_VALUE)
                        )
                    }
                }

                Spacer(
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .height(60.dp)
                        .width(1.dp)
                        .background(Color(0xFFEAEAEA))
                )

                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 10.dp),
                    state = filterListState,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 8.dp),
                ) {
                    FilterCategory.entries.forEach { category ->
                        itemsIndexed(category.presets) { index, filter ->
                            FilterItemCompose(
                                filterPreset = filter,
                                selected = selectedFilter == filter,
                                onClick = {
                                    uiState.setFilterData(filter, 1f)
                                },
                                modifier = Modifier.padding(end = if (index == category.presets.size - 1) 16.dp else 0.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.height(50.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CenterBox(modifier = Modifier
                    .size(50.dp)
                    .rippleClickable(25.dp) {
                        uiState.restoreLastState()
                        uiState.selectedEditOption = null
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
                    state = categoryListState,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(FilterCategory.entries) {
                        Column(
                            modifier = Modifier
                                .height(50.dp)
                                .width(IntrinsicSize.Min)
                                .rounded(5.dp)
                                .clickable {
                                    // Scroll to the first filter of the category
                                    coroutineScope.launch {
                                        val indexToScroll = FilterCategory.allFilters.indexOf(it.presets.first())
                                        if (indexToScroll < 0) return@launch

                                        categoryScrollLock = true
                                        category = it
                                        filterListState.animateScrollToItem(indexToScroll)
                                        categoryScrollLock = false
                                    }
                                }
                        ) {
                            CenterBox(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = stringResource(it.nameId).uppercase(),
                                    style = bold12,
                                    color = if (it == category) Color.Black else Color(0xFF808080),
                                    modifier = Modifier.width(IntrinsicSize.Max)
                                )
                            }

                            if (category == it) {
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
                            uiState.saveState()
                            uiState.selectedEditOption = null
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
}

@Composable
private fun FilterItemCompose(
    filterPreset: FilterPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(60.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(60.dp)
                .rounded(5.dp)
                .clickable { onClick() }
        ) {
            SubcomposeAsyncImage(
                model = getAssetAbsoluteFilePath(filterPreset.assetPath),
                contentDescription = stringResource(filterPreset.nameId),
                loading = { SimpleShimmer() },
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )

            if (selected) {
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .background(AlternativeColor.copy(0.7f))
                )

                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center),
                    tint = Color.Black,
                )
            }
        }

        Text(
            text = stringResource(filterPreset.nameId),
            style = medium12,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(Int.MAX_VALUE)
        )
    }
}

@Preview
@Composable
private fun Preview() {
    FilterBottomBar(
        uiState = EditImageUiState(),
    )
}
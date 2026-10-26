package com.apero.pinity.ui.screen.edit_image.component

import android.annotation.SuppressLint
import android.location.Address
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apero.pinity.data.enums.Decal
import com.apero.pinity.data.enums.DecalCategory
import com.apero.pinity.ui.component.foundation.CenterBox
import com.apero.pinity.ui.component.modifier.circle
import com.apero.pinity.ui.screen.edit_image.decal.component.Date.DecalDateListShow
import com.apero.pinity.ui.screen.edit_image.decal.component.DecalUsingImage
import com.apero.pinity.ui.screen.edit_image.decal.component.Location.DecalLocationListShow
import com.apero.pinity.ui.screen.edit_image.decal.component.Time.DecalTimeListShow
import com.apero.pinity.ui.theme.Gray6
import com.apero.pinity.ui.theme.Green200
import com.apero.pinity.ui.theme.PrimaryColor
import com.apero.pinity.ui.theme.medium16
import com.apero.pinity.ui.theme.regular16
import com.apero.pinity.util.logEvent

@SuppressLint("ResourceType")
@Composable
fun DecalCatalog(
    address: Address? = null,
    modifier: Modifier = Modifier,
    enable: Boolean = true,
    chosenDecal: Decal = Decal.Decal_Food_1,
    onSelectedDecal: (Decal) -> Unit = {}
) {
    if (!enable) return

    val state = rememberLazyListState()
    var currentCategory by remember { mutableStateOf(DecalCategory.Food) }


    LaunchedEffect(
        key1 = chosenDecal,
        block = {
            val list = Decal.entries.filter { currentCategory == it.category } // return a list

            if (list.isEmpty()) return@LaunchedEffect

            val index = list.indexOf(chosenDecal)

            if (index < 0) return@LaunchedEffect
            state.animateScrollToItem(index)
        }
    )

    LaunchedEffect(
        key1 = currentCategory,
        block = { state.animateScrollToItem(0) }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        /** Decal title */
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DecalCategory.entries.forEach { category ->
                CenterBox(
                    modifier = Modifier
                        .circle()
                        .background(
                            if (category == currentCategory) Color(0xFFD5E9C8) else Gray6
                        )
                        .border(
                            width = 0.5.dp,
                            color = if (category == currentCategory) Green200 else Gray6,
                            shape = CircleShape
                        )
                        .clickable {
                            currentCategory = category
                            when (category) {
                                DecalCategory.Weather -> logEvent("stickers_scr_weather_tab_click")
                                DecalCategory.Location -> logEvent("stickers_scr_location_tab_click")
                                DecalCategory.Time -> logEvent("stickers_scr_time_tab_click")
                                DecalCategory.Date -> logEvent("stickers_scr_date_tab_click")
                                DecalCategory.Food -> logEvent("stickers_scr_food_tab_click")
                                DecalCategory.Mood -> logEvent("stickers_scr_mood_tab_click")
                                DecalCategory.Quote -> logEvent("stickers_scr_quote_tab_click")
                                DecalCategory.Emoji -> logEvent("stickers_scr_emoji_tab_click")
                            }
                        }
                        .fillMaxWidth(0.1f)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(category.text),
                        style = if (category == currentCategory) {
                            medium16.copy(PrimaryColor)
                        } else {
                            regular16.copy(Color.Black)
                        },
                        modifier = Modifier,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }

            }
        }

        /** Decal items */
        LazyRow(
            state = state,
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            items(
                items = Decal.entries.filter { currentCategory == it.category },
                itemContent = { it ->
                    // List Decal
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        when (currentCategory) {
                            DecalCategory.Food,
                            DecalCategory.Mood,
                            DecalCategory.Quote,
                            DecalCategory.Emoji,
                            DecalCategory.Weather -> {
                                DecalUsingImage(
                                    decal = it,
                                    decalSelected = chosenDecal,
                                    onSelectedDecal = {
                                        when (it.category) {
                                            DecalCategory.Food -> logEvent("stickers_scr_food_icon_select")
                                            DecalCategory.Mood -> logEvent("stickers_scr_mood_icon_select")
                                            DecalCategory.Quote -> logEvent("stickers_scr_quote_icon_select")
                                            DecalCategory.Emoji -> logEvent("stickers_scr_emoji_icon_select")
                                            DecalCategory.Weather -> logEvent("stickers_scr_weather_icon_select")
                                            else -> {}
                                        }
                                        onSelectedDecal(it)
                                    }
                                )
                            }

                            DecalCategory.Location -> {
                                DecalLocationListShow(
                                    decal = it,
                                    address = address,
                                    decalSelected = chosenDecal,
                                    onSelectedDecal = {
                                        logEvent("stickers_scr_location_icon_select")
                                        onSelectedDecal(it)
                                    }
                                )
                            }

                            DecalCategory.Time -> {
                                DecalTimeListShow(
                                    decal = it,
                                    decalSelected = chosenDecal,
                                    onSelectedDecal = {
                                        logEvent("stickers_scr_time_icon_select")
                                        onSelectedDecal(it)
                                    }
                                )
                            }

                            DecalCategory.Date -> {
                                DecalDateListShow(
                                    decal = it,
                                    decalSelected = chosenDecal,
                                    onSelectedDecal = onSelectedDecal
                                )
                            }
                        }
                    }
                }
            )
            item { Spacer(modifier = Modifier.height(4.dp)) }
        }
    }

}

@Preview
@Composable
private fun PreviewTemplateCatalog() {
    DecalCatalog(
        modifier = Modifier.background(color = Color.White),
        chosenDecal = Decal.Decal_Food_5,
    )
}
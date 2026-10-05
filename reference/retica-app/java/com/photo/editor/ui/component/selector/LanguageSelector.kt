package com.photo.editor.ui.component.selector

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.domain.model.Language
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.BorderStroke
import com.photo.editor.common.ui.theme.PrimaryColor
import com.photo.editor.common.ui.theme.PrimaryYellowLightHover
import com.photo.editor.common.ui.theme.regular16
import java.util.Locale

@Composable
fun LanguageSelector(
    languages: List<Language>,
    selectedLanguage: Language?,
    onSelectLanguage: (Language) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = contentPadding
    ) {
        items(languages, key = { language -> language.code }) { language ->
            LanguageItem(
                language = language,
                selected = language == selectedLanguage,
                onClick = { onSelectLanguage(language) },
            )
        }
    }
}

@Composable
private fun LanguageItem(
    language: Language,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    CenterRow(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .rounded(10.dp)
            .background(if (selected) AlternativeColor else Color(0xFFFEFEFE))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        itemSpacing = 16.dp
    ) {
        Image(
            painterResource(language.drawableId),
            contentDescription = stringResource(R.string.language),
            modifier = Modifier
                .size(28.dp)
                .circle(),
            contentScale = ContentScale.Fit,
        )

        Text(
            text = Locale.forLanguageTag(language.code).displayName,
            style = regular16,
            color = Color.Black,
            modifier = Modifier.weight(1f),
        )

        Image(
            painter = if (selected) painterResource(R.drawable.radio_selected)
            else painterResource(R.drawable.radio_unselected),
            contentDescription = stringResource(R.string.radio_button),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Preview
@Composable
private fun Preview() {
    LanguageSelector(
        languages = Language.entries,
        selectedLanguage = Language.English,
        onSelectLanguage = {})
}

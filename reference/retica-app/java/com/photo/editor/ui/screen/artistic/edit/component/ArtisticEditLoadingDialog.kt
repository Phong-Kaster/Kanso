package com.photo.editor.ui.screen.artistic.edit.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.silentClickable
import com.photo.editor.common.ui.theme.bold14
import com.photo.editor.common.ui.theme.regular12


@Composable
fun ArtisticEditLoadingDialog(){

    CenterColumn(
        modifier = Modifier
            .fillMaxSize()
            .silentClickable { }
            .background(Color.Black.copy(0.8f)),
    ) {
        Image(
            painter = painterResource(R.drawable.artistic_loading),
            contentDescription = stringResource(R.string.loading),
            modifier = Modifier.width(48.dp).aspectRatio(48 / 52f),
            contentScale = ContentScale.Crop
        )

        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = stringResource(R.string.ai_loading_title),
            style = bold14,
            color = Color.White,
        )

        Text(
            modifier = Modifier
                .widthIn(max = 200.dp)
                .padding(top = 4.dp),
            text = stringResource(R.string.ai_loading_subtitle),
            style = regular12,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}
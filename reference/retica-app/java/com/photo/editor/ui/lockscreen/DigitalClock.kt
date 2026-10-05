package com.photo.editor.ui.lockscreen

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.tooling.preview.Preview
import com.photo.editor.common.ui.theme.appTextStyle
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd")

@Composable
fun DigitalClock(modifier: Modifier = Modifier) {
    var time by remember { mutableStateOf(LocalDateTime.now()) }
    val clock = time.format(TimeFormatter)
    val date = time.format(DateFormatter)

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            time = LocalDateTime.now()
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = clock,
            style = appTextStyle(100, 600, Color.White),
        )
        Text(
            text = date,
            style = appTextStyle(20, 400, Color.White),
        )
    }
}

@Preview
@Composable
private fun Preview() {
    DigitalClock()
}
package com.phongkaster.kanso.ui.tool.components.photocard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.TOOL_PROGRESS_TAG

/**
 * The progress ring in the middle of the photo card while an edit runs, filled to [progress]
 * (0 = just started, 1 = done).
 *
 * It is a determinate `CircularProgressIndicator`: given a value, it draws that share of the ring
 * and does not move by itself, so it never keeps a test or a preview waiting. The value the edit
 * reports jumps in steps; `animateFloatAsState` turns each jump into a short smooth glide from the
 * old value to the new one, so the ring fills steadily. It sits on a small round surface in the
 * theme's `surfaceContainerHigh` colour, so it stays visible over any photo.
 *
 * It shows no text; its content description, which screen readers say, tells that the photo is
 * being edited, and the indicator itself reports how far along it is.
 */
@Composable
internal fun EditProgress(progress: Float) {
    val description = stringResource(R.string.tool_progress_description)
    // The animated value follows `progress`, gliding to each new one instead of jumping.
    val shown by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "edit progress",
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = CircleShape,
    ) {
        CircularProgressIndicator(
            // A lambda, so only the drawing is redone as the value glides, not the layout.
            progress = { shown },
            modifier = Modifier
                .padding(12.dp)
                .size(48.dp)
                .testTag(TOOL_PROGRESS_TAG)
                .semantics { contentDescription = description },
        )
    }
}

/** The progress ring in light and dark, a little over half full. */
@PreviewLightDark
@Composable
private fun EditProgressPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        EditProgress(progress = 0.6f)
    }
}

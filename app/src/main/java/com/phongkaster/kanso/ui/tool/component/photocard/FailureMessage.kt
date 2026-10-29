package com.phongkaster.kanso.ui.tool.component.photocard

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.TOOL_FAILURE_TAG

/**
 * Returns the plain-words message for [failure]: what went wrong, and that the photo is
 * unchanged. The `when` has no `else` on purpose: a new kind of [EditFailure] would not compile
 * until it is given a message here.
 */
internal fun failureMessageRes(failure: EditFailure): Int = when (failure) {
    EditFailure.OutOfMemory -> R.string.tool_failed_out_of_memory
    EditFailure.NoSubject -> R.string.tool_failed_no_subject
    EditFailure.NoFace -> R.string.tool_failed_no_face
    EditFailure.NoPerson -> R.string.tool_failed_no_person
    EditFailure.ModelUnavailable -> R.string.tool_failed_model_unavailable
    EditFailure.Other -> R.string.tool_failed_other
}

/**
 * Shows [text], why the last edit did not work, on a card in the theme's error-container colours
 * at the top of the photo card. A tap on it calls [onDismiss], which hides it; the next edit
 * replaces it too. Screen readers announce it as soon as it appears.
 */
@Composable
internal fun FailureMessage(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    // Surface with onClick is a tappable card: it gets press feedback and a button's semantics.
    Surface(
        onClick = onDismiss,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .padding(16.dp)
            .testTag(TOOL_FAILURE_TAG)
            // A polite live region: TalkBack reads the message without interrupting itself.
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/** The failure message in light and dark, with the "out of memory" text. */
@PreviewLightDark
@Composable
private fun FailureMessagePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        FailureMessage(
            text = stringResource(failureMessageRes(EditFailure.OutOfMemory)),
            onDismiss = {},
        )
    }
}

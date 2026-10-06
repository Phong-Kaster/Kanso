package com.phongkaster.kanso.ui.placeholder

import com.phongkaster.kanso.ui.common.BackArrowIcon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R

/** The test tag on the root of the placeholder screen, so tests can look inside it. */
const val PLACEHOLDER_SCREEN_TAG = "placeholder-screen"

/**
 * The stand-in screen for a tool that is not built yet: a top bar titled with the tool's name,
 * a back arrow, and a short note that the tool is coming.
 *
 * [onBack] is called when the user taps the back arrow; the caller pops the back stack.
 * The [viewModel] is passed in by the navigation entry, which owns it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    viewModel: PlaceholderViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toolName = stringResource(viewModel.tool.labelRes)
    // Scaffold lays out the standard Material 3 screen slots: here a top bar and the content.
    Scaffold(
        modifier = modifier.testTag(PLACEHOLDER_SCREEN_TAG),
        topBar = {
            TopAppBar(
                title = { Text(toolName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = BackArrowIcon,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        // innerPadding is the space the top bar (and system bars) take; the content avoids it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.placeholder_message, toolName),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

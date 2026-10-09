package com.phongkaster.kanso.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher

/**
 * Any number followed by a percent sign, such as "100%" or "42%". It is internal, like the
 * matcher below that uses it, so it is visible to the whole test module and not only to this file.
 */
internal val PERCENTAGE = Regex("""\d+%""")

/**
 * Matches a node whose own text reads like a percentage or contains "100%". It is shared by
 * [PickerNoAlphaTest] (no percentage anywhere) and [PickerDefaultAlphaTest] (a percentage in the
 * default picker only), so it lives in this file and is internal.
 */
internal fun hasPercentageText(): SemanticsMatcher =
    SemanticsMatcher("has text with a percentage") { node ->
        node.config.getOrNull(SemanticsProperties.Text).orEmpty()
            .any { PERCENTAGE.containsMatchIn(it.text) || it.text.contains("100%") }
    }

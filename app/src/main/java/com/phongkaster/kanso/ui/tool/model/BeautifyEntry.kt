package com.phongkaster.kanso.ui.tool.model

/**
 * One entry of the Beautify panel's top row (layer 1): either a [Group] of tools, which opens its
 * own row, or a [Single] tool, which opens its slider at once.
 *
 * It is a sealed interface: a `when` over an entry must handle both cases, so the row cannot
 * forget one.
 */
sealed interface BeautifyEntry {

    /** A group, such as Face: tapping it shows the group's tools and a Back entry. */
    data class Group(val group: BeautifyGroup) : BeautifyEntry

    /** A tool of its own, such as Smooth: tapping it opens its slider. */
    data class Single(val tool: BeautifyTool) : BeautifyEntry

    /** The top row's entries. */
    companion object {

        /**
         * The top row, in order: Face, Body, then Smooth, as Meitu's Face | Body | Smooth.
         */
        val TOP_ROW: List<BeautifyEntry> = listOf(Group(BeautifyGroup.FACE), Group(BeautifyGroup.BODY), Single(BeautifyTool.SMOOTH))
    }
}

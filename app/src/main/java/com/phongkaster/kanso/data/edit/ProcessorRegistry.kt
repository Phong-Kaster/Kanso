package com.phongkaster.kanso.data.edit

import com.phongkaster.kanso.navigation.Tool

/**
 * Knows which [ToolProcessor] does each tool's default edit. The app builds one in its Koin
 * module and hands it to the tool screen's ViewModel, so the ViewModel never builds an engine
 * itself, and a test can pass a registry holding fake processors.
 *
 * A tool with no entry has no processor built yet; [processorFor] then returns null and the
 * screen simply has nothing to run.
 */
class ProcessorRegistry(private val processors: Map<Tool, ToolProcessor> = emptyMap()) {

    /** Returns the processor registered for [tool], or null when that tool has none yet. */
    fun processorFor(tool: Tool): ToolProcessor? = processors[tool]
}

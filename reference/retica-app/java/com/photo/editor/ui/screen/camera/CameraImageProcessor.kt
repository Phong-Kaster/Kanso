package com.photo.editor.ui.screen.camera

import android.content.Context
import com.photo.editor.editimage.model.filter.FilterData
import com.photo.editor.editimage.opengl.filter.CompositeFilterProgram
import com.photoeditor.opengl.BaseImageProcessor

class CameraImageProcessor: BaseImageProcessor() {
    private lateinit var filterProgram: CompositeFilterProgram

    override fun initGlPrograms(context: Context) {
        filterProgram = CompositeFilterProgram(context)
        glImageProcessor.setShaderPrograms(
            listOf(filterProgram)
        )
    }

    fun setFilterData(filterData: FilterData) {
        if (!programsInitialized) return

        glImageProcessor.taskExecutor.invoke {
            filterProgram.setData(filterData)
        }
    }
}
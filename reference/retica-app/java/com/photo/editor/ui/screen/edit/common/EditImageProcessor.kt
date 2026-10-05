package com.photo.editor.ui.screen.edit.common

import android.content.Context
import com.photo.editor.common.geometry.UnifyPoint
import com.photo.editor.common.geometry.UnifySize
import com.photo.editor.editimage.model.EditImageData
import com.photo.editor.editimage.model.FaceLandmark
import com.photo.editor.editimage.opengl.CropTransformProgram
import com.photo.editor.editimage.opengl.adjust.CompositeAdjustProgram
import com.photo.editor.editimage.opengl.beauty.CompositeBeautyProgram
import com.photo.editor.editimage.opengl.filter.CompositeFilterProgram
import com.photo.editor.editimage.opengl.frame.CompositeFrameProgram
import com.photo.editor.editimage.opengl.makeup.CompositeMakeupProgram
import com.photo.editor.editimage.opengl.reshape.CompositeReshapeProgram
import com.photoeditor.opengl.BaseImageProcessor

class EditImageProcessor: BaseImageProcessor() {
    override val clearColor = listOf(0.941f, 0.941f, 0.941f)

    private lateinit var cropTransformProgram: CropTransformProgram
    private lateinit var filterProgram: CompositeFilterProgram
    private lateinit var adjustProgram: CompositeAdjustProgram
    private lateinit var beautyProgram: CompositeBeautyProgram
    private lateinit var makeupProgram: CompositeMakeupProgram
    private lateinit var frameProgram: CompositeFrameProgram
    private lateinit var reshapeProgram: CompositeReshapeProgram

    private var currentEditData = EditImageData()

    override fun initGlPrograms(context: Context) {
        cropTransformProgram = CropTransformProgram(context)
        filterProgram = CompositeFilterProgram(context)
        adjustProgram = CompositeAdjustProgram(context)
        beautyProgram = CompositeBeautyProgram(context)
        makeupProgram = CompositeMakeupProgram(context)
        frameProgram = CompositeFrameProgram(context)
        reshapeProgram = CompositeReshapeProgram(context)

        glImageProcessor.setShaderPrograms(
            listOf(
                cropTransformProgram,
                filterProgram,
                adjustProgram,
                beautyProgram,
                makeupProgram,
                frameProgram,
                reshapeProgram,
            )
        )
    }

    fun setEditData(
        faceLandmarks: List<FaceLandmark>,
        editImageData: EditImageData
    ) {
        if (!programsInitialized) return

        glImageProcessor.taskExecutor.invoke {
            val cropData = editImageData.crop
            val filterData = editImageData.filter
            val adjustData = editImageData.adjust
            val colorData = editImageData.color
            val beautyData = editImageData.beauty
            val makeupData = editImageData.makeup
            val reshapeData = editImageData.reshape

            fun logTime(name: String, block: () -> Unit) {
                val start = System.nanoTime()
                block()
                val end = System.nanoTime()
//                println("$name Time taken: ${end - start} ns")
            }

            logTime("Crop") {
                if (currentEditData.crop != cropData) {
                    cropTransformProgram.setData(cropData)
                }
            }
            logTime("Filter"){
                if (currentEditData.filter != filterData) {
                    filterProgram.setData(filterData)
                }
            }
            logTime("Adjust"){
                if (currentEditData.adjust != adjustData || currentEditData.color != colorData) {
                    adjustProgram.setData(adjustData, colorData)
                }
            }
            logTime("Beauty"){
                if (currentEditData.beauty != beautyData) {
                    beautyProgram.setData(faceLandmarks, beautyData)
                }
            }
            logTime("Makeup"){
                if (currentEditData.makeup != makeupData) {
                    makeupProgram.setData(faceLandmarks, makeupData)
                }
            }
            logTime("Frame"){
                if (currentEditData.frame != editImageData.frame) {
                    frameProgram.setFrame(editImageData.frame)
                }
            }
            logTime("Reshape"){
                if (currentEditData.reshape != reshapeData) {
                    reshapeProgram.setData(reshapeData)
                }
            }

            currentEditData = editImageData
        }
    }
}
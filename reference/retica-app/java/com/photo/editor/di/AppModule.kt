package com.photo.editor.di

import com.photo.editor.localml.enhance.EnhanceImageUC
import com.photo.editor.localml.facelandmark.DetectFaceLandmarkUC
import com.photo.editor.localml.posedecection.PoseDetectionUC
import com.photo.editor.localml.removebackground.ForegroundToMaskUC
import com.photo.editor.localml.removebackground.RemoveBackgroundUC
import com.photo.editor.localml.removebackground.SelfieSegmentationUC
import com.photo.editor.localml.removebackground.SubjectSegmentationUC
import com.photo.editor.localml.removeobject.InteractiveSegmentationUC
import com.photo.editor.localml.removeobject.RemoveObjectUC
import com.photo.editor.localml.styletransfer.StylePredictInceptionV3UC
import com.photo.editor.localml.styletransfer.StyleTransferInceptionV3BlendUC
import com.photo.editor.localml.styletransfer.StyleTransferInceptionV3UC
import org.koin.dsl.module

val localMlModule = module {
    factory { DetectFaceLandmarkUC(get()) }
    factory { PoseDetectionUC() }
    factory { RemoveBackgroundUC(get(), get()) }
    factory { ForegroundToMaskUC() }
    factory { SubjectSegmentationUC() }
    factory { SelfieSegmentationUC() }
    factory { RemoveObjectUC(get()) }
    factory { InteractiveSegmentationUC(get()) }
    factory { StyleTransferInceptionV3BlendUC(get(), get()) }
    factory { StylePredictInceptionV3UC(get()) }
    factory { StylePredictInceptionV3UC(get()) }
    factory { StyleTransferInceptionV3UC(get(), get()) }
    factory { EnhanceImageUC(get()) }
}
package com.photo.editor.domain.model

sealed class ProgressStatus<T> {
    class None<T> : ProgressStatus<T>()
    data class Processing<T>(val progress: Float) : ProgressStatus<T>()
    data class Error<T>(val message: String) : ProgressStatus<T>()
    data class Success<T>(val result: T) : ProgressStatus<T>()
}
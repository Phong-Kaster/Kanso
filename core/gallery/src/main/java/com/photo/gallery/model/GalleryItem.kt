package com.photo.gallery.model

import android.net.Uri

open class GalleryItem(
    open val id: Long,
    open val uri: Uri,
    open val path: String,
    open val name: String,
    open val modifiedTime: Long,
    open val width: Long,
    open val height: Long,
    open val size: Long,
    open val bucketId: Long,
    open val bucketName: String,
)
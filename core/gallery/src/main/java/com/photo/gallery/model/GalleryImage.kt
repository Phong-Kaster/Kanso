package com.photo.gallery.model

import android.net.Uri

data class GalleryImage(
    override val id: Long,
    override val uri: Uri,
    override val path: String,
    override val name: String,
    override val modifiedTime: Long,
    override val width: Long,
    override val height: Long,
    override val size: Long,
    override val bucketId: Long,
    override val bucketName: String,
) : GalleryItem(
    id = id,
    uri = uri,
    path = path,
    name = name,
    modifiedTime = modifiedTime,
    width = width,
    height = height,
    size = size,
    bucketId = bucketId,
    bucketName = bucketName
)
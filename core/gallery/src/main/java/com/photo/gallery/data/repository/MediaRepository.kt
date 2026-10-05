package com.photo.gallery.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.photo.gallery.model.GalleryBucket
import com.photo.gallery.model.GalleryImage
import com.photo.gallery.model.GalleryItem
import com.photo.gallery.model.GalleryVideo

class MediaRepository {
    private val imageProjection = listOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_MODIFIED,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
    )

    private val videoProjection = listOf(
        MediaStore.Video.Media._ID,
        MediaStore.Images.Media.DATA,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DATE_MODIFIED,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT,
        MediaStore.Video.Media.BUCKET_ID,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
    )

    private fun cursorToImages(cursor: Cursor): List<GalleryImage> {
        val result = mutableListOf<GalleryImage>()

        val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
        val pathColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
        val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
        val dateColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
        val sizeColumn = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
        val widthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
        val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

        while (cursor.moveToNext()) {
            try {
                val id = cursor.getLong(idColumn)
                val path = cursor.getString(pathColumn)
                val name = cursor.getString(nameColumn)
                val date = cursor.getLong(dateColumn)
                val size = cursor.getLong(sizeColumn)
                val width = cursor.getLong(widthColumn)
                val height = cursor.getLong(heightColumn)
                val bucketId = cursor.getLong(bucketIdColumn)
                var bucketName = cursor.getString(bucketNameColumn)

                val contentUri: Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                if (bucketName == null) bucketName = "Storage"

                result += GalleryImage(id, contentUri, path, name, date, width, height, size, bucketId, bucketName)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        return result
    }

    private fun cursorToVideos(cursor: Cursor): List<GalleryVideo> {
        val result = mutableListOf<GalleryVideo>()

        val idColumn = cursor.getColumnIndex(MediaStore.Video.Media._ID)
        val pathColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
        val nameColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
        val dateColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
        val sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
        val widthColumn = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
        val heightColumn = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)

        while (cursor.moveToNext()) {
            try {
                val id = cursor.getLong(idColumn)
                val path = cursor.getString(pathColumn)
                val name = cursor.getString(nameColumn)
                val date = cursor.getLong(dateColumn)
                val size = cursor.getLong(sizeColumn)
                val width = cursor.getLong(widthColumn)
                val height = cursor.getLong(heightColumn)
                val bucketId = cursor.getLong(bucketIdColumn)
                var bucketName = cursor.getString(bucketNameColumn)
                val duration = cursor.getLong(durationColumn)
                val contentUri: Uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                if (bucketName == null) bucketName = "Storage"

                result += GalleryVideo(id, contentUri, path, name, date, width, height, size, bucketId, bucketName, duration)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        return result
    }

    fun getVersion(context: Context): String {
        return MediaStore.getVersion(context)
    }

    fun getAllBuckets(context: Context): List<GalleryBucket> {
        val belowQ = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        val buckets = mutableSetOf<GalleryBucket>()
        val imageCollection = if (belowQ) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val videoCollection = if (belowQ) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val imageBucketProjection = listOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        val videoBucketProjection = listOf(
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        )
        val selection = null
        val selectionArgs = null
        val sortOrder = null
        // Query for image buckets
        val imageQuery = context.contentResolver.query(
            imageCollection, imageBucketProjection.toTypedArray(), selection, selectionArgs, sortOrder,
        )
        imageQuery?.use { cursor ->
            val bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                try {
                    val bucketId = cursor.getLong(bucketIdColumn)
                    var bucketName = cursor.getString(bucketNameColumn)
                    if (bucketName == null) bucketName = "Storage"

                    buckets.add(GalleryBucket(bucketId, bucketName))
                } catch (_: Exception) {
                }
            }
        }

        // Query for video buckets
        val videoQuery = context.contentResolver.query(
            videoCollection, videoBucketProjection.toTypedArray(), selection, selectionArgs, sortOrder,
        )
        videoQuery?.use { cursor ->
            val bucketIdColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val bucketId = cursor.getLong(bucketIdColumn)
                var bucketName = cursor.getString(bucketNameColumn)
                if (bucketName == null) bucketName = "Storage"

                buckets.add(GalleryBucket(bucketId, bucketName))
            }
        }

        return buckets.toList().sortedBy { it.name }
    }

    fun getImageBuckets(context: Context): List<GalleryBucket> {
        val belowQ = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        val buckets = mutableSetOf<GalleryBucket>()
        val imageCollection = if (belowQ) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val imageBucketProjection = listOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )
        val selection = null
        val selectionArgs = null
        val sortOrder = null
        // Query for image buckets
        val imageQuery = context.contentResolver.query(
            imageCollection, imageBucketProjection.toTypedArray(), selection, selectionArgs, sortOrder,
        )

        imageQuery?.use { cursor ->
            val bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                try {
                    val bucketId = cursor.getLong(bucketIdColumn)
                    var bucketName = cursor.getString(bucketNameColumn)
                    if (bucketName == null) bucketName = "Storage"

                    buckets.add(GalleryBucket(bucketId, bucketName))
                } catch (_: Exception) {
                }
            }
        }

        return buckets.toList().sortedBy { it.name }
    }

    fun getBucketImages(context: Context, bucket: GalleryBucket?): List<GalleryImage> {
        val belowQ = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        val images = mutableListOf<GalleryImage>()
        val collection = if (belowQ) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val selection = if (bucket != null) "${MediaStore.Images.Media.BUCKET_ID} = ?" else null
        val selectionArgs = if (bucket != null) arrayOf(bucket.id.toString()) else null
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        val query = context.contentResolver.query(
            collection, imageProjection.toTypedArray(), selection, selectionArgs, sortOrder
        )
        query?.use { cursor ->
            images += cursorToImages(cursor)
        }
        return images.toList()
    }

    fun getAllImages(context: Context): List<GalleryImage> {
        return getBucketImages(context, null)
    }

    fun getBucketVideos(context: Context, bucket: GalleryBucket?): List<GalleryVideo> {
        val belowQ = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        val videos = mutableListOf<GalleryVideo>()
        val collection = if (belowQ) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val selection = if (bucket != null) "${MediaStore.Video.Media.BUCKET_ID} = ?" else null
        val selectionArgs = if (bucket != null) arrayOf(bucket.id.toString()) else null
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        val query = context.contentResolver.query(
            collection, videoProjection.toTypedArray(), selection, selectionArgs, sortOrder
        )
        query?.use { cursor ->
            videos += cursorToVideos(cursor)
        }
        return videos.toList()
    }

    fun getAllVideos(context: Context): List<GalleryVideo> {
        return getBucketVideos(context, null)
    }

    fun getBucketMedia(context: Context, bucket: GalleryBucket): List<GalleryItem> {
        val images = getBucketImages(context, bucket)
        val videos = getBucketVideos(context, bucket)
        // Merge images and videos list, preserve sorted order
        val result = mutableListOf<GalleryItem>()
        var iIdx = 0
        var vIdx = 0
        while (iIdx < images.size && vIdx < videos.size) {
            if (images[iIdx].modifiedTime >= videos[vIdx].modifiedTime) result += images[iIdx++]
            else result += videos[vIdx++]
        }
        while (iIdx < images.size) result += images[iIdx++]
        while (vIdx < videos.size) result += videos[vIdx++]
        return result
    }
}
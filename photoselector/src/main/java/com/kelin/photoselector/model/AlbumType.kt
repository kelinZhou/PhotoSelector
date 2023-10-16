package com.kelin.photoselector.model

import android.provider.MediaStore.Files.FileColumns

/**
 * **描述:** 相册类型。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/10 10:00 AM
 *
 * **版本:** v 1.0.0
 */
enum class AlbumType(val type: Int, val query: String) {
    /**
     * 照片相册。
     */
    PHOTO(0x01, "${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_IMAGE}"),
    /**
     * 视频相册。
     */
    VIDEO(0x02, "${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_VIDEO}"),
    /**
     * 照片和视频的混合相册。
     */
    PHOTO_VIDEO(0x03, "${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_IMAGE} OR ${FileColumns.MEDIA_TYPE} = ${FileColumns.MEDIA_TYPE_VIDEO}");

    companion object {
        internal fun typeOf(type: Int): AlbumType {
            values().forEach {
                if (type == it.type) {
                    return it
                }
            }
            throw NullPointerException("The AlbumType not found with type:$type !")
        }
    }
}
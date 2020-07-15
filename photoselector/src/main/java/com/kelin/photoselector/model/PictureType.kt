package com.kelin.photoselector.model

/**
 * **描述:** 文件类型，是图片还是视频。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/10 12:14 PM
 *
 * **版本:** v 1.0.0
 */
internal enum class PictureType(val type: Int) {
    /**
     * 图片。
     */
    PHOTO(0x01),
    /**
     * 视频。
     */
    VIDEO(0x02)
}
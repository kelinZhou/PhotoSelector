package com.kelin.photoselector.model

/**
 * **描述:** 相册
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/10 12:57 PM
 *
 * **版本:** v 1.0.0
 */
internal data class Album(
    /**
     * 相册的名字。
     */
    val name: String,
    /**
     * 封面。
     */
    val cover: Picture,
    /**
     * 该相册内所有的图片或视频。
     */
    var pictures: MutableList<Picture>,
    /**
     * 相册的路径。
     */
    val path: String = cover.parentOrNull ?: "/storage/"
)
package com.kelin.photoselector.model

import java.util.regex.Pattern

/**
 * **描述:** Photo的简单实现。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/14 2:05 PM
 *
 * **版本:** v 1.0.0
 */
open class PhotoImpl(
    /**
     * 图片或视频的路径，可以是网络上的url，也可以是本地的path。
     */
    override val uri: String,
    /**
     * 直接指定是否是视频文件，因为isVideo的判断逻辑其实是一种不太严谨的判断方式。
     */
    private val video: Boolean? = null
) : Photo {

    /**
     * 是否是视频文件。
     */
    override val isVideo: Boolean
        get() = video ?: Pattern.compile("\\.(3gp|mp4|flv|avi|rm|rmvb|wmv|ts|webm|mkv)\$").matcher(uri).find()
}
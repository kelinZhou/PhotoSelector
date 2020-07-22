package com.kelin.photoselector.model

import android.content.Context
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.Serializable

/**
 * **描述:** 图片。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/13 6:45 PM
 *
 * **版本:** v 1.0.0
 */
interface Photo : Serializable {
    /**
     * 图片或视频的路径，可以是网络上的url，也可以是本地的path。
     */
    val uri: String
    /**
     * 是否是视频文件。
     */
    val isVideo: Boolean

    /**
     * 图片或视频的Uri，可以是本地的也可以是网络上的。
     * @return 返回有效的Uri，可以返回空，如果返回空将使用url加载视频，建议在7.0及以上的系统中使用FileProvider。
     */
    fun getUri(context: Context): Uri?{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || !isLocalFile) {
            Uri.parse(uri)
        } else {
            Uri.fromFile(File(uri))
        }
    }

    /**
     * 判断是否是本地文件。
     */
    val isLocalFile: Boolean
        get() = !uri.startsWith("http", true)
}
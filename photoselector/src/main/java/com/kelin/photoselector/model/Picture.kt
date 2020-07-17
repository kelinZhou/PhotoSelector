package com.kelin.photoselector.model

import android.content.Context
import android.net.Uri
import android.os.Build
import java.io.File

/**
 * **描述:** 描述手机本地的照片、视频、影片等多媒体文件的属性。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/9 4:23 PM
 *
 * **版本:** v 1.0.0
 */
internal data class Picture(
    /**
     * 文件所在的路径。
     */
    val path: String,
    /**
     * 文件的大小。
     */
    val size: Long,
    /**
     * 类型
     */
    val type: PictureType,
    /**
     * 如果是视频文件的话该字段将是视频文件的可播放时长。
     */
    val duration: String,

    /**
     * 最后修改时间。
     */
    val modifyDate: String
) : Photo {

    val rootDirName: String
        get() = "/${path.split("/").let { if (it.size > 1) it[1] else "storage" }}/"
    /**
     * 父级路径。
     */
    val parent: String
        get() = parentOrNull ?: "UNKNOWN"
    /**
     * 父级路径。
     */
    val parentOrNull: String?
        get() = File(path).parent
    /**
     * 父级路径的名称。
     */
    val parentName: String
        get() = File(path).parentFile?.name ?: "UNKNOWN"
    /**
     * 文件名。
     */
    val name: String
        get() = File(path).name

    override val uri: String
        get() = path

    override val isVideo: Boolean
        get() = type == PictureType.VIDEO

    override fun getUri(context: Context): Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Uri.parse(path)
    } else {
        Uri.fromFile(File(path))
    }


    override fun equals(other: Any?): Boolean {
        return other != null && other is Photo && other.uri == uri
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + duration.hashCode()
        return result
    }
}
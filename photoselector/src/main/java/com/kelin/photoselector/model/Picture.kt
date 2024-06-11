package com.kelin.photoselector.model

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
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
internal data class Picture internal constructor(
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
    val modifyDate: String,
    /**
     * 压缩后的缓存路径。
     */
    private var cachePath: String? = null
) : Photo, Parcelable {

    internal var isComposeFinished = isVideo
        private set


    fun onComposeFinished(newPath: String? = null) {
        isComposeFinished = true
        cachePath = if (newPath.isNullOrEmpty()) null else newPath
    }

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
        get() = cachePath ?: path

    override val isVideo: Boolean
        get() = type == PictureType.VIDEO

    internal constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readLong(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(PictureType::class.java.classLoader, PictureType::class.java)
        } else {
            parcel.readParcelable(PictureType::class.java.classLoader)
        } ?: PictureType.PHOTO,
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString()
    )

    override fun equals(other: Any?): Boolean {
        return other != null && other is Picture && other.name == name && other.type == type && other.modifyDate == modifyDate
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + duration.hashCode()
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(path)
        parcel.writeLong(size)
        parcel.writeParcelable(type, flags)
        parcel.writeString(duration)
        parcel.writeString(modifyDate)
        parcel.writeString(cachePath)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Picture> {
        override fun createFromParcel(parcel: Parcel): Picture {
            return Picture(parcel)
        }

        override fun newArray(size: Int): Array<Picture?> {
            return arrayOfNulls(size)
        }
    }
}
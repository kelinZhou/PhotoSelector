package com.kelin.photoselector.model

import android.os.Parcel
import android.os.Parcelable

/**
 * **描述:** 文件类型，是图片还是视频。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/10 12:14 PM
 *
 * **版本:** v 1.0.0
 */
internal enum class PictureType(val type: Int) : Parcelable {

    /**
     * 图片。
     */
    PHOTO(0x01),

    /**
     * 视频。
     */
    VIDEO(0x02);

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PictureType> {
        override fun createFromParcel(parcel: Parcel): PictureType {
            val type = parcel.readInt()
            entries.forEach {
                if (it.type == type) {
                    return it
                }
            }
            return PHOTO
        }

        override fun newArray(size: Int): Array<PictureType?> {
            return arrayOfNulls(size)
        }
    }
}
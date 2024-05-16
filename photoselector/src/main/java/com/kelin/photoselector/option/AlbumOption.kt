package com.kelin.photoselector.option

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.kelin.okpermission.OkPermission
import com.kelin.photoselector.MutablePhotoCallabck
import com.kelin.photoselector.PhotoSelector
import com.kelin.photoselector.SinglePhotoCallback
import com.kelin.photoselector.model.AlbumType

interface SelectorOption {
    val context: Context
    val album: AlbumType
    val permissions: Array<String>
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
open class SystemAlbumOption internal constructor(
    override val context: Context,
    override var album: AlbumType
) : SelectorOption {
    override val permissions: Array<String>
        get() = when (album) {
            AlbumType.PHOTO -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            AlbumType.VIDEO -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            AlbumType.PHOTO_VIDEO -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        }
}

open class SelectorAlbumOption internal constructor(
    override val context: Context,
    override var album: AlbumType,

    /**
     * selectorId作为去重逻辑的依据，只有自定义相册才支持，系统相册不支持。
     * 该参数只有调用selectAll方法时maxLength大于1才有效。
     */
    var selectorId: Int = PhotoSelector.ID_REPEATABLE,
    /**
     * 限制最大文件大小，单位MB。
     */
    var maxSize: Float = 0F,
    /**
     * 限制视频文件时长，单位：秒；只有在相册支持视频选择时该参数才有意义。
     */
    var maxDuration: Int = 0
) : SelectorOption {
    override val permissions: Array<String>
        get() =  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (album) {
                AlbumType.PHOTO -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                AlbumType.VIDEO -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                AlbumType.PHOTO_VIDEO -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            OkPermission.permission_group.EXTERNAL_STORAGE
        }
}

/**
 * 选择照片或视频。
 * @param callback 选择成功后的回调。
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun SystemAlbumOption.select(callback: SinglePhotoCallback) {
    PhotoSelector.realOpenSelector(context, permissions, true, album, 1, PhotoSelector.ID_SINGLE, 0F, 0, callback)
}

/**
 * 选择照片或视频。
 * @param maxLength 要选择的数量。
 * @param callback 选择成功后的回调。
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun SystemAlbumOption.selectAll(maxLength: Int = PhotoSelector.defMaxLength, callback: MutablePhotoCallabck) {
    PhotoSelector.realOpenSelector(context, permissions, true, album, maxLength, PhotoSelector.ID_REPEATABLE, 0F, 0, callback)
}

/**
 * 选择照片或视频。
 * @param callback 选择成功后的回调。
 */
fun SelectorAlbumOption.select(callback: SinglePhotoCallback) {
    PhotoSelector.realOpenSelector(context, permissions, false, album, 1, PhotoSelector.ID_SINGLE, maxSize, maxDuration, callback)
}

/**
 * 选择照片或视频。
 * @param maxLength 要选择的数量。
 * @param callback 选择成功后的回调。
 */
fun SelectorAlbumOption.selectAll(maxLength: Int = PhotoSelector.defMaxLength, callback: MutablePhotoCallabck) {
    PhotoSelector.realOpenSelector(context, permissions, false, album, maxLength, if (maxLength == 1) PhotoSelector.ID_REPEATABLE else selectorId, maxSize, maxDuration, callback)
}
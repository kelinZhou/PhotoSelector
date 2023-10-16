package com.kelin.photoselector.option

import android.content.Context
import com.kelin.photoselector.MutablePhotoCallabck
import com.kelin.photoselector.PhotoSelector
import com.kelin.photoselector.SinglePhotoCallback
import com.kelin.photoselector.model.AlbumType

interface SelectorOption {
    val context: Context
    val album: AlbumType
}

open class SystemAlbumOption internal constructor(
    override val context: Context,
    override var album: AlbumType
) : SelectorOption

open class SelectorAlbumOption internal constructor(
    override val context: Context,
    override var album: AlbumType,
    /**
     * selectorId作为去重逻辑的依据，只有自定义相册才支持，系统相册不支持。
     * 改参数只有调用select方法时maxLength大于1才有效。
     */
    var selectorId: Int = PhotoSelector.ID_REPEATABLE,
    /**
     * 限制最大文件大小，单位MB。
     */
    var maxSize: Float = 0F,
    /**
     * 限制视频文件时长，只有在相册支持视频选择时该参数才有意义。
     */
    var maxDuration: Int = 0
) : SelectorOption

/**
 * 选择照片或视频。
 * @param callback 选择成功后的回调。
 */
fun SystemAlbumOption.select(callback: SinglePhotoCallback) {
    PhotoSelector.realOpenSelector(context, true, album, 1, PhotoSelector.ID_REPEATABLE, 0F, 0, callback)
}

/**
 * 选择照片或视频。
 * @param maxLength 要选择的数量。
 * @param callback 选择成功后的回调。
 */
fun SystemAlbumOption.selectAll(maxLength: Int = PhotoSelector.defMaxLength, callback: MutablePhotoCallabck) {
    PhotoSelector.realOpenSelector(context, true, album, maxLength, PhotoSelector.ID_REPEATABLE, 0F, 0, callback)
}

/**
 * 选择照片或视频。
 * @param callback 选择成功后的回调。
 */
fun SelectorAlbumOption.select(callback: SinglePhotoCallback) {
    PhotoSelector.realOpenSelector(context, false, album, 1, PhotoSelector.ID_SINGLE, maxSize, maxDuration, callback)
}

/**
 * 选择照片或视频。
 * @param maxLength 要选择的数量。
 * @param callback 选择成功后的回调。
 */
fun SelectorAlbumOption.selectAll(maxLength: Int = PhotoSelector.defMaxLength, callback: MutablePhotoCallabck) {
    PhotoSelector.realOpenSelector(context, false, album, maxLength, if (maxLength == 1) PhotoSelector.ID_SINGLE else selectorId, maxSize, maxDuration, callback)
}
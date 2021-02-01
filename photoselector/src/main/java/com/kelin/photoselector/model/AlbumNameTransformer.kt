package com.kelin.photoselector.model

import android.content.Context
import com.kelin.photoselector.PhotoSelector
import com.kelin.photoselector.R

/**
 * **描述:** 名字转换器
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/10 1:27 PM
 *
 * **版本:** v 1.0.0
 */
open class AlbumNameTransformer : NameTransformer {
    override fun transform(context: Context, name: String): String {
        return when {
            name.equals("camera", true) -> {
                context.getString(R.string.kelin_photo_selector_camera)
            }
            name.equals("download", true) -> {
                context.getString(R.string.kelin_photo_selector_download)
            }
            name.equals("weixin", true) -> {
                context.getString(R.string.kelin_photo_selector_weichat)
            }
            name.equals("screenshots", true) -> {
                context.getString(R.string.kelin_photo_selector_screenshots)
            }
            name.equals(PhotoSelector.DEFAULT_PICTURE_DIR, true) -> {
                context.getString(R.string.kelin_photo_selector_app_album)
            }
            else -> {
                name
            }
        }
    }
}
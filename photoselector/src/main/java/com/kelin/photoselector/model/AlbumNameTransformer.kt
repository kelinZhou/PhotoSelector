package com.kelin.photoselector.model

import com.kelin.photoselector.PhotoSelector

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
    override fun transform(name: String): String {
        return when {
            name.equals("camera", true) -> {
                "相机"
            }
            name.equals("download", true) -> {
                "下载"
            }
            name.equals("weixin", true) -> {
                "微信"
            }
            name.equals("screenshots", true) -> {
                "截屏"
            }
            name.equals(PhotoSelector.DEFAULT_PICTURE_DIR, true) -> {
                "应用相册"
            }
            else -> {
                name
            }
        }
    }
}
package com.kelin.photoselector.model

import android.content.Context

/**
 * **描述:** 名字转换器
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/10 1:27 PM
 *
 * **版本:** v 1.0.0
 */
interface NameTransformer {
    fun transform(context: Context, name: String): String
}
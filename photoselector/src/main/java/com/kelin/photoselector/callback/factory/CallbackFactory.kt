package com.kelin.photoselector.callback.factory

import com.kelin.photoselector.callback.LeakProofCallback

/**
 * **描述:** 回调的生产工厂。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/8/7 10:05 AM
 *
 * **版本:** v 1.0.0
 */
interface CallbackFactory<R> {
    fun createCallback(): LeakProofCallback<R>
}
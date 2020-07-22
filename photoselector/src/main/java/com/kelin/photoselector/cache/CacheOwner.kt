package com.kelin.photoselector.cache

/**
 * **描述:** 缓存拥有者。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/22 9:57 AM
 *
 * **版本:** v 1.0.0
 */
internal interface CacheOwner<E> {

    fun detach(id: Int)
}
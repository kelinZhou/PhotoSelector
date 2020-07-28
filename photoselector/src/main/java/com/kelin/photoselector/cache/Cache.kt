package com.kelin.photoselector.cache

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

/**
 * **描述:** 定义缓存接口，具有生命周期感知，可自动销毁。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/22 9:38 AM
 *
 * **版本:** v 1.0.0
 */
internal interface Cache<E> : LifecycleObserver {

    val cache: E

    fun onCache(caches: E)

    fun addCache(caches: E)

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy()

    fun remove(position: Int)

    fun remove(uri: String)
}
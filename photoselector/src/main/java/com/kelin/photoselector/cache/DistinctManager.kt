package com.kelin.photoselector.cache

import android.util.SparseArray
import androidx.lifecycle.LifecycleObserver
import com.kelin.photoselector.model.Picture

/**
 * **描述:** 从相册选择图片或视频是的去重管理。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/22 9:48 AM
 *
 * **版本:** v 1.0.0
 */
internal class DistinctManager private constructor() : CacheOwner<List<Picture>> {

    companion object {
        internal val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { DistinctManager() }
    }

    private val pool by lazy { SparseArray<Cache<List<Picture>>>() }

    internal fun tryNewCache(id: Int): LifecycleObserver {
        var cache = pool[id]
        if (cache == null) {
            cache = PhotoCache(id, this)
            pool.put(id, cache)
        }
        return cache
    }

    internal fun getSelected(id: Int): List<Picture>? {
        return if (id != -1) pool[id]?.cache else null
    }

    internal fun saveSelected(id: Int, selected: List<Picture>) {
        if (id != -1) {
            pool[id]?.onCache(selected)
        }
    }

    internal fun remove(id: Int, position: Int) {
        if (id != -1) {
            pool[id]?.remove(position)
        }
    }

    internal fun remove(id: Int, uri: String) {
        if (id != -1) {
            pool[id]?.remove(uri)
        }
    }

    override fun detach(id: Int) {
        pool.remove(id)
    }
}
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
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { DistinctManager() }
    }

    private val pool by lazy { SparseArray<Cache<List<Picture>>>() }

    fun tryNewCache(id: Int): LifecycleObserver {
        var cache = pool[id]
        if (cache == null) {
            cache = PhotoCache(id, this)
            pool.put(id, cache)
        }
        return cache
    }

    fun getSelected(id: Int): List<Picture>? {
        return if (id != -1) pool[id]?.cache else null
    }

    fun saveSelected(id: Int, selected: List<Picture>) {
        if (id != -1) {
            pool[id]?.onCache(selected)
        }
    }

    override fun detach(id: Int) {
        pool.remove(id)
    }
}
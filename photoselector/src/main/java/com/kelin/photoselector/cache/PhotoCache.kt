package com.kelin.photoselector.cache

import com.kelin.photoselector.model.Picture

/**
 * **描述:** 图片去重缓存。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/22 10:15 AM
 *
 * **版本:** v 1.0.0
 */
internal class PhotoCache(private val id: Int, private val owner: CacheOwner<List<Picture>>) : Cache<List<Picture>> {

    private var selectedPhotos: List<Picture>? = null

    override val cache: List<Picture>
        get() = selectedPhotos?.let { ArrayList<Picture>(it) } ?: emptyList()

    override fun onCache(photos: List<Picture>) {
        selectedPhotos = ArrayList(photos)
    }

    override fun onDestroy() {
        selectedPhotos = null
        owner.detach(id)
    }
}
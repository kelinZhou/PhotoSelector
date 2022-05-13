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
internal class PictureCache(private val id: Int, private val owner: CacheOwner<List<Picture>>) : Cache<List<Picture>> {

    private var selectedPhotos: MutableList<Picture>? = null

    override val cache: List<Picture>
        get() = selectedPhotos?.let { ArrayList<Picture>(it) } ?: emptyList()

    override fun onCache(caches: List<Picture>) {
        selectedPhotos = ArrayList(caches)
    }

    override fun addCache(caches: List<Picture>) {
        if (selectedPhotos == null) {
            selectedPhotos = ArrayList(caches)
        } else {
            selectedPhotos!!.addAll(caches)
        }
    }

    override fun remove(position: Int) {
        selectedPhotos?.also {
            if (position >= 0 && position < it.size) {
                it.removeAt(position)
            }
        }
    }

    override fun remove(uri: String) {
        selectedPhotos?.also {
            val i = it.iterator()
            while (i.hasNext()) {
                if (i.next().uri == uri) {
                    i.remove()
                    break
                }
            }
        }
    }

    override fun onDestroy() {
        selectedPhotos = null
        owner.detach(id)
    }
}
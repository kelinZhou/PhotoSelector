package com.kelin.photoselector.callback.factory

import android.app.Activity
import android.content.Context
import com.kelin.okpermission.OkActivityResult
import com.kelin.photoselector.PhotoSelectorActivity
import com.kelin.photoselector.callback.BaseCallback
import com.kelin.photoselector.callback.LeakProofCallback
import com.kelin.photoselector.model.AlbumType
import com.kelin.photoselector.model.Photo

/**
 * **描述:** 选择照片或视频的回调工厂。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/8/13 9:52 AM
 *
 * **版本:** v 1.0.0
 */
internal class SelectPictureCallbackFactory<R>(private val albumType: AlbumType, private val maxLength: Int, private val id: Int, private val maxSize: Float, private val maxDuration: Int) : CallbackFactory<R?> {
    override fun createCallback(): LeakProofCallback<R?> {
        return object : BaseCallback<R?>() {
            override fun onAttach(context: Context) {
                OkActivityResult.startActivity<R>(
                    context as Activity,
                    PhotoSelectorActivity.createPictureSelectorIntent(context, albumType, maxLength, id, maxSize, maxDuration)
                ) { data ->
                    if (data != null) {
                        callback(data)
                    } else {
                        callback(null)
                    }
                }
            }
        }
    }
}
package com.kelin.photoselector.callback.factory

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.kelin.okpermission.OkActivityResult
import com.kelin.photoselector.PhotoSelector
import com.kelin.photoselector.callback.BaseCallback
import com.kelin.photoselector.callback.LeakProofCallback
import com.kelin.photoselector.model.AlbumType
import com.kelin.photoselector.model.Photo
import com.kelin.photoselector.model.toPhoto

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class Android12SelectPictureCallbackFactory<R>(private val albumType: AlbumType, val id: Int, private val maxLength: Int) : CallbackFactory<R?> {

    override fun createCallback(): LeakProofCallback<R?> {
        return object : BaseCallback<R?>() {
            @Suppress("UNCHECKED_CAST")
            override fun onAttach(context: Context) {
                val intent = Intent(MediaStore.ACTION_PICK_IMAGES)
                if (maxLength > 1) {
                    intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, maxLength)
                }
                when (albumType) {
                    AlbumType.PHOTO -> intent.type = "image/*"
                    AlbumType.VIDEO -> intent.type = "video/*"
                    else -> {}
                }
                // Ensure that there's a camera activity to handle the intent
                // 官方注释，确保有一个活动来打开相机意图
                if (intent.resolveActivity(context.packageManager) != null) {
                    OkActivityResult.startActivity<Intent>(
                        context as Activity,
                        intent
                    ) { data ->
                        if (data != null) {
                            if (maxLength > 1) {
                                data.clipData.also { clipData ->
                                    if (clipData != null) {
                                        val result = ArrayList<Photo>()
                                        for (i in 0 until clipData.itemCount) {
                                            clipData.getItemAt(i).uri.toPhoto(context, albumType == AlbumType.VIDEO)?.also { result.add(it) }
                                        }
                                        callback(result as R)
                                    } else {
                                        callback(null)
                                    }
                                }
                            } else {
                                data.data.toPhoto(context, albumType == AlbumType.VIDEO).also { photo ->
                                    if (id == PhotoSelector.ID_SINGLE) {
                                        callback(photo as? R)
                                    } else {
                                        callback(photo?.let { listOf(it) } as? R)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

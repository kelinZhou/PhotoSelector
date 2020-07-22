package com.kelin.photoselector

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.kelin.photoselector.cache.DistinctManager
import com.kelin.photoselector.model.AlbumNameTransformer
import com.kelin.photoselector.model.AlbumType
import com.kelin.photoselector.model.NameTransformer
import com.kelin.photoselector.model.Photo
import java.io.File

/**
 * **描述:** 图片选择器核心类。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/10 1:25 PM
 *
 * **版本:** v 1.0.0
 */
object PhotoSelector {

    /**
     * 相册命名改变器。
     */
    private var albumNameTransformer: NameTransformer = AlbumNameTransformer()

    /**
     * 注册相册命名改变器。注册之后将会使用您自定义的改变器改变规则为相册命名。
     * @param transform 您自己的定义的改变器，您可以直接继承AlbumNameTransformer，这样您在需要处理时使用自己的逻辑，其余的返回super就可以了。
     */
    fun registAlbumNameTransformer(transform: NameTransformer) {
        albumNameTransformer = transform
    }

    internal fun transformAlbumName(name: String): String {
        return albumNameTransformer.transform(name)
    }

    /**
     * 打开图片选择页面。页面启动后只能选择图片文件。
     */
    fun openPhotoSelector(fragment: Fragment, maxCount: Int = 9, id: Int = fragment.hashCode(), result: (photos: List<Photo>) -> Unit) {
        val activity = fragment.activity
        if (activity != null) {
            if (id != -1) {
                fragment.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
            }
            PhotoSelectorActivity.startPictureSelectorPage(activity, AlbumType.PHOTO, maxCount, id, result)
        }
    }

    /**
     * 打开图片选择页面。页面启动后只能选择图片文件。
     */
    fun openPhotoSelector(context: Context, maxCount: Int = 9, id: Int = context.hashCode(), result: (photos: List<Photo>) -> Unit) {
        if (id != -1 && context is LifecycleOwner) {
            context.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
        }
        PhotoSelectorActivity.startPictureSelectorPage(context, AlbumType.PHOTO, maxCount, id, result)
    }

    /**
     * 打开视频选择页面。页面启动后只能选择视频文件。
     */
    fun openVideoSelector(context: Context, maxCount: Int = 9, id: Int = context.hashCode(), result: (photos: List<Photo>) -> Unit) {
        if (id != -1 && context is LifecycleOwner) {
            context.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
        }
        PhotoSelectorActivity.startPictureSelectorPage(context, AlbumType.VIDEO, maxCount, id, result)
    }

    /**
     * 打开图片和视频的选择页面。页面启动后即能选择图片文件也能选择视频文件。
     */
    fun openPictureSelector(context: Context, maxCount: Int = 9, id: Int = context.hashCode(), result: (photos: List<Photo>) -> Unit) {
        PhotoSelectorActivity.startPictureSelectorPage(context, AlbumType.PHOTO_VIDEO, maxCount, id, result)
    }

    /**
     * 打开图片和视频的预览页面。
     * @param context Activity的Context。
     * @param pictures 要预览的所有图片，可以是图片也可以是视频；可以是本地的也可以是网络的。
     * @param select 默认选中的页，例如你一共有5张图片需要预览，但是你希望默认加载第三张图片那么该参数你需要传2(0表示第一张)。
     */
    fun openPicturePreviewPage(context: Context, pictures: List<Photo>, select: Int = 0) {
        if (pictures.isNotEmpty()) {
            PhotoPreviewActivity.start(context, pictures, select)
        }
    }

    fun playVideoWithSystem(context: Context, photo: Photo) {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                photo.getUri(context) ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || photo.uri.startsWith("http")) {
                    Uri.parse(photo.uri)
                } else {
                    Uri.fromFile(File(photo.uri))
                },
                "video/*"
            )
        })
    }
}
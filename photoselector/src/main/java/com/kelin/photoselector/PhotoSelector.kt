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

    /**
     * 改变相册命名。
     */
    internal fun transformAlbumName(name: String): String {
        return albumNameTransformer.transform(name)
    }

    /**
     * 打开图片选择页面。页面启动后只能选择图片文件。
     * @param fragment 在Fragment中使用时无需Activity实例，只需传入当前的Fragment实例即可。
     * @param maxCount 最大数量，用于设置最多可选择多少张图片。
     * @param id    为本次选择设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Fragment的hashCode，即表示当前Fragment中不允许有重复的图片被选择，
     * 如果当前不是第一次打开图片选择且之前完成过选择(完成是指点击了图片选择页面的完成按钮)，那么之前选择过的图片默认会被勾选(即数据回显)。如果您的页面中有多处需要选择
     * 图片的地方且去重逻辑互不影响，那么您需要手动为每一处的打开设置不同的id。如果您不希望开启自动去重的功能，那么您可以将该参数设置为-1。
     * @param result 选中结果，当用户点击了完成按钮后会将用户已经勾选的所有图片(包括数据回显选中的图片)回调给您。
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
     * @param context 在Activity中使用时您需要传入当前Activity的实例。
     * @param maxCount 最大数量，用于设置最多可选择多少张图片。
     * @param id    为本次选择设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Activity的hashCode，即表示当前Activity中不允许有重复的图片被选择，
     * 如果当前不是第一次打开图片选择且之前完成过选择(完成是指点击了图片选择页面的完成按钮)，那么之前选择过的图片默认会被勾选(即数据回显)。如果您的页面中有多处需要选择
     * 图片的地方且去重逻辑互不影响，那么您需要手动为每一处的打开设置不同的id。如果您不希望开启自动去重的功能，那么您可以将该参数设置为-1。
     * @param result 选中结果，当用户点击了完成按钮后会将用户已经勾选的所有图片(包括数据回显选中的图片)回调给您。
     */
    fun openPhotoSelector(context: Context, maxCount: Int = 9, id: Int = context.hashCode(), result: (photos: List<Photo>) -> Unit) {
        if (id != -1 && context is LifecycleOwner) {
            context.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
        }
        PhotoSelectorActivity.startPictureSelectorPage(context, AlbumType.PHOTO, maxCount, id, result)
    }

    /**
     * 打开视频选择页面。页面启动后只能选择视频文件。
     * @param fragment 在Fragment中使用时无需Activity实例，只需传入当前的Fragment实例即可。
     * @param maxCount 最大数量，用于设置最多可选择多少个视频。
     * @param id    为本次选择设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Activity的hashCode，即表示当前Activity中不允许有重复的视频被选择，
     * 如果当前不是第一次打开视频选择且之前完成过选择(完成是指点击了视频选择页面的完成按钮)，那么之前选择过的视频默认会被勾选(即数据回显)。如果您的页面中有多处需要选择
     * 视频的地方且去重逻辑互不影响，那么您需要手动为每一处的打开设置不同的id。如果您不希望开启自动去重的功能，那么您可以将该参数设置为-1。
     * @param result 选中结果，当用户点击了完成按钮后会将用户已经勾选的所有视频(包括数据回显选中的视频)回调给您。
     */
    fun openVideoSelector(fragment: Fragment, maxCount: Int = 9, id: Int = fragment.hashCode(), result: (photos: List<Photo>) -> Unit) {
        val activity = fragment.activity
        if (activity != null) {
            if (id != -1) {
                fragment.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
            }
            PhotoSelectorActivity.startPictureSelectorPage(activity, AlbumType.VIDEO, maxCount, id, result)
        }
    }

    /**
     * 打开视频选择页面。页面启动后只能选择视频文件。
     * @param context 在Activity中使用时您需要传入当前Activity的实例。
     * @param maxCount 最大数量，用于设置最多可选择多少个视频。
     * @param id    为本次选择设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Activity的hashCode，即表示当前Activity中不允许有重复的视频被选择，
     * 如果当前不是第一次打开视频选择且之前完成过选择(完成是指点击了视频选择页面的完成按钮)，那么之前选择过的视频默认会被勾选(即数据回显)。如果您的页面中有多处需要选择
     * 视频的地方且去重逻辑互不影响，那么您需要手动为每一处的打开设置不同的id。如果您不希望开启自动去重的功能，那么您可以将该参数设置为-1。
     * @param result 选中结果，当用户点击了完成按钮后会将用户已经勾选的所有视频(包括数据回显选中的视频)回调给您。
     */
    fun openVideoSelector(context: Context, maxCount: Int = 9, id: Int = context.hashCode(), result: (photos: List<Photo>) -> Unit) {
        if (id != -1 && context is LifecycleOwner) {
            context.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
        }
        PhotoSelectorActivity.startPictureSelectorPage(context, AlbumType.VIDEO, maxCount, id, result)
    }

    /**
     * 打开图片和视频的选择页面。页面启动后即能选择图片文件也能选择视频文件。
     * @param fragment 在Fragment中使用时无需Activity实例，只需传入当前的Fragment实例即可。
     * @param maxCount 最大数量，用于设置最多可选择多少个图片和视频。
     * @param id    为本次选择设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Activity的hashCode，即表示当前Activity中不允许有重复的图片和视频被选择，
     * 如果当前不是第一次打开图片和视频选择且之前完成过选择(完成是指点击了图片和视频选择页面的完成按钮)，那么之前选择过的视频默认会被勾选(即数据回显)。如果您的页面中有多处需要选择
     * 图片和视频的地方且去重逻辑互不影响，那么您需要手动为每一处的打开设置不同的id。如果您不希望开启自动去重的功能，那么您可以将该参数设置为-1。
     * @param result 选中结果，当用户点击了完成按钮后会将用户已经勾选的所有图片和视频(包括数据回显选中的图片和视频)回调给您。
     */
    fun openPictureSelector(fragment: Fragment, maxCount: Int = 9, id: Int = fragment.hashCode(), result: (photos: List<Photo>) -> Unit) {
        val activity = fragment.activity
        if (activity != null) {
            if (id != -1) {
                fragment.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
            }
            PhotoSelectorActivity.startPictureSelectorPage(activity, AlbumType.PHOTO_VIDEO, maxCount, id, result)
        }
    }

    /**
     * 打开图片和视频的选择页面。页面启动后即能选择图片文件也能选择视频文件。
     * @param context 在Activity中使用时您需要传入当前Activity的实例。
     * @param maxCount 最大数量，用于设置最多可选择多少个图片和视频。
     * @param id    为本次选择设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Activity的hashCode，即表示当前Activity中不允许有重复的图片和视频被选择，
     * 如果当前不是第一次打开图片和视频选择且之前完成过选择(完成是指点击了图片和视频选择页面的完成按钮)，那么之前选择过的视频默认会被勾选(即数据回显)。如果您的页面中有多处需要选择
     * 图片和视频的地方且去重逻辑互不影响，那么您需要手动为每一处的打开设置不同的id。如果您不希望开启自动去重的功能，那么您可以将该参数设置为-1。
     * @param result 选中结果，当用户点击了完成按钮后会将用户已经勾选的所有图片和视频(包括数据回显选中的图片和视频)回调给您。
     */
    fun openPictureSelector(context: Context, maxCount: Int = 9, id: Int = context.hashCode(), result: (photos: List<Photo>) -> Unit) {
        if (id != -1 && context is LifecycleOwner) {
            context.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
        }
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

    /**
     * 调用系统的播放功能播放视频。
     * @param context 需要Activity的Context。
     * @param photo Photo对象。
     */
    fun playVideoWithSystem(context: Context, photo: Photo) {
        playVideoWithSystem(
            context, photo.getUri(context) ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || photo.uri.startsWith("http")) {
                Uri.parse(photo.uri)
            } else {
                Uri.fromFile(File(photo.uri))
            }
        )
    }

    /**
     * 调用系统的播放功能播放视频。
     * @param context 需要Activity的Context。
     * @param uri 视频文件的uri地址。
     */
    fun playVideoWithSystem(context: Context, uri: Uri) {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                uri,
                "video/*"
            )
        })
    }
}
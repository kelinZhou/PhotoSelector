package com.kelin.photoselector

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.kelin.okpermission.OkPermission
import com.kelin.photoselector.cache.DistinctManager
import com.kelin.photoselector.callback.factory.*
import com.kelin.photoselector.callback.factory.SelectPictureCallbackFactory
import com.kelin.photoselector.model.*
import com.kelin.photoselector.model.AlbumType
import com.kelin.photoselector.option.SelectorAlbumOption
import com.kelin.photoselector.option.SystemAlbumOption
import java.io.File

typealias SinglePhotoCallback = (photo: Photo?) -> Unit

typealias MutablePhotoCallabck = (photos: List<Photo>?) -> Unit

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
     * 当选择图片可以重复时的ID。
     */
    const val ID_REPEATABLE = -1

    /**
     * 单选图片或视频时的ID。
     */
    internal const val ID_SINGLE = -9999

    internal const val DEFAULT_PICTURE_DIR = "photoSelector"

    /**
     * 相册命名改变器。
     */
    private var albumNameTransformer: NameTransformer = AlbumNameTransformer()

    private var fileProvider: String? = null
    internal var defMaxLength: Int = 9

    /**
     * 记录是否需要自动旋转图片，针对某些设备拍照后会自动旋转的问题。
     */
    internal var isAutoCompress: Boolean = false

    /**
     * 拍照和录像时的视频或图片的存储路径。
     */
    private var pictureDir: String = DEFAULT_PICTURE_DIR

    /**
     * 是否在相册中显示拍照按钮。
     */
    internal var isAlbumTakePictureEnable: Boolean = true

    private val requireFileProvider: String
        get() = fileProvider ?: throw NullPointerException("You need call the init method first to set fileProvider.")


    private var cacheDir: String? = null

    internal val requireCacheDir: String
        get() = "${cacheDir ?: throw NullPointerException("You need call the init method first.")}/KelinPhotoSelector/CompressAndRotate/"

    /**
     * 初始化PhotoSelector库，改方法几乎不耗时，可放心在Application的onCreate方法中使用。
     * @param context Application的Context即可。
     * @param provider 用于适配在7.0及以上Android版本的文件服务。
     * @param autoCompress 是否开启自动压缩，如果开启自动压缩还会同时打开图片自动纠正的功能(针对某些机型(例如小米手机)拍照后图片会歪的问题)。
     * @param maxLength 统一设置选择图片或视频时的最大选择数量。如有特殊情况则可以在具体调用时再行设置。
     */
    fun init(context: Context, provider: String, autoCompress: Boolean = false, maxLength: Int = 9, albumTakePictureEnable: Boolean = true) {
        cacheDir = context.cacheDir.absolutePath
        defMaxLength = maxLength
        fileProvider = provider
        isAutoCompress = autoCompress
        isAlbumTakePictureEnable = albumTakePictureEnable
        pictureDir = context.packageName.let {
            val index = it.lastIndexOf(".")
            if (index >= 0 && index < it.length) {
                it.substring(index + 1)
            } else {
                DEFAULT_PICTURE_DIR
            }
        }
    }

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
    internal fun transformAlbumName(context: Context, name: String): String {
        return albumNameTransformer.transform(context, name)
    }

    /**
     * 拍摄照片。
     * @param fragment 在Fragment中使用时无需Activity实例，只需传入当前的Fragment实例即可。
     * @param id    为本次拍照设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Fragment的hashCode，即表示当前Fragment中不允许有重复的图片被选择，
     * 如果你的页面有多张照片需要上传那么本次拍照会在id相同的选择中复显。
     * @param targetFile 拍摄的照片要存放的临时目标文件，默认为空，如果为空则使用默认的目标路径。如果你需要自己指定则需要传入该值。
     * @param onResult 拍摄完成的回调，会将照片文件回调给您。
     */
    fun takePhoto(fragment: Fragment, id: Int = fragment.hashCode(), targetFile: File? = null, onResult: (photo: File?) -> Unit) {
        fragment.activity?.also { activity ->
            if (id != ID_REPEATABLE && id != ID_SINGLE) {
                fragment.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
            }
            attachCallback(activity, PermissionCallbackFactory(OkPermission.permission_group.CAMERA_FOR_PICTURE_OR_VIDEO)) { context, granted ->
                if (granted) {
                    takePicture(context as Activity, id, MediaStore.ACTION_IMAGE_CAPTURE, targetFile ?: File("${context.getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath}${File.separator}${pictureDir}${File.separator}", "${System.currentTimeMillis()}.jpg"), onResult)
                }
            }
        }
    }

    /**
     * 拍摄照片。
     * @param activity 需要当前的Activity实例。
     * @param id    为本次拍照设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Activity的hashCode，即表示当前Activity中不允许有重复的图片被选择，
     * 如果你的页面有多张照片需要上传那么本次拍照会在id相同的选择中复显。
     * @param targetFile 拍摄的照片要存放的临时目标文件，默认为空，如果为空则使用默认的目标路径。如果你需要自己指定则需要传入该值。
     * @param onResult 拍摄完成的回调，会将照片文件回调给您。
     */
    fun takePhoto(activity: Activity, id: Int = activity.hashCode(), targetFile: File? = null, onResult: (photo: File?) -> Unit) {
        if (id != ID_REPEATABLE && id != ID_SINGLE && activity is LifecycleOwner) {
            activity.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
        }
        attachCallback(activity, PermissionCallbackFactory(OkPermission.permission_group.CAMERA_FOR_PICTURE_OR_VIDEO)) { context, granted ->
            if (granted) {
                takePicture(context as Activity, id, MediaStore.ACTION_IMAGE_CAPTURE, targetFile ?: File("${context.getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath}${File.separator}${pictureDir}${File.separator}", "${System.currentTimeMillis()}.jpg"), onResult)
            }
        }
    }

    /**
     * 拍摄视频。
     * @param fragment 在Fragment中使用时无需Activity实例，只需传入当前的Fragment实例即可。
     * @param id    为本次拍照设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Fragment的hashCode，即表示当前Fragment中不允许有重复的视频被选择，
     * 如果你的页面有多个视频需要上传那么本次拍摄会在id相同的选择中复显。
     * @param targetFile 拍摄的视频要存放的临时目标文件，默认为空，如果为空则使用默认的目标路径。如果你需要自己指定则需要传入该值。
     * @param onResult 拍摄完成的回调，会将视频文件回调给您。
     */
    fun takeVideo(fragment: Fragment, id: Int = fragment.hashCode(), targetFile: File? = null, onResult: (photo: File?) -> Unit) {
        fragment.activity?.also { activity ->
            if (id != ID_REPEATABLE && id != ID_SINGLE) {
                fragment.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
            }
            attachCallback(activity, PermissionCallbackFactory(OkPermission.permission_group.CAMERA_FOR_PICTURE_OR_VIDEO)) { context, granted ->
                if (granted) {
                    takePicture(context as Activity, id, MediaStore.ACTION_VIDEO_CAPTURE, targetFile ?: File("${context.getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath}${File.separator}${pictureDir}${File.separator}", "${System.currentTimeMillis()}.mp4"), onResult)
                }
            }
        }
    }

    /**
     * 拍摄视频。
     * @param activity 需要当前的Activity实例。
     * @param id    为本次拍照设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Activity的hashCode，即表示当前Activity中不允许有重复的图片被选择，
     * 如果你的页面有多个视频需要上传那么本次拍摄会在id相同的选择中复显。
     * @param targetFile 拍摄的视频要存放的临时目标文件，默认为空，如果为空则使用默认的目标路径。如果你需要自己指定则需要传入该值。
     * @param onResult 拍摄完成的回调，会将视频文件回调给您。
     */
    fun takeVideo(activity: Activity, id: Int = activity.hashCode(), targetFile: File? = null, onResult: (photo: File?) -> Unit) {
        if (id != ID_REPEATABLE && id != ID_SINGLE && activity is LifecycleOwner) {
            activity.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
        }
        attachCallback(activity, PermissionCallbackFactory(OkPermission.permission_group.CAMERA_FOR_PICTURE_OR_VIDEO)) { context, granted ->
            if (granted) {
                takePicture(context as Activity, id, MediaStore.ACTION_VIDEO_CAPTURE, targetFile ?: File("${context.getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath}${File.separator}${pictureDir}${File.separator}", "${System.currentTimeMillis()}.mp4"), onResult)
            }
        }
    }

    fun takePicture(activity: Activity, id: Int, action: String, targetFile: File, onResult: (photo: File?) -> Unit) {
        attachCallback(activity, TakePictureCallbackFactory(id, action, targetFile, requireFileProvider)) { _, r -> onResult(r) }
    }

    fun <R> attachCallback(context: Context, factory: CallbackFactory<R>, callback: (context: Context, r: R) -> Unit) {
        factory.createCallback().createAndAttachTo(context, callback)
    }

    /**
     * 打开图片选择页面（单选）。页面启动后只能选择图片文件。
     * @param fragment 在Fragment中使用时无需Activity实例，只需传入当前的Fragment实例即可。
     * @param result 选中结果，由于是单选，是有意当用户点击了某个图片的选择框后就会将这个图片回调给你。
     */
    @Deprecated("Please use withSysAlbum or withSelectorAlbum method.", replaceWith = ReplaceWith("PhotoSelector.withSelectorAlbum(fragment, AlbumType.PHOTO){\nselect { photo ->\n\n}\n}", "com.kelin.photoselector.model.AlbumType"))
    fun openPhotoSelectorSingle(fragment: Fragment, result: SinglePhotoCallback) {
        fragment.activity?.also { activity ->
            realOpenSelector<Photo>(activity, false, AlbumType.PHOTO, 1, ID_SINGLE, 0F, 0, result)
        }
    }

    /**
     * 打开图片选择页面。页面启动后只能选择图片文件。
     * @param fragment 在Fragment中使用时无需Activity实例，只需传入当前的Fragment实例即可。
     * @param maxLength 最大数量，用于设置最多可选择多少张图片。
     * @param id    为本次选择设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Fragment的hashCode，即表示当前Fragment中不允许有重复的图片被选择，
     * 如果当前不是第一次打开图片选择且之前完成过选择(完成是指点击了图片选择页面的完成按钮)，那么之前选择过的图片默认会被勾选(即数据回显)。如果您的页面中有多处需要选择
     * 图片的地方且去重逻辑互不影响，那么您需要手动为每一处的打开设置不同的id。如果您不希望开启自动去重的功能，那么您可以将该参数设置为ID_REPEATABLE。
     * @param result 选中结果，当用户点击了完成按钮后会将用户已经勾选的所有图片(包括数据回显选中的图片)回调给您。
     */
    @Deprecated("Please use withSysAlbum or withSelectorAlbum method.", replaceWith = ReplaceWith("PhotoSelector.withSelectorAlbum(fragment, AlbumType.PHOTO){\nselectAll(maxLength) { photos ->\n\n}\n}", "com.kelin.photoselector.model.AlbumType"))
    fun openPhotoSelector(fragment: Fragment, maxLength: Int = defMaxLength, id: Int = fragment.hashCode(), result: MutablePhotoCallabck) {
        fragment.activity?.also { activity ->
            if (id != ID_REPEATABLE && id != ID_SINGLE) {
                fragment.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
            }
            realOpenSelector<List<Photo>>(activity, false, AlbumType.PHOTO, maxLength, id, 0F, 0, result)
        }
    }

    /**
     * 打开图片选择页面（单选）。页面启动后只能选择图片文件。
     * @param context 在Activity中使用时您需要传入当前Activity的实例。
     * @param result 选中结果，由于是单选，是有意当用户点击了某个图片的选择框后就会将这个图片回调给你。
     */
    @Deprecated("Please use withSysAlbum or withSelectorAlbum method.", replaceWith = ReplaceWith("PhotoSelector.withSelectorAlbum(context, AlbumType.PHOTO){\nselect { photo ->\n\n}\n}", "com.kelin.photoselector.model.AlbumType"))
    fun openPhotoSelectorSingle(context: Context, result: SinglePhotoCallback) {
        realOpenSelector<Photo>(context, false, AlbumType.PHOTO, 1, ID_SINGLE, 0F, 0, result)
    }

    /**
     * 打开图片选择页面。页面启动后只能选择图片文件。
     * @param context 在Activity中使用时您需要传入当前Activity的实例。
     * @param maxLength 最大数量，用于设置最多可选择多少张图片。
     * @param id    为本次选择设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Activity的hashCode，即表示当前Activity中不允许有重复的图片被选择，
     * 如果当前不是第一次打开图片选择且之前完成过选择(完成是指点击了图片选择页面的完成按钮)，那么之前选择过的图片默认会被勾选(即数据回显)。如果您的页面中有多处需要选择
     * 图片的地方且去重逻辑互不影响，那么您需要手动为每一处的打开设置不同的id。如果您不希望开启自动去重的功能，那么您可以将该参数设置为ID_REPEATABLE。
     * @param result 选中结果，当用户点击了完成按钮后会将用户已经勾选的所有图片(包括数据回显选中的图片)回调给您。
     */
    @Deprecated("Please use withSysAlbum or withSelectorAlbum method.", replaceWith = ReplaceWith("PhotoSelector.withSelectorAlbum(context, AlbumType.PHOTO){\nselectAll(maxLength) { photos ->\n\n}\n}", "com.kelin.photoselector.model.AlbumType"))
    fun openPhotoSelector(context: Context, maxLength: Int = defMaxLength, id: Int = context.hashCode(), result: MutablePhotoCallabck) {
        if (id != ID_REPEATABLE && id != ID_SINGLE && context is LifecycleOwner) {
            context.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
        }
        realOpenSelector<List<Photo>>(context, false, AlbumType.PHOTO, maxLength, id, 0F, 0, result)
    }

    /**
     * 打开视频选择页面（单选）。页面启动后只能选择图片文件。
     * @param fragment 在Fragment中使用时无需Activity实例，只需传入当前的Fragment实例即可。
     * @param maxDuration 最大时长显示，当选择视频是，该参数用于限制选择的最大视频时长，单位秒，0表示不限制时长，默认不限制。
     * @param result 选中结果，由于是单选，是有意当用户点击了某个视频的选择框后就会将这个视频回调给你。
     */
    @Deprecated("Please use withSysAlbum or withSelectorAlbum method.", replaceWith = ReplaceWith("PhotoSelector.withSelectorAlbum(fragment, AlbumType.VIDEO){\nselect { photo ->\n\n}\n}", "com.kelin.photoselector.model.AlbumType"))
    fun openVideoSelectorSingle(fragment: Fragment, maxDuration: Int = 0, result: SinglePhotoCallback) {
        fragment.activity?.also { activity ->
            realOpenSelector<Photo>(activity, false, AlbumType.VIDEO, 1, ID_SINGLE, 0F, maxDuration, result)
        }
    }

    /**
     * 打开视频选择页面。页面启动后只能选择视频文件。
     * @param fragment 在Fragment中使用时无需Activity实例，只需传入当前的Fragment实例即可。
     * @param maxLength 最大数量，用于设置最多可选择多少个视频。
     * @param id    为本次选择设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Activity的hashCode，即表示当前Activity中不允许有重复的视频被选择，
     * 如果当前不是第一次打开视频选择且之前完成过选择(完成是指点击了视频选择页面的完成按钮)，那么之前选择过的视频默认会被勾选(即数据回显)。如果您的页面中有多处需要选择
     * 视频的地方且去重逻辑互不影响，那么您需要手动为每一处的打开设置不同的id。如果您不希望开启自动去重的功能，那么您可以将该参数设置为ID_REPEATABLE。
     * @param maxDuration 最大时长显示，当选择视频是，该参数用于限制选择的最大视频时长，单位秒，0表示不限制时长，默认不限制。
     * @param result 选中结果，当用户点击了完成按钮后会将用户已经勾选的所有视频(包括数据回显选中的视频)回调给您。
     */
    @Deprecated("Please use withSysAlbum or withSelectorAlbum method.", replaceWith = ReplaceWith("PhotoSelector.withSelectorAlbum(fragment, AlbumType.VIDEO){\nselectAll(maxLength) { photos ->\n\n}\n}", "com.kelin.photoselector.model.AlbumType"))
    fun openVideoSelector(fragment: Fragment, maxLength: Int = defMaxLength, id: Int = fragment.hashCode(), maxDuration: Int = 0, result: MutablePhotoCallabck) {
        fragment.activity?.also { activity ->
            if (id != ID_REPEATABLE && id != ID_SINGLE) {
                fragment.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
            }
            realOpenSelector<List<Photo>>(activity, false, AlbumType.VIDEO, maxLength, id, 0F, maxDuration, result)
        }
    }

    /**
     * 打开视频选择页面（单选）。页面启动后只能选择图片文件。
     * @param context 在Activity中使用时您需要传入当前Activity的实例。
     * @param maxDuration 最大时长显示，当选择视频是，该参数用于限制选择的最大视频时长，单位秒，0表示不限制时长，默认不限制。
     * @param result 选中结果，由于是单选，是有意当用户点击了某个视频的选择框后就会将这个视频回调给你。
     */
    @Deprecated("Please use withSysAlbum or withSelectorAlbum method.", replaceWith = ReplaceWith("PhotoSelector.withSelectorAlbum(context, AlbumType.VIDEO){\nselect { photo ->\n\n}\n}", "com.kelin.photoselector.model.AlbumType"))
    fun openVideoSelectorSingle(context: Context, maxDuration: Int = 0, result: SinglePhotoCallback) {
        realOpenSelector<Photo>(context, false, AlbumType.VIDEO, 1, ID_SINGLE, 0F, maxDuration, result)
    }

    /**
     * 打开视频选择页面。页面启动后只能选择视频文件。
     * @param context 在Activity中使用时您需要传入当前Activity的实例。
     * @param maxLength 最大数量，用于设置最多可选择多少个视频。
     * @param id    为本次选择设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Activity的hashCode，即表示当前Activity中不允许有重复的视频被选择，
     * 如果当前不是第一次打开视频选择且之前完成过选择(完成是指点击了视频选择页面的完成按钮)，那么之前选择过的视频默认会被勾选(即数据回显)。如果您的页面中有多处需要选择
     * 视频的地方且去重逻辑互不影响，那么您需要手动为每一处的打开设置不同的id。如果您不希望开启自动去重的功能，那么您可以将该参数设置为ID_REPEATABLE。
     * @param maxDuration 最大时长显示，当选择视频是，该参数用于限制选择的最大视频时长，单位秒，0表示不限制时长，默认不限制。
     * @param result 选中结果，当用户点击了完成按钮后会将用户已经勾选的所有视频(包括数据回显选中的视频)回调给您。
     */
    @Deprecated("Please use withSysAlbum or withSelectorAlbum method.", replaceWith = ReplaceWith("PhotoSelector.withSelectorAlbum(context, AlbumType.VIDEO){\nselectAll(maxLength) { photos ->\n\n}\n}", "com.kelin.photoselector.model.AlbumType"))
    fun openVideoSelector(context: Context, maxLength: Int = defMaxLength, id: Int = context.hashCode(), maxDuration: Int = 0, result: MutablePhotoCallabck) {
        if (id != ID_REPEATABLE && id != ID_SINGLE && context is LifecycleOwner) {
            context.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
        }
        realOpenSelector<List<Photo>>(context, false, AlbumType.VIDEO, maxLength, id, 0F, maxDuration, result)
    }

    /**
     * 打开图片视频选择页面（单选）。页面启动后只能选择图片文件。
     * @param fragment 在Fragment中使用时无需Activity实例，只需传入当前的Fragment实例即可。
     * @param maxDuration 最大时长显示，当选择视频是，该参数用于限制选择的最大视频时长，单位秒，0表示不限制时长，默认不限制。
     * @param result 选中结果，由于是单选，是有意当用户点击了某个图片或视频的选择框后就会将这个图片或视频回调给你。
     */
    @Deprecated("Please use withSysAlbum or withSelectorAlbum method.", replaceWith = ReplaceWith("PhotoSelector.withSelectorAlbum(fragment, AlbumType.PHOTO_VIDEO){\nselect { photo ->\n\n}\n}", "com.kelin.photoselector.model.AlbumType"))
    fun openPictureSelectorSingle(fragment: Fragment, maxDuration: Int = 0, result: SinglePhotoCallback) {
        fragment.activity?.also { activity ->
            realOpenSelector<Photo>(activity, false, AlbumType.PHOTO_VIDEO, 1, ID_SINGLE, 0F, maxDuration, result)
        }
    }

    /**
     * 打开图片和视频的选择页面。页面启动后即能选择图片文件也能选择视频文件。
     * @param fragment 在Fragment中使用时无需Activity实例，只需传入当前的Fragment实例即可。
     * @param maxLength 最大数量，用于设置最多可选择多少个图片和视频。
     * @param id    为本次选择设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Activity的hashCode，即表示当前Activity中不允许有重复的图片和视频被选择，
     * 如果当前不是第一次打开图片和视频选择且之前完成过选择(完成是指点击了图片和视频选择页面的完成按钮)，那么之前选择过的视频默认会被勾选(即数据回显)。如果您的页面中有多处需要选择
     * 图片和视频的地方且去重逻辑互不影响，那么您需要手动为每一处的打开设置不同的id。如果您不希望开启自动去重的功能，那么您可以将该参数设置为ID_REPEATABLE。
     * @param maxDuration 最大时长显示，当选择视频是，该参数用于限制选择的最大视频时长，单位秒，0表示不限制时长，默认不限制。
     * @param result 选中结果，当用户点击了完成按钮后会将用户已经勾选的所有图片和视频(包括数据回显选中的图片和视频)回调给您。
     */
    @Deprecated("Please use withSysAlbum or withSelectorAlbum method.", replaceWith = ReplaceWith("PhotoSelector.withSelectorAlbum(fragment, AlbumType.PHOTO_VIDEO){\nselectAll(maxLength) { photos ->\n\n}\n}", "com.kelin.photoselector.model.AlbumType"))
    fun openPictureSelector(fragment: Fragment, maxLength: Int = defMaxLength, id: Int = fragment.hashCode(), maxDuration: Int = 0, result: MutablePhotoCallabck) {
        fragment.activity?.also { activity ->
            if (id != ID_REPEATABLE && id != ID_SINGLE) {
                fragment.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
            }
            realOpenSelector<List<Photo>>(activity, false, AlbumType.PHOTO_VIDEO, maxLength, id, 0F, maxDuration, result)
        }
    }

    /**
     * 打开图片视频选择页面（单选）。页面启动后只能选择图片文件。
     * @param context 在Activity中使用时您需要传入当前Activity的实例。
     * @param maxDuration 最大时长显示，当选择视频是，该参数用于限制选择的最大视频时长，单位秒，0表示不限制时长，默认不限制。
     * @param result 选中结果，由于是单选，是有意当用户点击了某个图片或视频的选择框后就会将这个图片或视频回调给你。
     */
    @Deprecated("Please use withSysAlbum or withSelectorAlbum method.", replaceWith = ReplaceWith("PhotoSelector.withSelectorAlbum(context, AlbumType.PHOTO_VIDEO){\nselect { photo ->\n\n}\n}", "com.kelin.photoselector.model.AlbumType"))
    fun openPictureSelectorSingle(context: Context, maxDuration: Int = 0, result: SinglePhotoCallback) {
        realOpenSelector<Photo>(context, false, AlbumType.PHOTO_VIDEO, 1, ID_SINGLE, 0F, maxDuration, result)
    }

    /**
     * 打开图片和视频的选择页面。页面启动后即能选择图片文件也能选择视频文件。
     * @param context 在Activity中使用时您需要传入当前Activity的实例。
     * @param maxLength 最大数量，用于设置最多可选择多少个图片和视频。
     * @param id    为本次选择设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Activity的hashCode，即表示当前Activity中不允许有重复的图片和视频被选择，
     * 如果当前不是第一次打开图片和视频选择且之前完成过选择(完成是指点击了图片和视频选择页面的完成按钮)，那么之前选择过的视频默认会被勾选(即数据回显)。如果您的页面中有多处需要选择
     * 图片和视频的地方且去重逻辑互不影响，那么您需要手动为每一处的打开设置不同的id。如果您不希望开启自动去重的功能，那么您可以将该参数设置为ID_REPEATABLE。
     * @param maxDuration 最大时长显示，当选择视频是，该参数用于限制选择的最大视频时长，单位秒，0表示不限制时长，默认不限制。
     * @param result 选中结果，当用户点击了完成按钮后会将用户已经勾选的所有图片和视频(包括数据回显选中的图片和视频)回调给您。
     */
    @Deprecated("Please use withSysAlbum or withSelectorAlbum method.", replaceWith = ReplaceWith("PhotoSelector.withSelectorAlbum(context, AlbumType.PHOTO_VIDEO){\nselectAll(maxLength) { photos ->\n\n}\n}", "com.kelin.photoselector.model.AlbumType"))
    fun openPictureSelector(context: Context, maxLength: Int = defMaxLength, id: Int = context.hashCode(), maxDuration: Int = 0, result: MutablePhotoCallabck) {
        if (id != ID_REPEATABLE && id != ID_SINGLE && context is LifecycleOwner) {
            context.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
        }
        realOpenSelector<List<Photo>>(context, false, AlbumType.PHOTO_VIDEO, maxLength, id, 0F, maxDuration, result)
    }

    /**
     * 使用系统相册。
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun withSysAlbum(context: Context, album: AlbumType, option: SystemAlbumOption.() -> Unit) {
        option(SystemAlbumOption(context, album))
    }

    /**
     * 使用系统相册。
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun withSysAlbum(context: Context, album: AlbumType): SystemAlbumOption {
        return SystemAlbumOption(context, album)
    }

    /**
     * 使用系统相册。
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun withSysAlbum(fragment: Fragment, album: AlbumType, option: SystemAlbumOption.() -> Unit) {
        fragment.activity?.also { option(SystemAlbumOption(it, album)) }
    }

    /**
     * 使用系统相册。
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun withSysAlbum(fragment: Fragment, album: AlbumType): SystemAlbumOption? {
        return fragment.activity?.let { SystemAlbumOption(it, album) }
    }

    /**
     * 使用自定义相册。
     */
    fun withSelectorAlbum(context: Context, album: AlbumType, option: SelectorAlbumOption.() -> Unit) {
        option(SelectorAlbumOption(context, album))
    }

    /**
     * 使用自定义相册。
     */
    fun withSelectorAlbum(context: Context, album: AlbumType): SelectorAlbumOption {
        return SelectorAlbumOption(context, album)
    }

    /**
     * 使用自定义相册。
     */
    fun withSelectorAlbum(fragment: Fragment, album: AlbumType, option: SelectorAlbumOption.() -> Unit) {
        fragment.activity?.also { option(SelectorAlbumOption(it, album)) }
    }

    /**
     * 使用自定义相册。
     */
    fun withSelectorAlbum(fragment: Fragment, album: AlbumType): SelectorAlbumOption? {
        return fragment.activity?.let { SelectorAlbumOption(it, album) }
    }

    internal fun <R> realOpenSelector(context: Context, useSys: Boolean, albumType: AlbumType, maxLength: Int, id: Int, maxSize: Float, maxDuration: Int, result: (photo: R?) -> Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            OkPermission.permission_group.EXTERNAL_STORAGE
        }
        attachCallback(context, PermissionCallbackFactory(permission)) { ctx, r ->
            if (r) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && useSys) {
                    attachCallback(ctx, Android12SelectPictureCallbackFactory<R>(albumType, maxLength)) { _, photos ->
                        result(photos)
                    }
                } else {
                    attachCallback(ctx, SelectPictureCallbackFactory<R>(albumType, maxLength, id, maxSize, maxDuration)) { _, photos ->
                        result(photos)
                    }
                }
            }
        }
    }

    /**
     * 打开图片和视频的预览页面。
     * @param context Activity的Context。
     * @param pictures 要预览的所有图片，可以是图片也可以是视频；可以是本地的也可以是网络的。
     * @param select 默认选中的页，例如你一共有5张图片需要预览，但是你希望默认加载第三张图片那么该参数你需要传2(0表示第一张)。
     */
    fun openPicturePreviewPage(context: Context, pictures: List<Photo>, select: Int = 0) {
        if (context is Activity && pictures.isNotEmpty()) {
            PhotoSelectorActivity.startPreview(context, pictures, select)
        }
    }

    /**
     * 根据索引移除选中，当你使用了自动去重功能后，如果你在图片选择完毕后对用户提供了删除功能，那么你需要在用户删除后调用该方法同步删除。
     * @param fragment 如果你选择图片时没有手动设置id且传入的第一个参数是fragment那么就同步删除的时候也需要传入fragment。
     * @param position 要删除的索引(删除第几个)，索引从0开始。
     */
    fun removeSelected(fragment: Fragment, position: Int) {
        removeSelected(fragment.hashCode(), position)
    }

    /**
     * 根据索引移除选中，当你使用了自动去重功能后，如果你在图片选择完毕后对用户提供了删除功能，那么你需要在用户删除后调用该方法同步删除。
     * @param context 如果你选择图片时没有手动设置id且传入的第一个参数是context那么就同步删除的时候也需要传入context。
     * @param position 要删除的索引(删除第几个)，索引从0开始。
     */
    fun removeSelected(context: Context, position: Int) {
        removeSelected(context.hashCode(), position)
    }

    /**
     * 根据索引移除选中，当你使用了自动去重功能后，如果你在图片选择完毕后对用户提供了删除功能，那么你需要在用户删除后调用该方法同步删除。
     * @param id 选择图片或视频时的id。
     * @param position 要删除的索引(删除第几个)，索引从0开始。
     */
    fun removeSelected(id: Int, position: Int) {
        DistinctManager.instance.remove(id, position)
    }

    /**
     * 根据filePath移除选中，当你使用了自动去重功能后，如果你在图片选择完毕后对用户提供了删除功能，那么你需要在用户删除后调用该方法同步删除。
     * @param fragment 如果你选择图片时没有手动设置id且传入的第一个参数是fragment那么就同步删除的时候也需要传入fragment。
     * @param uri 要删除的图片或视频文件的uri(文件path路径)。
     */
    fun removeSelected(fragment: Fragment, uri: String) {
        removeSelected(fragment.hashCode(), uri)
    }

    /**
     * 根据filePath移除选中，当你使用了自动去重功能后，如果你在图片选择完毕后对用户提供了删除功能，那么你需要在用户删除后调用该方法同步删除。
     * @param context 如果你选择图片时没有手动设置id且传入的第一个参数是context那么就同步删除的时候也需要传入context。
     * @param uri 要删除的图片或视频文件的uri(文件path路径)。
     */
    fun removeSelected(context: Context, uri: String) {
        removeSelected(context.hashCode(), uri)
    }

    /**
     * 根据filePath移除选中，当你使用了自动去重功能后，如果你在图片选择完毕后对用户提供了删除功能，那么你需要在用户删除后调用该方法同步删除。
     * @param id 选择图片或视频时的id。
     * @param uri 要删除的图片或视频文件的uri(文件path路径)。
     */
    fun removeSelected(id: Int, uri: String) {
        DistinctManager.instance.remove(id, uri)
    }

    /**
     * 调用系统的播放功能播放视频。
     * @param context 需要Activity的Context。
     * @param photo Photo对象。
     */
    fun playVideo(context: Context, photo: Photo) {
        playVideo(context, photo.uri)
    }

    /**
     * 调用PhotoSelector的视频播放功能播放视频。
     * @param context 需要Activity的Context。
     * @param uri 视频文件的uri地址，可以是网络上的url路径也可以是本地的视频文件地址。
     */
    fun playVideo(context: Context, uri: String) {
        if (context is Activity && uri.isNotEmpty() && uri.length > 12) {
            PhotoSelectorActivity.playVideo(context, uri)
        }
    }

    /**
     * 调用系统的播放功能播放视频。
     * @param context 需要Activity的Context。
     * @param photo Photo对象。
     */
    fun playVideoWithSystem(context: Context, photo: Photo) {
        playVideoWithSystem(
            context, photo.getUri(),
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(photo.uri)) ?: "video/*"
        )
    }

    /**
     * 调用系统的播放功能播放视频。
     * @param context 需要Activity的Context。
     * @param uri 视频文件的uri地址。
     */
    fun playVideoWithSystem(context: Context, uri: Uri, type: String = "video/*") {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                uri,
                type
            )
        })
    }
}
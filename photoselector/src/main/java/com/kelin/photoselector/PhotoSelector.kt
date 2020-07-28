package com.kelin.photoselector

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.kelin.okpermission.OkActivityResult
import com.kelin.okpermission.OkPermission
import com.kelin.photoselector.cache.DistinctManager
import com.kelin.photoselector.model.*
import com.kelin.photoselector.model.AlbumType
import com.kelin.photoselector.model.Picture
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

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

    internal const val DEFAULT_PICTURE_DIR = "photoSelector"

    /**
     * 相册命名改变器。
     */
    private var albumNameTransformer: NameTransformer = AlbumNameTransformer()

    private var fileProvider: String? = null
    private var defMaxCount: Int = 9

    /**
     * 拍照和录像时的视频或图片的存储路径。
     */
    private var pictureDir: String = DEFAULT_PICTURE_DIR

    private val requireFileProvider: String
        get() = fileProvider ?: throw NullPointerException("You need call the init method first to set fileProvider.")

    /**
     * 初始化。
     * @param provider 用于适配在7.0及以上Android版本的文件服务。
     */
    fun init(context: Context, provider: String, maxCount: Int = 9) {
        defMaxCount = maxCount
        fileProvider = provider
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
    internal fun transformAlbumName(name: String): String {
        return albumNameTransformer.transform(name)
    }

    /**
     * 拍摄照片。
     * @param fragment 在Fragment中使用时无需Activity实例，只需传入当前的Fragment实例即可。
     * @param id    为本次拍照设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Activity的hashCode，即表示当前Activity中不允许有重复的图片被选择，
     * 如果你的页面有多张照片需要上传那么本次拍照会在id相同的选择中复显。
     * @param targetFile 拍摄的照片要存放的临时目标文件，默认为空，如果为空则使用默认的目标路径。如果你需要自己指定则需要传入该值。
     * @param onResult 拍摄完成的回调，会将照片文件回调给您。
     */
    fun takePhoto(fragment: Fragment, id: Int = fragment.hashCode(), targetFile: File? = null, onResult: (photo: File?) -> Unit) {
        fragment.activity?.also { activity ->
            if (id != -1) {
                fragment.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
            }
            OkPermission.with(activity)
                .addDefaultPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .checkAndApply { granted, _ ->
                    if (granted) {
                        takePicture(activity, id, MediaStore.ACTION_IMAGE_CAPTURE, targetFile ?: File("${Environment.getExternalStorageDirectory().absolutePath}/${pictureDir}/", "${System.currentTimeMillis()}.jpg"), onResult)
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
        if (id != -1 && activity is LifecycleOwner) {
            activity.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
        }
        OkPermission.with(activity)
            .addDefaultPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
            .checkAndApply { granted, _ ->
                if (granted) {
                    takePicture(activity, id, MediaStore.ACTION_IMAGE_CAPTURE, targetFile ?: File("${Environment.getExternalStorageDirectory().absolutePath}/${pictureDir}/", "${System.currentTimeMillis()}.jpg"), onResult)
                }
            }
    }

    /**
     * 拍摄视频。
     * @param fragment 在Fragment中使用时无需Activity实例，只需传入当前的Fragment实例即可。
     * @param id    为本次拍照设置一个id，该id是去重逻辑的核心。可以不传，如果不传则默认为当前Activity的hashCode，即表示当前Activity中不允许有重复的视频被选择，
     * 如果你的页面有多个视频需要上传那么本次拍摄会在id相同的选择中复显。
     * @param targetFile 拍摄的视频要存放的临时目标文件，默认为空，如果为空则使用默认的目标路径。如果你需要自己指定则需要传入该值。
     * @param onResult 拍摄完成的回调，会将视频文件回调给您。
     */
    fun takeVideo(fragment: Fragment, id: Int = fragment.hashCode(), targetFile: File? = null, onResult: (photo: File?) -> Unit) {
        fragment.activity?.also { activity ->
            if (id != -1) {
                fragment.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
            }
            OkPermission.with(activity)
                .addDefaultPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .checkAndApply { granted, _ ->
                    if (granted) {
                        takePicture(activity, id, MediaStore.ACTION_VIDEO_CAPTURE, targetFile ?: File("${Environment.getExternalStorageDirectory().absolutePath}/${pictureDir}/", "${System.currentTimeMillis()}.mp4"), onResult)
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
        if (id != -1 && activity is LifecycleOwner) {
            activity.lifecycle.addObserver(DistinctManager.instance.tryNewCache(id))
        }
        OkPermission.with(activity)
            .addDefaultPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
            .checkAndApply { granted, _ ->
                if (granted) {
                    takePicture(activity, id, MediaStore.ACTION_VIDEO_CAPTURE, targetFile ?: File("${Environment.getExternalStorageDirectory().absolutePath}/${pictureDir}/", "${System.currentTimeMillis()}.mp4"), onResult)
                }
            }
    }

    private fun takePicture(activity: Activity, id: Int, action: String, targetFile: File, onResult: (photo: File?) -> Unit) {
        val intent = Intent(action)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (targetFile.exists()) {
            targetFile.delete()
        } else if (targetFile.parentFile?.exists() == false) {
            targetFile.parentFile?.mkdirs()
        }
        val uri = targetFile.toUri(activity, requireFileProvider)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        OkActivityResult.startActivity(activity, intent) { resultCode ->
            if (resultCode == Activity.RESULT_OK && targetFile.exists()) {
                val isVideoAction = action == MediaStore.ACTION_VIDEO_CAPTURE
                MediaScannerConnection.scanFile(activity, arrayOf(targetFile.absolutePath), arrayOf(if (isVideoAction) "video/mp4" else "image/jpeg"), null)
                val duration = if (isVideoAction) {
                    MediaMetadataRetriever().let { m ->
                        m.setDataSource(targetFile.absolutePath)
                        m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 1
                    }
                } else {
                    0
                }
                DistinctManager.instance.addSelected(id, Picture(targetFile.absolutePath, targetFile.length(), if (isVideoAction) PictureType.VIDEO else PictureType.PHOTO, formatDuration(duration), SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())))
                onResult(targetFile)
            } else {
                onResult(null)
            }
        }
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
    fun openPhotoSelector(fragment: Fragment, maxCount: Int = defMaxCount, id: Int = fragment.hashCode(), result: (photos: List<Photo>) -> Unit) {
        fragment.activity?.also { activity ->
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
    fun openPhotoSelector(context: Context, maxCount: Int = defMaxCount, id: Int = context.hashCode(), result: (photos: List<Photo>) -> Unit) {
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
    fun openVideoSelector(fragment: Fragment, maxCount: Int = defMaxCount, id: Int = fragment.hashCode(), result: (photos: List<Photo>) -> Unit) {
        fragment.activity?.also { activity ->
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
    fun openVideoSelector(context: Context, maxCount: Int = defMaxCount, id: Int = context.hashCode(), result: (photos: List<Photo>) -> Unit) {
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
    fun openPictureSelector(fragment: Fragment, maxCount: Int = defMaxCount, id: Int = fragment.hashCode(), result: (photos: List<Photo>) -> Unit) {
        fragment.activity?.also { activity ->
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
    fun openPictureSelector(context: Context, maxCount: Int = defMaxCount, id: Int = context.hashCode(), result: (photos: List<Photo>) -> Unit) {
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
    fun playVideoWithSystem(context: Context, photo: Photo) {
        playVideoWithSystem(
            context, photo.getUri(context) ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || photo.uri.startsWith("http")) {
                Uri.parse(photo.uri)
            } else {
                Uri.fromFile(File(photo.uri))
            },
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

    internal fun formatDuration(duration: Long): String {
        return when {
            duration == 0L -> {
                ""
            }
            duration < 1000 -> {
                "00:01"
            }
            else -> {
                (duration / 1000).let { d ->
                    if (d > 3600) {
                        "%02d:%02d:%02d".format(d / 3600, d / 60 % 60, d % 60)
                    } else {
                        "%02d:%02d".format(d / 60 % 60, d % 60)
                    }
                }
            }
        }
    }
}

fun File.toUri(context: Context, provider: String = "${context.packageName}.fileProvider"): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(context, provider, this)
    } else {
        Uri.fromFile(this)
    }
}
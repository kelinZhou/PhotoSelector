package com.kelin.photoselector.loader

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.SharedPreferences
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.kelin.photoselector.PhotoSelector
import com.kelin.photoselector.R
import com.kelin.photoselector.model.Album
import com.kelin.photoselector.model.Picture
import com.kelin.photoselector.model.PictureType
import com.kelin.photoselector.model.formatToDurationString
import com.kelin.photoselector.widget.AlbumsDialog
import java.io.File
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.concurrent.Volatile

internal typealias OnAlbumSelectedListener = (album: Album) -> Unit

internal typealias OnAlbumMorePictureListener = (pictures: List<Picture>) -> Unit

/**
 * **描述:** 相册管理器。。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2024/6/5 09:42
 *
 * **版本:** v 1.0.0
 */
internal class AlbumManager(activity: Activity, lifecycleOwner: LifecycleOwner, private val sp: SharedPreferences, private val maxSize: Float, private val maxDuration: Long) : LifecycleEventObserver {

    companion object {
        private const val WHAT_DISPATCH_DEF_ALBUM = 0x0010
        private const val WHAT_DISPATCH_MORE_PICTURES = 0x0011
    }

    private val context = if (activity.isDestroyed) {
        WeakReference<Activity>(null)
    } else {
        WeakReference(activity)
    }

    private var currentAlbumName: String? = null
        set(value) {
            field = value
            sp.edit().putString("kelin_photo_selector_selected_album_name", value).apply()
        }
        get() = field ?: sp.getString("kelin_photo_selector_selected_album_name", "")


    private val albumsDialog: AlbumsDialog?
        get() = context.get()?.let { activity ->
            AlbumsDialog(activity) {
                currentAlbumName = it.name
                onAlbumSelectedListener?.invoke(it)
            }
        }


    private val handler by lazy {
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    WHAT_DISPATCH_DEF_ALBUM -> {
                        val albums = innerAlbums.toTypedArray()
                        if (albums.isNotEmpty()) {
                            val defAlbum = albums.find { a -> a.name == sp.getString("kelin_photo_selector_selected_album_name", "") } ?: albums.first()
                            currentAlbumName = defAlbum.name
                            onAlbumSelectedListener?.invoke(defAlbum)
                        }
                    }

                    WHAT_DISPATCH_MORE_PICTURES -> {
                        @Suppress("UNCHECKED_CAST")
                        (msg.obj as? List<Picture>)?.also { onAlbumMorePicturesListener?.invoke(it) }
                    }
                }
            }
        }
    }

    private val dataFormat by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.CHINA) }

    private val executor by lazy { Executors.newSingleThreadExecutor() }

    private val innerAlbums: MutableList<Album> by lazy { ArrayList() }

    private var innerCursor: Cursor? = null

    @Volatile
    private var onAlbumSelectedListener: OnAlbumSelectedListener? = null

    @Volatile
    private var onAlbumMorePicturesListener: OnAlbumMorePictureListener? = null

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    /**
     * 解析相册图片。
     * @param cursor 游标。
     */
    fun parsePicture(cursor: Cursor, albumSelectedListener: OnAlbumSelectedListener, albumMorePicturesListener: OnAlbumMorePictureListener) {
        if (!cursor.isClosed && cursor.count > 0) {
            innerCursor = cursor
            onAlbumSelectedListener = albumSelectedListener
            onAlbumMorePicturesListener = albumMorePicturesListener
            executor.execute(this::innerParsePicture)
        }
    }

    @SuppressLint("SdCardPath", "Range")
    private fun innerParsePicture() {
        innerCursor?.run {
            moveToFirst()
            var result = ArrayList<Picture>()
            var page = 0
            do {
                val path = getString(getColumnIndex(MediaStore.Files.FileColumns.DATA))
                val name = getString(getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME))
                val file = path.let { if (path.isNullOrEmpty()) null else File(path) }
                val size = getLong(getColumnIndex(MediaStore.Files.FileColumns.SIZE))
                val type = getInt(getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)).let {
                    if (it == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                        PictureType.VIDEO
                    } else {
                        PictureType.PHOTO
                    }
                }
                val isVideo = type == PictureType.VIDEO
                val duration = if (isVideo) {
                    getLong(getColumnIndex(MediaStore.Files.FileColumns.DURATION)).let {
                        when {
                            it > 0 -> it
                            size >= 4096 -> { //但是视频太小的将会导致无法播放，所以这里过滤一下文件大小。
                                //有些手机的有些视频可能从数据库查不到视频长度，如果长度是0则认为没有查到，那么就用下面的方式重新获取一次视频长度。
                                MediaMetadataRetriever().let { m ->
                                    m.setDataSource(path)
                                    m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 1
                                }
                            }

                            else -> 0
                        }
                    }
                } else {
                    0
                }
                if (file?.exists() == true && (!isVideo || size >= 4096)) {  //判断文件存在并且如果是视频则视频必须大于4kb(小于4kb的视频可能无法播放)
                    val available = if (isVideo && maxDuration > 0) { //如果设置了最大时长并且是视频时则校验视频时长限制
                        duration <= maxDuration  //视频时长小于限制时则认为是满足条件的视频。
                    } else {  //在不需要校验视频时长限制时校验文件大小
                        maxSize <= 0 || size.let { (it + 5000) / 10000 / 100F } <= maxSize  //没设置文件大小或则文件大小符合要求时则认为是满足条件的图片。
                    }
                    if (available) {
                        //如果满足条件则被添加到结果中，否则不添加
                        result.add(
                            Picture(
                                file.absolutePath,
                                size,
                                type,
                                if (isVideo) duration.formatToDurationString() else "",
                                dataFormat.format(getLong(getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)) * 1000)
                            )
                        )
                    } else {
                        Log.d("PhotoSelector:", "过滤过大的或过长的图片视频：path=$path, name=$name, maxSize=${size / 1000000F}MB, duration=${duration.formatToDurationString()}")
                    }
                } else {
                    Log.d("PhotoSelector:", "照片或视频读取失败：path=$path, name=$name")
                }
                if (result.size >= 1000) {
                    parseAlbum(page, result)
                    result = ArrayList()
                    page++
                }
            } while (onAlbumMorePicturesListener != null && moveToNext())
            close()
            parseAlbum(page, result)
        }
    }

    /**
     * 解析相册。
     * @param page 页码，从0开始。
     * @param pictures 图片数据。
     */
    private fun parseAlbum(page: Int, pictures: ArrayList<Picture>) {
        context.get()?.also { activity ->
            if (pictures.isNotEmpty()) {
                val albumsResult = pictures.groupBy { it.parent }.mapTo(ArrayList()) {
                    val cover = it.value.first()
                    Album(
                        PhotoSelector.transformAlbumName(activity, cover.parentName),
                        cover,
                        it.value.toMutableList()
                    )
                }.apply {
                    val cover = pictures.first()
                    add(
                        0,
                        Album(
                            activity.getString(R.string.kelin_photo_selector_all),
                            cover,
                            pictures,
                            cover.rootDirName
                        )
                    )
                }
                dispatchAlbum(page, albumsResult)
            }
        }
    }

    /**
     * 分发相册。
     * @param page 当前页码，从0开始。
     * @param albums 相册列表。
     */
    private fun dispatchAlbum(page: Int, albums: List<Album>) {
        if (albums.isNotEmpty()) {
            if (page == 0) {
                innerAlbums.addAll(albums)
                Message.obtain(handler, WHAT_DISPATCH_DEF_ALBUM).sendToTarget()
            } else {
                albums.forEach { album ->
                    val cache = innerAlbums.find { it.name == album.name }
                    if (cache == null) {
                        innerAlbums.add(album)
                    } else {
                        cache.pictures.addAll(album.pictures)
                        if (album.name == currentAlbumName) {
                            Message.obtain(handler, WHAT_DISPATCH_MORE_PICTURES, album.pictures).sendToTarget()
                        }
                    }
                }
            }
        }
    }

    fun onSelectAlbums() {
        albumsDialog?.run {
            if (!isShowing) {
                show(innerAlbums, currentAlbumName)
            }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            albumsDialog?.run {
                if (isShowing) {
                    dismiss()
                }
            }
            context.clear()
            onAlbumSelectedListener = null
            onAlbumMorePicturesListener = null
            executor.shutdown()
            handler.removeCallbacksAndMessages(null)
        }
    }
}
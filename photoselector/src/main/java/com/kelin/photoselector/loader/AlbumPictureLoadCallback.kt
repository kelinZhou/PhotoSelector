package com.kelin.photoselector.loader

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.kelin.photoselector.PhotoSelector
import com.kelin.photoselector.R
import com.kelin.photoselector.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * **描述:** LoadCallback的具体实现，相册加载逻辑的具体处理。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/10 10:05 AM
 *
 * **版本:** v 1.0.0
 */
internal class AlbumPictureLoadCallback(private val context: Context, private val onLoaded: (result: List<Album>) -> Unit) : LoaderManager.LoaderCallbacks<Cursor> {

    private val dataFormat by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.CHINA) }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(
            context,
            MediaStore.Files.getContentUri("external"),
            arrayOf(
                FileColumns._ID,
                FileColumns.DATA,
                FileColumns.MEDIA_TYPE,
                FileColumns.DISPLAY_NAME,
                FileColumns.DATE_ADDED,
                FileColumns.DURATION,
                FileColumns.SIZE,
                FileColumns.MIME_TYPE,
                FileColumns.DATE_MODIFIED
            ),
            " ${FileColumns.SIZE} > 0 AND ${AlbumType.typeOf(id).query}",
            null,
            "${FileColumns.DATE_MODIFIED} DESC"
        )
    }

    @SuppressLint("SdCardPath")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            val result = ArrayList<Picture>()
            do {
                val path = cursor.getString(cursor.getColumnIndex(FileColumns.DATA))
                val name = cursor.getString(cursor.getColumnIndex(FileColumns.DISPLAY_NAME))
                val file = path.let { if (path.isNullOrEmpty()) null else File(path) }
                val size = cursor.getLong(cursor.getColumnIndex(FileColumns.SIZE))
                val type = cursor.getInt(cursor.getColumnIndex(FileColumns.MEDIA_TYPE)).let {
                    if (it == FileColumns.MEDIA_TYPE_VIDEO) {
                        PictureType.VIDEO
                    } else {
                        PictureType.PHOTO
                    }
                }
                val isVideo = type == PictureType.VIDEO
                val duration = if (isVideo) {
                    cursor.getLong(cursor.getColumnIndex(FileColumns.DURATION)).let {
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
                if (file?.exists() == true && (type == PictureType.PHOTO || size >= 4096)) {
                    result.add(
                        Picture(
                            file.absolutePath,
                            size,
                            type,
                            if (isVideo) duration.formatToDurationString() else "",
                            dataFormat.format(cursor.getLong(cursor.getColumnIndex(FileColumns.DATE_MODIFIED)) * 1000)
                        )
                    )
                } else {
                    Log.d("PhotoSelector:", "照片或视频读取失败：path=$path, name=$name")
                }
            } while (cursor.moveToNext())
            onLoaded(
                if (result.isEmpty()) {
                    emptyList()
                } else {
                    result.groupBy { it.parent }.mapTo(ArrayList()) {
                        val cover = it.value.first()
                        Album(
                            PhotoSelector.transformAlbumName(context, cover.parentName),
                            cover,
                            it.value
                        )
                    }.apply {
                        val cover = result.first()
                        add(
                            0,
                            Album(
                                context.getString(R.string.kelin_photo_selector_all),
                                cover,
                                result,
                                cover.rootDirName
                            )
                        )
                    }
                }
            )
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
    }
}
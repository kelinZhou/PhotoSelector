package com.kelin.photoselector.loader

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
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
import com.kelin.photoselector.model.*
import java.io.File

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
                FileColumns.MIME_TYPE
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
                val path = cursor.getString(cursor.getColumnIndexOrThrow(FileColumns.DATA))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME))
                val file = path.let { if (path.isNullOrEmpty()) null else File(path) }
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(FileColumns.SIZE))
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)).let {
                    if (it == FileColumns.MEDIA_TYPE_VIDEO) {
                        PictureType.VIDEO
                    } else {
                        PictureType.PHOTO
                    }
                }
                val isVideo = type == PictureType.VIDEO
                val duration = if (isVideo) {
                    cursor.getLong(cursor.getColumnIndexOrThrow(FileColumns.DURATION)).let {
                        if (it > 0) {
                            it
                        } else {//有些手机的有些视频可能从数据库查不到视频长度，如果长度是0则认为没有查到，那么就用下面的方式重新获取一次视频长度。
                            MediaMetadataRetriever().let { m ->
                                m.setDataSource(path)
                                m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 1
                            }
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
                            if (isVideo) formatDuration(duration) else ""
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
                            PhotoSelector.transformAlbumName(cover.parentName),
                            cover,
                            it.value
                        )
                    }.apply {
                        val cover = result.first()
                        add(
                            0,
                            Album(
                                "全部",
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

    private fun formatDuration(duration: Long): String {
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

    override fun onLoaderReset(loader: Loader<Cursor>) {
    }
}
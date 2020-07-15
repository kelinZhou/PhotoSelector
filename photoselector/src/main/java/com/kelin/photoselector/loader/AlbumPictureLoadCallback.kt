package com.kelin.photoselector.loader

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
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
            val result = ArrayList<PictureWrapper>()
            do {
                val path = cursor.getString(cursor.getColumnIndexOrThrow(FileColumns.DATA))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME))
                val file = path.let { if (path.isNullOrEmpty()) null else File(path) }
                if (file?.exists() == true) {
                    result.add(
                        PictureWrapper(
                            Picture(
                                file.absolutePath,
                                cursor.getLong(cursor.getColumnIndexOrThrow(FileColumns.SIZE)),
                                cursor.getInt(cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)).let {
                                    if (it == FileColumns.MEDIA_TYPE_VIDEO) {
                                        PictureType.VIDEO
                                    } else {
                                        PictureType.PHOTO
                                    }
                                },
                                formatDuration(cursor.getLong(cursor.getColumnIndexOrThrow(FileColumns.DURATION)))
                            )
                        )
                    )
                } else {
                    Log.d("PhotoSelector:", "照片或视频读取失败：path=$path, name=$name")
                }
            } while (cursor.moveToNext())
            onLoaded(result.groupBy { it.picture.parent }.mapTo(ArrayList()) {
                val cover = it.value.first().picture
                Album(
                    PhotoSelector.transformAlbumName(cover.parentName),
                    cover,
                    it.value
                )
            }.apply {
                val cover = result.first().picture
                add(
                    0,
                    Album(
                        "全部",
                        cover,
                        result,
                        cover.rootDirName
                    )
                )
            })
        }
    }

    private fun formatDuration(duration: Long): String {
        Log.d("=========duration:", duration.toString())
        return if (duration <= 1000) {
            ""
        } else {
            (duration / 1000).let { d ->
                if (d > 3600) {
                    "%02d:%02d:%02d".format(d / 3600, d / 60 % 60, d % 60)
                } else {
                    "%02d:%02d".format(d / 60 % 60, d % 60)
                }
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
    }
}
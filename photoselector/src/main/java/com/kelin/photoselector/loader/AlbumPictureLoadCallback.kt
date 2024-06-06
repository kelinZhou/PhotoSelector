package com.kelin.photoselector.loader

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.kelin.photoselector.model.*

/**
 * **描述:** LoadCallback的具体实现，相册加载逻辑的具体处理。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/10 10:05 AM
 *
 * **版本:** v 1.0.0
 */
internal class AlbumPictureLoadCallback(private val context: Context, private val onLoaded: (cursor: Cursor) -> Unit) : LoaderManager.LoaderCallbacks<Cursor> {

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

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
        if (cursor != null) {
            onLoaded(cursor)
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
    }
}
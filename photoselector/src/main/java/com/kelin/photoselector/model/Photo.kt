package com.kelin.photoselector.model

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.Serializable


/**
 * **描述:** 图片。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/13 6:45 PM
 *
 * **版本:** v 1.0.0
 */
interface Photo : Serializable {
    /**
     * 图片或视频的路径，可以是网络上的url，也可以是本地的path。
     */
    val uri: String

    /**
     * 是否是视频文件。
     */
    val isVideo: Boolean

    /**
     * 获取目标文件，如果当前是一个网络文件则会返回null，如果是本地文件则会返回File。
     */
    val targetFile: File?
        get() = if (uri.startsWith("http")) null else File(uri)

    /**
     * 图片或视频的Uri，可以是本地的也可以是网络上的。
     * @return 返回有效的Uri，可以返回空，如果返回空将使用url加载视频，建议在7.0及以上的系统中使用FileProvider。
     */
    fun getUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || !isLocalFile) {
            Uri.parse(uri)
        } else {
            Uri.fromFile(File(uri))
        }
    }

    /**
     * 判断是否是本地文件。
     */
    val isLocalFile: Boolean
        get() = !uri.startsWith("http", true)
}

internal fun Long.formatToDurationString(): String {
    return when {
        this == 0L -> {
            "00:00"
        }

        this < 1000 -> {
            "00:01"
        }

        else -> {
            ((this + 500) / 1000).let { d ->
                if (d > 3600) {
                    "%02d:%02d:%02d".format(d / 3600, d / 60 % 60, d % 60)
                } else {
                    "%02d:%02d".format(d / 60 % 60, d % 60)
                }
            }
        }
    }
}

fun Photo.toUri(context: Context, provider: String = "${context.packageName}.fileProvider"): Uri? {
    return targetFile?.toUri(context, provider)
}


fun File.toUri(context: Context, provider: String = "${context.packageName}.fileProvider"): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(context, provider, this)
    } else {
        Uri.fromFile(this)
    }
}

fun Uri?.toPhoto(context: Context, isVideo: Boolean? = null): Photo? {
    return this?.let { uri ->
        var realPath = ""
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                realPath = cursor.getString(columnIndex)
            }
        } finally {
            cursor?.close()
        }
        PhotoImpl(realPath, isVideo)
    }
}

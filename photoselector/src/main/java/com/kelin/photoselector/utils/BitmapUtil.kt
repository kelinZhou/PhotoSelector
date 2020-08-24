package com.kelin.photoselector.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.kelin.photoselector.PhotoSelector
import com.kelin.photoselector.model.Picture
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.math.max


/**
 * **描述:** Bitmap处理工具。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/31 9:55 AM
 *
 * **版本:** v 1.0.0
 */

internal fun Picture.compressAndRotateByDegree() {
    val file = File("${PhotoSelector.requireCacheDir}${name}")
    if (file.exists() && file.length() >= 1024) {
        onComposeFinished("${PhotoSelector.requireCacheDir}${name}")
    } else {
        Executors.newSingleThreadExecutor().execute {
            try {
                val orientation: Int = ExifInterface(uri).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }.also { degree ->
                    if (degree != 0f) {
                        compress(1080, 1920, degree)?.let { bm ->
                            Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, Matrix().apply { postRotate(degree, bm.width / 2f, bm.height / 2f) }, true).also {
                                bm.recycle()
                            }
                        }
                    } else {
                        compress(1080, 1920, degree)
                    }?.apply {
                        onComposeFinished(writeToFile("${PhotoSelector.requireCacheDir}${name}"))
                        recycle()
                    } ?: onComposeFinished()//保证onComposeFinished()一定会被回调。
                }
            } catch (e: Exception) {
                onComposeFinished() //保证onComposeFinished()一定会被回调。
                e.printStackTrace()
            }
        }
    }
}

internal fun Picture.compress(screenWidth: Int, screenHeight: Int, degree: Float): Bitmap? {
    val opts = BitmapFactory.Options()
    opts.inJustDecodeBounds = true
    BitmapFactory.decodeFile(uri, opts)
    return if (screenWidth > 0 && screenHeight > 0 && !opts.outMimeType.contains("gif")) {
        val width = opts.outWidth
        val height = opts.outHeight
        val vertical = if (degree == 0f || degree == 180f) { // 如果是竖立的图片
            height > width
        } else {
            width > height
        }
        val inSampleSize = if (vertical) {
            max(width / screenWidth, height / screenHeight)
        } else {
            max(height / screenWidth, width / screenHeight)
        }
        opts.inSampleSize = inSampleSize
        opts.inJustDecodeBounds = false
        BitmapFactory.decodeFile(uri, opts)
    } else {
        null
    }
}

internal fun Bitmap.writeToFile(targetPath: String): String? {
    return try {
        val targetFile = File(targetPath)
        if (!targetFile.exists()) {
            targetFile.parentFile?.apply {
                if (!exists()) {
                    mkdirs()
                }
            }
            if (!targetFile.exists()) {
                targetFile.createNewFile()
            }
        }
        val fos = FileOutputStream(targetPath)
        //通过io流的方式来压缩保存图片
        if (hasAlpha()) {
            compress(Bitmap.CompressFormat.PNG, 100, fos)
        } else {
            compress(Bitmap.CompressFormat.JPEG, 70, fos)
        }
        fos.flush()
        fos.close()
        if (targetFile.exists()) {
            targetPath
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

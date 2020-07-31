package com.kelin.photoselector.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.AsyncTask
import androidx.exifinterface.media.ExifInterface
import com.kelin.photoselector.model.Photo
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor


/**
 * **描述:** Bitmap处理工具。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/31 9:55 AM
 *
 * **版本:** v 1.0.0
 */

fun Photo.rotateByDegree() {
    Executors.newSingleThreadExecutor().execute {
        try {
            val exifInterface = ExifInterface(uri)
            val orientation: Int = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }.also { degree ->
                if (degree != 0f) {
                    BitmapFactory.decodeFile(uri)?.let { bm ->
                        Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, Matrix().apply { postRotate(degree, bm.width / 2f, bm.height / 2f) }, true)?.writeToFile(uri)
                        bm.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun Bitmap.writeToFile(targetPath: String) {
    try {
        val fos = FileOutputStream(File(targetPath))
        //通过io流的方式来压缩保存图片
        if (hasAlpha()) {
            compress(Bitmap.CompressFormat.PNG, 100, fos)
        } else {
            compress(Bitmap.CompressFormat.JPEG, 100, fos)
        }
        fos.flush()
        fos.close()
        recycle()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

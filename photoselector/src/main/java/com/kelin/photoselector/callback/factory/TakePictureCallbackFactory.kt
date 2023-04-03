package com.kelin.photoselector.callback.factory

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.os.*
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import com.kelin.okpermission.OkActivityResult
import com.kelin.photoselector.PhotoSelector
import com.kelin.photoselector.cache.DistinctManager
import com.kelin.photoselector.callback.BaseCallback
import com.kelin.photoselector.callback.LeakProofCallback
import com.kelin.photoselector.model.Picture
import com.kelin.photoselector.model.PictureType
import com.kelin.photoselector.model.formatToDurationString
import com.kelin.photoselector.model.toUri
import com.kelin.photoselector.widget.ProgressDialog
import com.kelin.photoselector.utils.compressAndRotateByDegree
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * **描述:** 拍照录像回调工厂。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/8/7 10:10 AM
 *
 * **版本:** v 1.0.0
 */
class TakePictureCallbackFactory(private val id: Int, private val action: String, private val targetFile: File, private val fileProvider: String, val isVideoAction: Boolean = action == MediaStore.ACTION_VIDEO_CAPTURE) : CallbackFactory<File?> {

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private var progressDialog: ProgressDialog? = null

    override fun createCallback(): LeakProofCallback<File?> {
        return TakePictureCallback()
    }

    private inner class TakePictureCallback : BaseCallback<File?>() {
        override fun onAttach(context: Context) {
            val intent = Intent(action)
            // Ensure that there's a camera activity to handle the intent
            // 官方注释，确保有一个活动来打开相机意图
            if (intent.resolveActivity(context.packageManager) != null) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                if (targetFile.exists()) {
                    targetFile.delete()
                } else if (targetFile.parentFile?.exists() == false) {
                    targetFile.parentFile?.mkdirs()
                }
                val uri = targetFile.toUri(context, fileProvider)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                OkActivityResult.startActivityForCode(context as Activity, intent) { resultCode ->
                    if (resultCode == Activity.RESULT_OK && targetFile.exists() && contextOrNull != null) {
                        val duration = if (isVideoAction) {
                            MediaMetadataRetriever().let { m ->
                                m.setDataSource(targetFile.absolutePath)
                                m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 1
                            }
                        } else {
                            0
                        }
                        val picture = Picture(
                            targetFile.absolutePath,
                            targetFile.length(),
                            if (isVideoAction) PictureType.VIDEO else PictureType.PHOTO, duration.formatToDurationString(),
                            SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
                        )
                        if (PhotoSelector.isAutoCompress && !isVideoAction) {
                            picture.compressAndRotateByDegree()
                        }
                        insertToAlbum()
                        onTakePictureFinished(picture)
                    } else {
                        callback(null)
                    }
                }
            }
        }

        private fun onTakePictureFinished(picture: Picture) {
            if (!PhotoSelector.isAutoCompress || picture.isComposeFinished) {
                if (progressDialog != null) {
                    progressDialog!!.dismiss()
                }
                DistinctManager.instance.addSelected(id, picture)
                callback(File(picture.uri))
            } else {
                if (progressDialog == null) {
                    progressDialog = ProgressDialog().also { dialog ->
                        val context = contextOrNull
                        val fm = if (context is FragmentActivity) {
                            context.supportFragmentManager
                        } else {
                            null
                        }
                        if (fm != null) {
                            dialog.show(fm, id.toString())
                        }
                    }
                }
                handler.postDelayed({
                    onTakePictureFinished(picture)
                }, 100)
            }
        }


        private fun insertToAlbum() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                MediaScannerConnection.scanFile(contextOrNull, arrayOf(targetFile.absolutePath), arrayOf(if (isVideoAction) "video/mp4" else "image/jpeg"), null)
            } else {
                contextOrNull?.also { context ->
                    context.contentResolver.also { resolver ->
                        val values = getVideoContentValues(targetFile)
                        resolver.insert(if (isVideoAction) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.also { uri ->
                            resolver.openOutputStream(uri)?.use { os ->
                                val `is` = FileInputStream(targetFile)
                                FileUtils.copy(`is`, os)
                                `is`.close()
                                os.close()
                            }
                        }
                    }
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        fun getVideoContentValues(file: File): ContentValues {
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            values.put(MediaStore.MediaColumns.MIME_TYPE, if (isVideoAction) "video/mp4" else "image/jpeg")
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
            return values
        }
    }
}
package com.kelin.photoselector.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.toBitmap
import coil.drawable.ScaleDrawable
import coil.target.Target
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.kelin.photoselector.databinding.ViewKelinPhotoSelectorPhotoTargetViewBinding

/**
 * **描述:** 用来显示图片的空间。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/20 10:35 AM
 *
 * **版本:** v 1.0.0
 */
class PhotoTargetView @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStylesAttr: Int = 0) : RelativeLayout(context, attrs, defStylesAttr) {

    private val vb by lazy { ViewKelinPhotoSelectorPhotoTargetViewBinding.inflate(LayoutInflater.from(context), this, false) }

    init {
        addView(vb.root)
    }

    internal val imageView: SubsamplingScaleImageView by lazy { vb.ivKelinPhotoSelectorPhotoView }

    internal val defImageView: AppCompatImageView by lazy { vb.ivKelinPhotoSelectorGifView }

    internal val progressBar: ProgressBar by lazy { vb.pbKelinPhotoSelectorProgress }

    val target by lazy { PhotoTarget(this) }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {

        // 获取 drawable 长宽
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight

        drawable.setBounds(0, 0, width, height)
        // 获取drawable的颜色格式
        val config = if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        // 创建bitmap
        val bitmap = Bitmap.createBitmap(width, height, config)
        // 创建bitmap画布
        val canvas = Canvas(bitmap)
        // 将drawable 内容画到画布中
        drawable.draw(canvas)
        return bitmap
    }

    override fun setOnClickListener(l: OnClickListener?) {
        vb.ivKelinPhotoSelectorPhotoView.setOnClickListener(l)
        vb.ivKelinPhotoSelectorGifView.setOnClickListener(l)
    }

    inner class PhotoTarget(private val target: PhotoTargetView) : Target {
        override fun onError(error: Drawable?) {
            target.progressBar.visibility = View.GONE
            target.imageView.visibility = View.GONE
            target.defImageView.apply {
                setImageDrawable(error)
                visibility = View.VISIBLE
            }
        }

        override fun onSuccess(result: Drawable) {
            target.progressBar.visibility = View.GONE
            if (result is ScaleDrawable) {
                target.imageView.visibility = View.GONE
                target.defImageView.apply {
                    setImageDrawable(result)
                    result.start()
                    visibility = View.VISIBLE
                }
            } else {
                target.defImageView.visibility = View.GONE
                target.imageView.apply {
                    setImage(ImageSource.bitmap(result.toBitmap()))
                    visibility = View.VISIBLE
                }
            }
        }
    }
}
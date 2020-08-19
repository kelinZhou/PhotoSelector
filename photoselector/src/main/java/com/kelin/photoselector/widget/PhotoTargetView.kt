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
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.kelin.photoselector.R
import kotlinx.android.synthetic.main.view_kelin_photo_selector_photo_target_view.view.*

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

    init {
        LayoutInflater.from(context).inflate(R.layout.view_kelin_photo_selector_photo_target_view, this)
    }

    internal val imageView: SubsamplingScaleImageView by lazy { ivKelinPhotoSelectorPhotoView }

    internal val defImageView: AppCompatImageView by lazy { ivKelinPhotoSelectorGifView }

    internal val progressBar: ProgressBar by lazy { pbKelinPhotoSelectorProgress }

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
        ivKelinPhotoSelectorPhotoView.setOnClickListener(l)
        ivKelinPhotoSelectorGifView.setOnClickListener(l)
    }

    inner class PhotoTarget(private val target: PhotoTargetView) : CustomViewTarget<PhotoTargetView, Drawable>(target) {
        override fun onLoadFailed(errorDrawable: Drawable?) {
            target.progressBar.visibility = View.GONE
            target.imageView.visibility = View.GONE
            target.defImageView.apply {
                setImageDrawable(errorDrawable)
                visibility = View.VISIBLE
            }
        }

        override fun onResourceCleared(placeholder: Drawable?) {
            target.progressBar.visibility = View.VISIBLE
            target.imageView.visibility = View.GONE
            target.defImageView.apply {
                setImageDrawable(placeholder)
                visibility = View.VISIBLE
            }
        }

        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
            target.progressBar.visibility = View.GONE
            if (resource is GifDrawable) {
                target.imageView.visibility = View.GONE
                target.defImageView.apply {
                    setImageDrawable(resource)
                    resource.start()
                    visibility = View.VISIBLE
                }
            } else {
                target.defImageView.visibility = View.GONE
                target.imageView.apply {
                    setImage(ImageSource.bitmap(drawableToBitmap(resource)))
                    visibility = View.VISIBLE
                }
            }
        }
    }
}
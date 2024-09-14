package com.kelin.photoselector.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import coil.drawable.ScaleDrawable
import coil.target.Target
import com.github.chrisbanes.photoview.PhotoView
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

    private val handler by lazy { object: Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            if (msg.what == 0x0100_0001) {
                lastTap = 0
                onClickListener?.onClick(this@PhotoTargetView)
            }
        }
    } }

    private var onClickListener: OnClickListener? = null

    init {
        addView(vb.root)
    }

    internal val imageView: PhotoView by lazy { vb.ivKelinPhotoSelectorPhotoView }

    internal val defImageView: AppCompatImageView by lazy { vb.ivKelinPhotoSelectorGifView }

    internal val progressBar: ProgressBar by lazy { vb.pbKelinPhotoSelectorProgress }

    val target by lazy { PhotoTarget(this) }

    private var lastTap:Long = 0

    override fun setOnClickListener(l: OnClickListener?) {
        onClickListener = l
        vb.ivKelinPhotoSelectorPhotoView.setOnClickListener {
            onClickListener?.onClick(this)
        }
        vb.ivKelinPhotoSelectorGifView.setOnClickListener {
            if (lastTap == 0L) {
                lastTap = System.currentTimeMillis()
                handler.sendMessageDelayed(Message.obtain(handler, 0x0100_0001), 500)
            }else if (System.currentTimeMillis() - lastTap > 500) {
                lastTap = 0
                onClickListener?.onClick(this)
            }else{
                lastTap = 0
                handler.removeMessages(0x0100_0001)
                // onDoubleClick  //普通图片不支持双击
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
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
                    setImageDrawable(result)
                    visibility = View.VISIBLE
                }
            }
        }
    }
}
package com.kelin.photoselector.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * **描述:** 用来显示图片的控件。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/10 2:47 PM
 *
 * **版本:** v 1.0.0
 */
internal class PhotoImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : AppCompatImageView(context, attrs, defStyleAttr) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}
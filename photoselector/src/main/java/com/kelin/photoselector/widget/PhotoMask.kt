package com.kelin.photoselector.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * **描述:** 图片的蒙层，用于使得文字信息看的更加清楚。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/10 6:38 PM
 *
 * **版本:** v 1.0.0
 */
internal class PhotoMask @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}
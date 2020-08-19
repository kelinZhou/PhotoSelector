package com.kelin.photoselector.widget

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min

/**
 * **描述:** 相册列表控件。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/22 1:54 PM
 *
 * **版本:** v 1.0.0
 */
class AlbumsListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : RecyclerView(context, attrs, defStyleAttr) {
    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(min(MeasureSpec.getSize(heightSpec), (context.resources.displayMetrics.heightPixels * 0.6).toInt()), MeasureSpec.getMode(heightSpec)))
    }
}
package com.kelin.photoselector.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.kelin.photoselector.ui.BasePhotoSelectorFragment

/**
 * **描述:** PhotoSelector的工具。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/8/3 3:11 PM
 *
 * **版本:** v 1.0.0
 */

internal fun Window.fullScreen() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
}

internal fun Window.translucentStatusBar() {
    //当系统版本为4.4或者4.4以上时可以使用沉浸式状态栏
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        //透明状态栏
        addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    }
}


internal val Context?.statusBarOffsetPx: Int
    get() = if (this == null) {
        0
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        val appContext = applicationContext
        val resourceId = appContext.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            appContext.resources.getDimensionPixelSize(resourceId)
        } else {
            25f.dp2px(this)
        }
    } else {
        25f.dp2px(this)
    }

internal val Context.screenWidth: Int
    get() = resources.displayMetrics.widthPixels

internal val Context.screenHeight: Int
    get() = resources.displayMetrics.heightPixels

internal fun Float.dp2px(context: Context): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics).toInt()
}

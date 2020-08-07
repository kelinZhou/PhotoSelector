package com.kelin.photoselector.callback

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

/**
 * **描述:** 定义回调接口。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/8/7 9:32 AM
 *
 * **版本:** v 1.0.0
 */
interface LeakProofCallback<R> : LifecycleObserver {

    fun createAndAttachTo(context: Context, callback: (context: Context, r: R) -> Unit)

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy()
}
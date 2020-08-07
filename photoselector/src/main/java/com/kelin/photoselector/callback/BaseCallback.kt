package com.kelin.photoselector.callback

import android.content.Context

/**
 * **描述:** LeakProofCallback的基本实现。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/8/7 9:47 AM
 *
 * **版本:** v 1.0.0
 */
abstract class BaseCallback<R> : LeakProofCallback<R> {

    private var localCallback: ((context: Context, r: R) -> Unit)? = null
    private var localContext: Context? = null

    internal val contextOrNull: Context?
        get() = localContext

    final override fun createAndAttachTo(context: Context, callback: (context: Context, r: R) -> Unit) {
        localCallback = callback
        localContext = context
        onAttach(context)
    }

    abstract fun onAttach(context: Context)

    internal fun callback(r: R) {
        if (localContext != null && localCallback != null) {
            localCallback!!(localContext!!, r)
        }
    }

    final override fun onDestroy() {
        localContext = null
        localCallback = null
    }
}
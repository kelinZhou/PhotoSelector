package com.kelin.photoselector.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * **描述:** Fragment的基类。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/8/19 3:21 PM
 *
 * **版本:** v 1.0.0
 */
internal abstract class BasePhotoSelectorFragment<VB : ViewBinding> : Fragment() {

    protected val applicationContext: Context
        get() = requireContext().applicationContext

    private var mVB: VB? = null

    internal val vb: VB
        get() = mVB!!

    final override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return generateViewBinding(inflater, container).let {
            mVB = it
            it.root
        }
    }

    abstract fun generateViewBinding(inflater: LayoutInflater, container: ViewGroup?, attachToParent: Boolean = false): VB

    override fun onDestroy() {
        super.onDestroy()
        mVB = null
    }

    fun finish() {
        activity?.finish()
    }
}

internal inline fun <VB : ViewBinding> BasePhotoSelectorFragment<VB>.withViewBinding(block: VB.() -> Unit) {
    block(vb)
}
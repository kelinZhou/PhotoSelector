package com.kelin.photoselector.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * **描述:** Fragment的基类。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/8/19 3:21 PM
 *
 * **版本:** v 1.0.0
 */
internal abstract class BasePhotoSelectorFragment : Fragment() {

    val applicationContext: Context
        get() = requireContext().applicationContext

    protected abstract val rootLayoutRes: Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(rootLayoutRes, container, false)
    }

    fun finish() {
        activity?.finish()
    }
}
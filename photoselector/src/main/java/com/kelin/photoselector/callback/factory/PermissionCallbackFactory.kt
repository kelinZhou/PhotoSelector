package com.kelin.photoselector.callback.factory

import android.content.Context
import com.kelin.okpermission.OkPermission
import com.kelin.photoselector.callback.BaseCallback
import com.kelin.photoselector.callback.LeakProofCallback

/**
 * **描述:** 相机权限回调工厂。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/8/7 10:10 AM
 *
 * **版本:** v 1.0.0
 */
class PermissionCallbackFactory(private val permissions: Array<String>) : CallbackFactory<Boolean> {
    override fun createCallback(): LeakProofCallback<Boolean> {
        return object :BaseCallback<Boolean>(){
            override fun onAttach(context: Context) {
                OkPermission.with(context)
                    .addDefaultPermissions(*permissions)
                    .checkAndApply { granted, p ->
                        callback(granted)
                    }
            }
        }
    }
}
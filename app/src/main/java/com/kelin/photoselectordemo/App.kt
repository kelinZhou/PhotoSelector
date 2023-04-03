package com.kelin.photoselectordemo

import android.app.Application
import com.kelin.photoselector.PhotoSelector

/**
 * **描述:** Application
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/23 10:47 AM
 *
 * **版本:** v 1.0.0
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        PhotoSelector.init(this,"${packageName}.fileProvider", true)
    }
}
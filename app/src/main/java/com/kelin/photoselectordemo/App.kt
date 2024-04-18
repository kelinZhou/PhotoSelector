package com.kelin.photoselectordemo

import android.app.Application
import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.imageLoader
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
class App : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        PhotoSelector.init(this,"${packageName}.fileProvider", true, albumTakePictureEnable = false)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                //添加GIF支持
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }
}
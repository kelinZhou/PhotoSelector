package com.kelin.photoselectordemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.kelin.photoselector.PhotoSelector
import com.kelin.photoselector.model.PhotoImpl
import com.kelin.photoselectordemo.databinding.HolderImageBinding

/**
 * **描述:** 图片列表页面。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/15 11:01 AM
 *
 * **版本:** v 1.0.0
 */
class ImageListActivity : AppCompatActivity() {

    companion object {
        private const val KEY_URIS = "key_uris"
        fun start(context: Context, vararg uris: String) {
            context.startActivity(Intent(context, ImageListActivity::class.java).apply {
                putExtra(KEY_URIS, uris)
            })
        }
    }

    private val photos by lazy {
        //这里使用PhotoImpl(uri)的方式只是演示PhotoImpl的一种简便的用法，其实建议你最好使用PhotoImpl(uri, isVideo)的方式直接指定是否是视频文件。
        intent.getStringArrayExtra(KEY_URIS)?.map { uri -> PhotoImpl(uri) } ?: emptyList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_list)
        findViewById<RecyclerView>(R.id.rvImageList).run {
            layoutManager = LinearLayoutManager(this@ImageListActivity)
            adapter = object : RecyclerView.Adapter<ImageHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
                    return ImageHolder(HolderImageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
                }

                override fun getItemCount(): Int {
                    return photos.size
                }

                override fun onBindViewHolder(holder: ImageHolder, position: Int) {
                    holder.vb.also { vb ->
                        val photo = photos[position]
                        Glide.with(this@ImageListActivity)
                            .load(photo.uri)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .apply(RequestOptions.centerCropTransform())
                            .into(vb.ivPhoto)
                        vb.ivPlayVideo.visibility = if (photo.isVideo) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                }
            }
        }
    }

    private inner class ImageHolder(val vb: HolderImageBinding) : RecyclerView.ViewHolder(vb.root) {
        init {
            vb.root.setOnClickListener {
                PhotoSelector.openPicturePreviewPage(this@ImageListActivity, photos, layoutPosition)
            }
            vb.ivPlayVideo.setOnClickListener {
                PhotoSelector.playVideo(this@ImageListActivity, photos[layoutPosition])
            }
        }
    }
}
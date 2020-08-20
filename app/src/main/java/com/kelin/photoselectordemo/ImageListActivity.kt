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
import kotlinx.android.synthetic.main.activity_image_list.*
import kotlinx.android.synthetic.main.holder_image.view.*

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
        rvImageList.run {
            layoutManager = LinearLayoutManager(this@ImageListActivity)
            adapter = object : RecyclerView.Adapter<ImageHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
                    return ImageHolder(LayoutInflater.from(parent.context).inflate(R.layout.holder_image, parent, false))
                }

                override fun getItemCount(): Int {
                    return photos.size
                }

                override fun onBindViewHolder(holder: ImageHolder, position: Int) {
                    holder.itemView.also { iv ->
                        val photo = photos[position]
                        Glide.with(this@ImageListActivity)
                            .load(photo.uri)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .apply { RequestOptions.centerCropTransform() }
                            .into(iv.ivPhoto)
                        iv.ivPlayVideo.visibility = if (photo.isVideo) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                }

            }
        }
    }

    private inner class ImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                PhotoSelector.openPicturePreviewPage(this@ImageListActivity, photos, layoutPosition)
            }
            itemView.ivPlayVideo.setOnClickListener {
                PhotoSelector.playVideo(this@ImageListActivity, photos[layoutPosition])
            }
        }
    }
}
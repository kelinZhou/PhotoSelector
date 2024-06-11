package com.kelin.photoselector.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.imageLoader
import coil.request.ImageRequest
import com.kelin.photoselector.PhotoSelector
import com.kelin.photoselector.R
import com.kelin.photoselector.databinding.FragmentKelinPhotoSelectorPhotoPreviewBinding
import com.kelin.photoselector.databinding.ViewKelinPhotoSelectorPhotoViewBinding
import com.kelin.photoselector.model.Photo
import com.kelin.photoselector.model.Picture
import com.kelin.photoselector.utils.statusBarOffsetPx

/**
 * **描述:** 图片和视频预览页面。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/8/19 4:05 PM
 *
 * **版本:** v 1.0.0
 */
internal class PhotoPreviewFragment : BasePhotoSelectorFragment<FragmentKelinPhotoSelectorPhotoPreviewBinding>() {

    companion object {
        private const val KEY_PHOTO_URLS_DATA = "key_kelin_photo_selector_photo_urls_data"
        private const val KEY_PICTURE_URLS_DATA = "key_kelin_photo_selector_picture_urls_data"
        private const val KEY_SELECTED_POSITION = "key_kelin_photo_selector_selected_position"

        @Suppress("unchecked_cast")
        fun configurationPreviewIntent(intent: Intent, list: List<Photo>, position: Int = 0) {
            if (list.all { it is Parcelable }) {  //优先使用Parsable可以提高效率。
                intent.putParcelableArrayListExtra(KEY_PICTURE_URLS_DATA, (list as List<Picture>).let { if (it is ArrayList) it else ArrayList(it) })
            } else {
                intent.putExtra(KEY_PHOTO_URLS_DATA, list.let { if (it is ArrayList) it else ArrayList(it) })
            }
            intent.putExtra(KEY_SELECTED_POSITION, position)
        }
    }


    @Suppress("unchecked_cast")
    private val photos by lazy {
        requireArguments().let { arg ->
            arg.getParcelableArrayList<Picture>(KEY_PICTURE_URLS_DATA) ?: arg.getSerializable(KEY_PHOTO_URLS_DATA).let {
                (it as? ArrayList<Photo>) ?: throw IllegalArgumentException("Photos must not be null!")
            }
        }
    }

    /**
     * 图片浏览的ViewPage的适配器。
     */
    private val pageAdapter by lazy { PhotoViewPageAdapter(photos) }

    override fun generateViewBinding(inflater: LayoutInflater, container: ViewGroup?, attachToParent: Boolean): FragmentKelinPhotoSelectorPhotoPreviewBinding {
        return FragmentKelinPhotoSelectorPhotoPreviewBinding.inflate(inflater, container, attachToParent)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        withViewBinding {
            //获取初始化索引，默认为第一个。
            val p = requireArguments().getInt(KEY_SELECTED_POSITION, 0)
            tvKelinPhotoSelectorIndicator.apply {
                if (photos.size > 1) {
                    (layoutParams as ViewGroup.MarginLayoutParams).also { lp ->
                        lp.topMargin = context.statusBarOffsetPx
                    }
                    //设置图片预览指示器
                    text = "${p + 1}/${photos.size}"
                } else {
                    visibility = View.GONE
                }
            }
            //初始化ViewPager以及所有子View。
            vpKelinPhotoSelectorPager.run {
                offscreenPageLimit = 3  //设置预加载3页，即左边、当前、右边，这样在滑动时更加流畅，基本看不到loading。
                //为ViewPager设置页面切换监听
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        //当选中页面变更后更新指示器。
                        tvKelinPhotoSelectorIndicator.text = "${position + 1}/${pageAdapter.itemCount}"
                    }
                })
                adapter = pageAdapter
                setCurrentItem(p, false)
            }
        }
    }

    private inner class PhotoViewPageAdapter(private val photos: List<Photo>) : RecyclerView.Adapter<PhotoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            return PhotoViewHolder(ViewKelinPhotoSelectorPhotoViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            holder.vb.also { vb ->
                val photo = photos[position]
                vb.root.context.imageLoader.enqueue(
                    ImageRequest.Builder(vb.root.context)
                        .data(photo.uri)
                        .error(R.drawable.kelin_photo_selector_img_load_error)
                        .listener { _, _ ->
                            vb.ivKelinPhotoSelectorPlayVideo.visibility = if (photo.isVideo) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                        }
                        .target(vb.ptKelinPhotoSelectorPhotoTargetView.target)
                        .build()
                )
            }
        }

        override fun getItemCount(): Int {
            return photos.size
        }
    }

    private inner class PhotoViewHolder(val vb: ViewKelinPhotoSelectorPhotoViewBinding) : RecyclerView.ViewHolder(vb.root) {
        init {
            vb.ptKelinPhotoSelectorPhotoTargetView.setOnClickListener { finish() }
            //设置播放视频控件点击之后调用系统的播放视频功能播放视频。
            vb.ivKelinPhotoSelectorPlayVideo.setOnClickListener {
                PhotoSelector.playVideo(requireActivity(), photos[layoutPosition])
            }
        }
    }
}
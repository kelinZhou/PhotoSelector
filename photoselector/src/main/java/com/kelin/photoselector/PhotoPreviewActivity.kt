package com.kelin.photoselector

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.kelin.photoselector.model.Photo
import kotlinx.android.synthetic.main.activity_kelin_photo_selector_photo_preview.*
import kotlinx.android.synthetic.main.view_kelin_photo_selector_photo_view.view.*
import java.io.File

/**
 * **描述:** 图片和视频预览页面。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/13 6:36 PM
 *
 * **版本:** v 1.0.0
 */
class PhotoPreviewActivity : AppCompatActivity() {

    companion object {

        private const val KEY_PHOTO_URLS_DATA = "key_photo_urls_data"
        private const val KEY_SELECTED_POSITION = "key_selected_position"


        internal fun start(context: Context, list: List<Photo>, position: Int = 0) {
            context.startActivity(Intent(context, PhotoPreviewActivity::class.java).apply {
                putExtra(KEY_PHOTO_URLS_DATA, list.let { if (it is ArrayList) it else ArrayList(it) })
                putExtra(KEY_SELECTED_POSITION, position)
            })
        }
    }

    @Suppress("unchecked_cast")
    private val photos by lazy {
        intent.getSerializableExtra(KEY_PHOTO_URLS_DATA).let {
            it as? ArrayList<Photo> ?: throw IllegalArgumentException("Photos must not be null!")
        }
    }

    /**
     * 图片浏览的ViewPage的适配器。
     */
    private val pageAdapter by lazy { PhotoViewPageAdapter(photos) }
    /**
     * 使用预加载的方式加载ViewPager中的所有子View。以空间换时间的实现方式，预览图片过多的情况暂不考虑。
     */
    private lateinit var pageViews: List<View>

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kelin_photo_selector_photo_preview)
        supportActionBar?.hide()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        //当系统版本为4.4或者4.4以上时可以使用沉浸式状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //透明状态栏
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        //获取初始化索引，默认为第一个。
        val p = intent.getIntExtra(KEY_SELECTED_POSITION, 0)
        //设置图片预览指示器
        tvKelinPhotoSelectorIndicator.text = "${p + 1}/${photos.size}"
        //初始化ViewPager以及所有子View。
        val inflater = LayoutInflater.from(this)
        vpKelinPhotoSelectorPager.run {
            pageViews = photos.mapIndexed { index, photo ->
                //初始化所有子View。
                val pageView = inflater.inflate(R.layout.view_kelin_photo_selector_photo_view, this, false)
                pageView.ivKelinPhotoSelectorPhotoView.apply {
                    Glide.with(this)
                        .load(photo.uri)
                        .addListener(getLoadListener(index))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(this)
                    setOnTapListener { finish() }
                    setOnExitListener { _, _, _, _, _ ->
                        finish()
                    }
                }
                //设置播放视频控件点击之后调用系统的播放视频功能播放视频。
                pageView.ivKelinPhotoSelectorPlayVideo.setOnClickListener {
                    PhotoSelector.playVideoWithSystem(this@PhotoPreviewActivity, photos[vpKelinPhotoSelectorPager.currentItem])
                }
                pageView
            }
            //为ViewPager设置页面切换监听
            setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

                }

                override fun onPageSelected(position: Int) {
                    //当选中页面变更后更新指示器。
                    tvKelinPhotoSelectorIndicator.text = "${position + 1}/${adapter!!.count}"
                }

                override fun onPageScrollStateChanged(state: Int) {

                }
            })
            //转场动画
//            viewTreeObserver.addOnGlobalLayoutListener(object :ViewTreeObserver.OnGlobalLayoutListener{
//                override fun onGlobalLayout() {
//                    vpPager.viewTreeObserver.removeOnGlobalLayoutListener(this)
//                }
//
//            })
            adapter = pageAdapter
            currentItem = p
        }
    }

    private fun getLoadListener(position: Int): RequestListener<Drawable?> {
        return ImageLoadListener(position)
    }

    private inner class ImageLoadListener(private val position: Int) : RequestListener<Drawable?> {
        override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
            e?.printStackTrace()
            pbKelinPhotoSelectorProgress.visibility = View.GONE
            pageViews[position].ivKelinPhotoSelectorPhotoView.setImageResource(R.drawable.kelin_photo_selector_img_load_error)
            return true
        }

        override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
            pbKelinPhotoSelectorProgress.visibility = View.GONE
            pageViews[position].ivKelinPhotoSelectorPlayVideo.visibility = if (photos[position].isVideo) {
                View.VISIBLE
            } else {
                View.GONE
            }
            return false
        }
    }

    private inner class PhotoViewPageAdapter(private val photos: List<Photo>) : PagerAdapter() {

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            return pageViews[position].apply { container.addView(this) }
        }

        override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
            container.removeView(pageViews[position])
        }

        override fun getCount(): Int {
            return photos.size
        }

        override fun isViewFromObject(view: View, obj: Any): Boolean {
            return view == obj
        }
    }
}
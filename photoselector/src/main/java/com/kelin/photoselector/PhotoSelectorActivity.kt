package com.kelin.photoselector

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.loader.app.LoaderManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.kelin.okpermission.OkActivityResult
import com.kelin.okpermission.OkPermission
import com.kelin.photoselector.cache.DistinctManager
import com.kelin.photoselector.loader.AlbumPictureLoadCallback
import com.kelin.photoselector.model.*
import com.kelin.photoselector.model.AlbumType
import com.kelin.photoselector.model.PictureType
import com.kelin.photoselector.ui.AlbumsDialog
import kotlinx.android.synthetic.main.activity_kelin_photo_selector_list.*
import kotlinx.android.synthetic.main.holder_kelin_photo_selector_picture.view.*

/**
 * **描述:** 照片选择的Activity。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/9 3:50 PM
 *
 * **版本:** v 1.0.0
 */
class PhotoSelectorActivity : AppCompatActivity() {

    companion object {

        private const val KEY_KELIN_PHOTO_SELECTOR_ALBUM_TYPE = "key_kelin_photo_selector_album_type"
        private const val KEY_KELIN_PHOTO_SELECTOR_MAX_COUNT = "key_kelin_photo_selector_max_count"
        private const val KEY_KELIN_PHOTO_SELECTOR_ID = "key_kelin_photo_selector_id"

        internal fun startPictureSelectorPage(context: Context, albumType: AlbumType, maxCount: Int, id: Int, result: (photos: List<Photo>) -> Unit) {
            OkPermission.with(context)
                .addDefaultPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .checkAndApply { granted, permissions ->
                    if (granted) {
                        OkActivityResult.startActivity<List<Photo>>(
                            context as Activity,
                            Intent(context, PhotoSelectorActivity::class.java).apply {
                                putExtra(KEY_KELIN_PHOTO_SELECTOR_ID, id)
                                putExtra(KEY_KELIN_PHOTO_SELECTOR_ALBUM_TYPE, albumType.type)
                                putExtra(KEY_KELIN_PHOTO_SELECTOR_MAX_COUNT, maxCount)
                            }) { resultCode, data ->
                            if (resultCode == Activity.RESULT_OK && data != null) {
                                result(data)
                            } else {
                                result(emptyList())
                            }
                        }
                    }
                }
        }
    }

    private val id by lazy { intent.getIntExtra(KEY_KELIN_PHOTO_SELECTOR_ID, -1) }

    private var currentAlbumName: String = ""

    private val albumsDialog by lazy { AlbumsDialog(this, albums, currentAlbumName) { onAlbumSelected(it) } }

    private val message by lazy {
        when (albumType) {
            AlbumType.PHOTO -> "图片"
            AlbumType.VIDEO -> "视频"
            else -> "图片和视频"
        }
    }

    private val albumType by lazy { AlbumType.typeOf(intent.getIntExtra(KEY_KELIN_PHOTO_SELECTOR_ALBUM_TYPE, AlbumType.PHOTO_VIDEO.type)) }

    private val maxCount by lazy { intent.getIntExtra(KEY_KELIN_PHOTO_SELECTOR_MAX_COUNT, 9) }

    private val listAdapter by lazy { PhotoListAdapter(DistinctManager.instance.getSelected(id)) }

    private val listLayoutManager by lazy {
        object : GridLayoutManager(this@PhotoSelectorActivity, getSpanCount(resources.configuration)) {
            override fun onLayoutChildren(
                recycler: RecyclerView.Recycler?,
                state: RecyclerView.State?
            ) {
                try {
                    super.onLayoutChildren(recycler, state)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.also {
            it.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return 1
                }
            }
        }
    }

    private var albums = emptyList<Album>()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kelin_photo_selector_list)
        rlKelinPhotoSelectorToolbar.setPadding(0, getStatusBarOffsetPx(), 0, 0)
        supportActionBar?.hide()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        //当系统版本为4.4或者4.4以上时可以使用沉浸式状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //透明状态栏
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        tvKelinPhotoSelectorPageTitle.text = "选择$message"
        updateSelectedCount(0)

        rvKelinPhotoSelectorPhotoListView.run {
            layoutManager = listLayoutManager
            adapter = listAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        tvKelinPhotoSelectorModifiedDate.visibility = View.VISIBLE
                    } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        tvKelinPhotoSelectorModifiedDate.visibility = View.GONE
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    tvKelinPhotoSelectorModifiedDate.text = listAdapter.getItem(listLayoutManager.findFirstVisibleItemPosition()).modifyDate
                }
            })
        }
        LoaderManager.getInstance(this).initLoader(albumType.type, null, AlbumPictureLoadCallback(applicationContext) {
            albums = it
            val defAlbum = it.find { a -> a.name == PreferenceManager.getDefaultSharedPreferences(applicationContext).getString("kelin_photo_selector_selected_album_name", "") } ?: it.firstOrNull()
            if (defAlbum == null) {
                Toast.makeText(this, "您的设备中没有任何${message}", Toast.LENGTH_SHORT).show()
            } else {
                onAlbumSelected(defAlbum)
            }
        })
        //关闭当前页面
        ivKelinPhotoSelectorFinish.setOnClickListener { finish() }
        //变更相册
        rlKelinPhotoSelectorAlbumName.setOnClickListener { onSelectAlbums() }
        //重新选择
        tvKelinPhotoSelectorReselect.setOnClickListener {
            listAdapter.clearSelected()
            updateSelectedCount(0)
        }
        //预览选中图片
        tvKelinPhotoSelectorPreview.setOnClickListener {
            PhotoSelector.openPicturePreviewPage(this, listAdapter.selectedPictures)
        }
        //用户点击完成按钮。
        btnKelinPhotoSelectorDone.setOnClickListener {
            listAdapter.selectedPictures.also {
                DistinctManager.instance.saveSelected(id, it)
                OkActivityResult.setResultData(this, it)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        listLayoutManager.spanCount = getSpanCount(newConfig)
    }

    private fun getSpanCount(config: Configuration): Int {
        return if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            8
        } else {
            4
        }
    }

    private fun onAlbumSelected(album: Album) {
        currentAlbumName = album.name
        PreferenceManager.getDefaultSharedPreferences(applicationContext).edit().putString("kelin_photo_selector_selected_album_name", album.name).apply()
        listAdapter.setPhotos(album.pictures)
        tvKelinPhotoSelectorAlbumName.text = album.name
    }

    private fun onSelectAlbums() {
        albumsDialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun updateSelectedCount(selectedCount: Int) {
        val visible = if (selectedCount > 0) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
        tvKelinPhotoSelectorPreview.apply {
            visibility = visible
            if (selectedCount > 0) {
                text = "${getString(R.string.kelin_photo_selector_preview)}(${selectedCount})"
            }
        }
        tvKelinPhotoSelectorReselect.visibility = visible
        btnKelinPhotoSelectorDone.apply {
            text = "完成($selectedCount/$maxCount)"
            isEnabled = selectedCount > 0
        }
    }

    private fun getStatusBarOffsetPx(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val appContext = applicationContext
            val resourceId = appContext.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                appContext.resources.getDimensionPixelSize(resourceId)
            } else {
                0
            }
        } else {
            0
        }
    }

    private inner class PhotoListAdapter(initialSelected: List<Picture>?) : RecyclerView.Adapter<PhotoHolder>() {

        init {
            if (!initialSelected.isNullOrEmpty()) {
                updateSelectedCount(initialSelected.size)
            }
        }

        private var photoList: List<Picture>? = null

        internal val selectedPictures: ArrayList<Picture> = initialSelected?.let { if (it is ArrayList) it else ArrayList(it) } ?: ArrayList()

        internal val dataList: List<Picture>
            get() = photoList ?: emptyList()

        internal fun setPhotos(photos: List<Picture>, refresh: Boolean = true) {
            photoList = photos
            if (refresh) {
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
            return PhotoHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.holder_kelin_photo_selector_picture,
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return photoList?.size ?: 0
        }

        internal fun notifyItemChanged(item: Picture) {
            photoList?.run {
                notifyItemChanged(indexOf(item))
            }
        }

        internal fun getItem(position: Int): Picture {
            return photoList?.get(position)
                ?: throw NullPointerException("The item must not be null!")
        }

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val item = getItem(position)
            holder.itemView.also { iv ->
                Glide.with(iv.context)
                    .load(item.uri)
                    .apply(RequestOptions().centerCrop().placeholder(R.drawable.image_placeholder)).into(iv.ivKelinPhotoSelectorPhotoView)
                val no = selectedPictures.indexOf(item)
                iv.rlKelinPhotoSelectorChecker.isSelected = no >= 0
                iv.pmKelinPhotoSelectorPhotoViewMask.isSelected = no >= 0
                iv.tvKelinPhotoSelectorChecker.text = if (no >= 0) {
                    (no + 1).toString()
                } else {
                    null
                }
                iv.tvKelinPhotoSelectorVideoDuration.apply {
                    visibility = if (item.type == PictureType.VIDEO) {
                        text = item.duration
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        }

        fun clearSelected() {
            if (selectedPictures.isNotEmpty()) {
                val targetPositions = selectedPictures.map { photoList!!.indexOf(it) }
                selectedPictures.clear()
                targetPositions.forEach {
                    notifyItemChanged(it)
                }
            }
        }
    }

    private inner class PhotoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.rlKelinPhotoSelectorChecker.setOnClickListener {
                listAdapter.getItem(layoutPosition).apply {
                    val selectedPictures = listAdapter.selectedPictures
                    //如果被选中了的资源中没有当前的资源，那么就认为当前用户的目的是选中，否则就是取消选中。
                    val isSelected = !selectedPictures.contains(this)
                    if (isSelected && listAdapter.selectedPictures.size >= maxCount) {
                        Toast.makeText(applicationContext, "最多只能选择${maxCount}${if (albumType == AlbumType.PHOTO) "张" else "个"}$message", Toast.LENGTH_SHORT).show()
                    } else {
                        if (isSelected) {
                            selectedPictures.add(this)
                        } else {
                            //取消选中时判断是否是取消的最后一个，如果不是的话那还要刷新其他的条目变更序号。
                            val isLast = this == selectedPictures.lastOrNull()
                            //获取当前取消的是第几个，一定要在当前资源没有从选中池里面已出的时候获取，否则永远是-1。
                            val currentIndex = selectedPictures.indexOf(this)
                            selectedPictures.remove(this)
                            if (!isLast) {
                                selectedPictures.forEachIndexed { i, p ->
                                    if (i >= currentIndex) {
                                        listAdapter.notifyItemChanged(p)
                                    }
                                }
                            }
                        }
                        listAdapter.notifyItemChanged(layoutPosition)
                        updateSelectedCount(listAdapter.selectedPictures.size)
                    }
                }
            }
            itemView.setOnClickListener {
                PhotoSelector.openPicturePreviewPage(this@PhotoSelectorActivity, listOf(listAdapter.getItem(layoutPosition)))
            }
        }
    }
}
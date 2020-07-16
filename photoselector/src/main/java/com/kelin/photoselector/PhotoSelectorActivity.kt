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

        internal fun startPictureSelectorPage(context: Context, albumType: AlbumType, maxCount: Int, result: (photos: List<Photo>) -> Unit) {
            OkPermission.with(context)
                .addDefaultPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .checkAndApply { granted, permissions ->
                    if (granted) {
                        OkActivityResult.startActivity<List<Photo>>(
                            context as Activity,
                            Intent(context, PhotoSelectorActivity::class.java).apply {
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

    private val listAdapter by lazy { PhotoListAdapter() }

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
        rvKelinPhotoSelectorPhotoListView.run {
            layoutManager = listLayoutManager
            adapter = listAdapter
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
        tvKelinPhotoSelectorPageTitle.text = "选择$message"
        updateSelectedCount(0)
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
            PhotoSelector.openPicturePreviewPage(this, listAdapter.selectedPictures.map { it.picture })
        }
        btnKelinPhotoSelectorDone.setOnClickListener {
            listAdapter.selectedPictures.mapTo(ArrayList()) { it.picture }.also {
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
        tvKelinPhotoSelectorPreview.visibility = visible
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

    private inner class PhotoListAdapter : RecyclerView.Adapter<PhotoHolder>() {

        private var photoList: List<PictureWrapper>? = null

        internal val selectedPictures: MutableList<PictureWrapper> = ArrayList()

        internal fun setPhotos(photos: List<PictureWrapper>, refresh: Boolean = true) {
            if (selectedPictures.isNotEmpty()) {
                photos.forEach {
                    val s = selectedPictures.find { p -> p.picture.uri == it.picture.uri }
                    if (s != null) {
                        it.isSelected = true
                        it.no = s.no
                    }
                }
            }
            photoList = photos
            refresh.isTrue { notifyDataSetChanged() }
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

        internal fun notifyItemChanged(item: PictureWrapper) {
            photoList?.run {
                notifyItemChanged(indexOfFirst { it.picture.uri == item.picture.uri })
            }
        }

        internal fun getItem(position: Int): PictureWrapper {
            return photoList?.get(position)
                ?: throw NullPointerException("The item must not be null!")
        }

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val item = getItem(position)
            holder.itemView.also { iv ->
                val picture = item.picture
                Glide.with(iv.context)
                    .load(picture.uri)
                    .apply(RequestOptions().centerCrop().placeholder(R.drawable.image_placeholder)).into(iv.ivKelinPhotoSelectorPhotoView)
                iv.rlKelinPhotoSelectorChecker.isSelected = item.isSelected
                iv.tvKelinPhotoSelectorChecker.text = if (item.isSelected) {
                    item.no?.toString()
                } else {
                    null
                }
                iv.tvKelinPhotoSelectorVideoDuration.apply {
                    visibility = if (picture.type == PictureType.VIDEO) {
                        text = picture.duration
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        }

        fun clearSelected() {
            if (selectedPictures.isNotEmpty()) {
                selectedPictures.clear()
                photoList!!.forEachIndexed { i, p ->
                    if (p.isSelected) {
                        p.isSelected = false
                        p.no = null
                        notifyItemChanged(i)
                    }
                }
            }
        }
    }

    private inner class PhotoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.rlKelinPhotoSelectorChecker.setOnClickListener {
                listAdapter.getItem(layoutPosition).apply {
                    val s = !isSelected
                    if (s && listAdapter.selectedPictures.size >= maxCount) {
                        Toast.makeText(applicationContext, "最多只能选择${maxCount}${if (albumType == AlbumType.PHOTO) "张" else "个"}$message", Toast.LENGTH_SHORT).show()
                    } else {
                        val selectedPictures = listAdapter.selectedPictures
                        no = if (s) {
                            selectedPictures.add(this)
                            selectedPictures.size
                        } else {
                            if (no!! < selectedPictures.size) {
                                selectedPictures.forEach {
                                    if (it.no ?: 0 > no!!) {
                                        it.no = it.no!! - 1
                                        listAdapter.notifyItemChanged(it)
                                    }
                                }
                            }
                            selectedPictures.removeFirst { it.picture.uri == picture.uri }
                            null
                        }
                        isSelected = s//这一句必须放到后面
                        listAdapter.notifyItemChanged(layoutPosition)
                        updateSelectedCount(listAdapter.selectedPictures.size)
                    }
                }
            }
            itemView.setOnClickListener {
                PhotoSelector.openPicturePreviewPage(this@PhotoSelectorActivity, listOf(listAdapter.getItem(layoutPosition).picture))
            }
        }
    }
}

internal fun <E> MutableIterable<E>.removeFirst(filter: (e: E) -> Boolean): Boolean {
    val ech = iterator()
    while (ech.hasNext()) {
        if (filter(ech.next())) {
            ech.remove()
            return true
        }
    }
    return false
}
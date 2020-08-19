package com.kelin.photoselector.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.loader.app.LoaderManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.kelin.okpermission.OkActivityResult
import com.kelin.photoselector.PhotoSelector
import com.kelin.photoselector.R
import com.kelin.photoselector.cache.DistinctManager
import com.kelin.photoselector.loader.AlbumPictureLoadCallback
import com.kelin.photoselector.model.Album
import com.kelin.photoselector.model.AlbumType
import com.kelin.photoselector.model.Picture
import com.kelin.photoselector.model.PictureType
import com.kelin.photoselector.utils.compressAndRotateByDegree
import com.kelin.photoselector.utils.statusBarOffsetPx
import com.kelin.photoselector.widget.AlbumsDialog
import com.kelin.photoselector.widget.ProgressDialog
import kotlinx.android.synthetic.main.fragment_kelin_photo_selector_list.*
import kotlinx.android.synthetic.main.holder_kelin_photo_selector_picture.view.*

/**
 * **描述:** 相册页面。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/8/19 3:12 PM
 *
 * **版本:** v 1.0.0
 */
internal class AlbumFragment : BasePhotoSelectorFragment() {

    companion object {
        private const val KEY_KELIN_PHOTO_SELECTOR_ALBUM_TYPE = "key_kelin_photo_selector_album_type"
        private const val KEY_KELIN_PHOTO_SELECTOR_MAX_COUNT = "key_kelin_photo_selector_max_count"
        private const val KEY_KELIN_PHOTO_SELECTOR_ID = "key_kelin_photo_selector_id"

        internal fun configurationPictureSelectorIntent(intent: Intent, albumType: AlbumType, maxLength: Int, id: Int){
            intent.putExtra(KEY_KELIN_PHOTO_SELECTOR_ID, id)
            intent.putExtra(KEY_KELIN_PHOTO_SELECTOR_ALBUM_TYPE, albumType.type)
            intent.putExtra(KEY_KELIN_PHOTO_SELECTOR_MAX_COUNT, maxLength)
        }
    }

    override val rootLayoutRes: Int
        get() = R.layout.fragment_kelin_photo_selector_list

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private val sp by lazy {
        applicationContext.getSharedPreferences("${applicationContext.packageName}_photo_selector", Context.MODE_PRIVATE)
    }

    private val selectId by lazy { requireArguments().getInt(KEY_KELIN_PHOTO_SELECTOR_ID, -1) }

    private var currentAlbumName: String = ""

    private val albumsDialog by lazy { AlbumsDialog(requireActivity(), albums, currentAlbumName) { onAlbumSelected(it) } }

    private val message by lazy {
        when (albumType) {
            AlbumType.PHOTO -> "图片"
            AlbumType.VIDEO -> "视频"
            else -> "图片和视频"
        }
    }

    private val albumType by lazy { AlbumType.typeOf(requireArguments().getInt(KEY_KELIN_PHOTO_SELECTOR_ALBUM_TYPE, AlbumType.PHOTO_VIDEO.type)) }

    private val maxLength by lazy { requireArguments().getInt(KEY_KELIN_PHOTO_SELECTOR_MAX_COUNT, PhotoSelector.defMaxLength) }

    private val listAdapter by lazy { PhotoListAdapter(DistinctManager.instance.getSelected(selectId, albumType)) }

    private val listLayoutManager by lazy {
        object : GridLayoutManager(requireContext(), getSpanCount(isLandscape(resources.configuration))) {
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rlKelinPhotoSelectorToolbar.setPadding(0, context.statusBarOffsetPx, 0, 0)

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
            val defAlbum = it.find { a -> a.name == sp.getString("kelin_photo_selector_selected_album_name", "") } ?: it.firstOrNull()
            if (defAlbum == null) {
                Toast.makeText(applicationContext, "您的设备中没有任何${message}", Toast.LENGTH_SHORT).show()
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
            PhotoSelector.openPicturePreviewPage(requireActivity(), listAdapter.selectedPictures)
        }
        //用户点击完成按钮。
        btnKelinPhotoSelectorDone.setOnClickListener {
            onSelectDone()
        }
    }

    private fun onSelectDone(needProgress: Boolean = true) {
        listAdapter.selectedPictures.also { selected ->
            if (!PhotoSelector.isAutoCompress || selected.all { it.isComposeFinished }) {  //如果压缩已经完成(无论是否成功)
                DistinctManager.instance.saveSelected(selectId, selected)
                OkActivityResult.setResultData(requireActivity(), selected)
            } else {  //如果压缩没有完成
                if (needProgress) {  //如果需要进度提示
                    ProgressDialog().show(requireFragmentManager(), selectId.toString())
                }
                handler.postDelayed({
                    onSelectDone(false)
                }, 100)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        listLayoutManager.spanCount = getSpanCount(isLandscape(newConfig))
    }

    private fun isLandscape(config: Configuration) = config.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun getSpanCount(landscape: Boolean): Int = if (landscape) 8 else 4

    private fun onAlbumSelected(album: Album) {
        if (album.name != currentAlbumName) {
            currentAlbumName = album.name
            sp.edit().putString("kelin_photo_selector_selected_album_name", album.name).apply()
            listAdapter.setPhotos(album.pictures)
            tvKelinPhotoSelectorAlbumName.text = album.name
        }
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
            text = "完成($selectedCount/$maxLength)"
            isEnabled = selectedCount > 0
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
                    .apply(RequestOptions.centerCropTransform().placeholder(R.drawable.image_placeholder))
                    .into(iv.ivKelinPhotoSelectorPhotoView)
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
                    if (isSelected && listAdapter.selectedPictures.size >= maxLength) {
                        Toast.makeText(applicationContext, "最多只能选择${maxLength}${if (albumType == AlbumType.PHOTO) "张" else "个"}$message", Toast.LENGTH_SHORT).show()
                    } else {
                        if (isSelected) {
                            if (PhotoSelector.isAutoCompress && !isVideo) {
                                compressAndRotateByDegree()
                            }
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
                PhotoSelector.openPicturePreviewPage(requireActivity(), listOf(listAdapter.getItem(layoutPosition)))
            }
        }
    }
}
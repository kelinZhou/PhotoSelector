package com.kelin.photoselector.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.loader.app.LoaderManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.kelin.okpermission.OkActivityResult
import com.kelin.photoselector.PhotoSelector
import com.kelin.photoselector.R
import com.kelin.photoselector.cache.DistinctManager
import com.kelin.photoselector.loader.AlbumPictureLoadCallback
import com.kelin.photoselector.model.*
import com.kelin.photoselector.model.Album
import com.kelin.photoselector.model.AlbumType
import com.kelin.photoselector.model.Picture
import com.kelin.photoselector.model.PictureType
import com.kelin.photoselector.utils.compressAndRotateByDegree
import com.kelin.photoselector.utils.statusBarOffsetPx
import com.kelin.photoselector.widget.AlbumsDialog
import com.kelin.photoselector.widget.ProgressDialog
import kotlinx.android.synthetic.main.fragment_kelin_photo_selector_album.*
import kotlinx.android.synthetic.main.holder_kelin_photo_selector_picture.view.*
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

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
        private const val KEY_KELIN_PHOTO_SELECTOR_MAX_DURATION = "key_kelin_photo_selector_max_duration"

        /**
         * 配置选择意图。
         * @param albumType 相册类型。
         * @param maxLength 最多可以选择多少个。
         * @param id 本次选择的唯一ID，应当是与View关联的。
         * @param maxDuration 选择视频是的最大时长限制，单位秒。
         */
        internal fun configurationPictureSelectorIntent(intent: Intent, albumType: AlbumType, maxLength: Int, id: Int, maxDuration: Long) {
            intent.putExtra(KEY_KELIN_PHOTO_SELECTOR_ID, id)
            intent.putExtra(KEY_KELIN_PHOTO_SELECTOR_ALBUM_TYPE, albumType.type)
            intent.putExtra(KEY_KELIN_PHOTO_SELECTOR_MAX_COUNT, maxLength)
            if (maxDuration > 0) {
                //这里乘以1000是为了转换为毫秒，方便后面做比对。
                intent.putExtra(KEY_KELIN_PHOTO_SELECTOR_MAX_DURATION, maxDuration * 1000)
            }
        }
    }

    private val dataFormat by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.CHINA) }

    override val rootLayoutRes: Int
        get() = R.layout.fragment_kelin_photo_selector_album

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private val sp by lazy {
        applicationContext.getSharedPreferences("${applicationContext.packageName}_photo_selector", Context.MODE_PRIVATE)
    }

    private val selectorId by lazy { requireArguments().getInt(KEY_KELIN_PHOTO_SELECTOR_ID, PhotoSelector.ID_REPEATABLE) }

    private val justSelectOne by lazy { selectorId == PhotoSelector.ID_SINGLE }

    private var currentAlbumName: String = ""

    private val albumsDialog by lazy { AlbumsDialog(requireActivity(), albums, currentAlbumName) { onAlbumSelected(it) } }

    private val message by lazy {
        when (albumType) {
            AlbumType.PHOTO -> getString(R.string.kelin_photo_selector_pictures)
            AlbumType.VIDEO -> getString(R.string.kelin_photo_selector_videos)
            else -> getString(R.string.kelin_photo_selector_pictures_and_videos)
        }
    }

    private val albumType by lazy { AlbumType.typeOf(requireArguments().getInt(KEY_KELIN_PHOTO_SELECTOR_ALBUM_TYPE, AlbumType.PHOTO_VIDEO.type)) }

    private val maxLength by lazy { requireArguments().getInt(KEY_KELIN_PHOTO_SELECTOR_MAX_COUNT, PhotoSelector.defMaxLength) }

    private val isSingleSelector by lazy { maxLength == 1 }

    private val listAdapter by lazy {
        PhotoListAdapter(DistinctManager.instance.getSelected(selectorId, albumType))
    }

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

        tvKelinPhotoSelectorPageTitle.text = "${getString(R.string.kelin_photo_selector_select)}$message"
        updateSelectedCount(0)

        rvKelinPhotoSelectorPhotoListView.run {
            layoutManager = listLayoutManager
            adapter = listAdapter
            itemAnimator = DefaultItemAnimator().apply {
                addDuration = 0
                changeDuration = 200
                removeDuration = 0
                moveDuration = 0
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        tvKelinPhotoSelectorModifiedDate.visibility = View.VISIBLE
                    } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        tvKelinPhotoSelectorModifiedDate.visibility = View.GONE
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    tvKelinPhotoSelectorModifiedDate.text = if (listAdapter.itemCount - listAdapter.albumOffset > 0) {
                        listLayoutManager.findFirstVisibleItemPosition().let {
                            val position = if (it == 0) 1 else it
                            if (listAdapter.itemCount > position) {
                                listAdapter.getItem(position).modifyDate
                            } else {
                                ""
                            }
                        }
                    } else {
                        ""
                    }
                }
            })
        }
        LoaderManager.getInstance(this).initLoader(albumType.type, null, AlbumPictureLoadCallback(applicationContext, requireArguments().getLong(KEY_KELIN_PHOTO_SELECTOR_MAX_DURATION, 0)) {
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
            updateSelectedCount(listAdapter.selectedPictures.size)
        }
        //预览选中图片
        tvKelinPhotoSelectorPreview.setOnClickListener {
            PhotoSelector.openPicturePreviewPage(requireActivity(), listAdapter.selectedPictures)
        }
        if (justSelectOne) {
            btnKelinPhotoSelectorDone.setBackgroundColor(Color.TRANSPARENT)
        } else {
            //用户点击完成按钮。
            btnKelinPhotoSelectorDone.setOnClickListener {
                onSelectDone()
            }
        }
    }

    private fun onSelectDone(needProgress: Boolean = true) {
        listAdapter.selectedPictures.also { selected ->
            if (!PhotoSelector.isAutoCompress || selected.all { it.isComposeFinished }) {  //如果压缩已经完成(无论是否成功)
                DistinctManager.instance.saveSelected(selectorId, selected)
                OkActivityResult.setResultData(requireActivity(), getRealResult(selected))
            } else {  //如果压缩没有完成
                if (needProgress) {  //如果需要进度提示
                    ProgressDialog().show(requireFragmentManager(), selectorId.toString())
                }
                handler.postDelayed({
                    onSelectDone(false)
                }, 100)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getRealResult(selected: List<Picture>): Serializable? {
        return if (selectorId == PhotoSelector.ID_SINGLE) {
            selected.firstOrNull()
        } else {
            ArrayList(selected)
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
            text = "${getString(if (justSelectOne) R.string.kelin_photo_selector_selected else R.string.kelin_photo_selector_done)}($selectedCount/$maxLength)"
            isEnabled = !justSelectOne && selectedCount > 0
        }
    }

    private fun onPictureSelectedChanged(picture: Picture, isSelected: Boolean) {
        val selectedPictures = listAdapter.selectedPictures
        if (isSelected) {
            if (PhotoSelector.isAutoCompress && !picture.isVideo) {
                picture.compressAndRotateByDegree()
            }
            selectedPictures.add(picture)
        } else {
            //取消选中时判断是否是取消的最后一个，如果不是的话那还要刷新其他的条目变更序号。
            val isLast = picture == selectedPictures.lastOrNull()
            //获取当前取消的是第几个，一定要在当前资源没有从选中池里面移除的时候获取，否则永远是-1。
            val currentIndex = selectedPictures.indexOf(picture)
            selectedPictures.remove(picture)
            if (!isLast) {
                selectedPictures.forEachIndexed { i, p ->
                    if (i >= currentIndex) {
                        listAdapter.notifyItemChanged(p)
                    }
                }
            }
        }
        updateSelectedCount(listAdapter.selectedPictures.size)
    }

    private fun onPictureTook(picture: File, isVideo: Boolean) {
        val duration = if (isVideo) {
            MediaMetadataRetriever().let { m ->
                m.setDataSource(picture.absolutePath)
                m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 1
            }
        } else {
            0
        }
        listAdapter.addPicture(
            Picture(
                picture.absolutePath,
                picture.length(),
                if (isVideo) PictureType.VIDEO else PictureType.PHOTO,
                if (isVideo) duration.formatToDurationString() else "",
                dataFormat.format(Date())
            )
        )
        if (isSingleSelector) {
            onSelectDone()
        }
    }

    private inner class PhotoListAdapter(private val initialSelected: List<Picture>?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        init {
            if (!initialSelected.isNullOrEmpty()) {
                updateSelectedCount(initialSelected.size)
            }
        }

        private var photoList: MutableList<Picture> = ArrayList()

        val selectedPictures: MutableList<Picture> = initialSelected?.toMutableList() ?: ArrayList()

        val albumOffset: Int
            get() = if (PhotoSelector.isAlbumTakePictureEnable) 1 else 0

        fun setPhotos(photos: List<Picture>, refresh: Boolean = true) {
            photoList.run {
                clear()
                addAll(photos)
            }
            if (refresh) {
                notifyDataSetChanged()
            }
        }

        fun addPicture(picture: Picture, refresh: Boolean = true) {
            photoList.add(0, picture)
            onPictureSelectedChanged(picture, true)
            if (refresh) {
                notifyItemInserted(1)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == 0 && PhotoSelector.isAlbumTakePictureEnable) 0 else 1
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 0) {
                TakePhotoHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.holder_kelin_photo_take_picture,
                        parent,
                        false
                    )
                )
            } else {
                PhotoHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.holder_kelin_photo_selector_picture,
                        parent,
                        false
                    )
                )
            }
        }

        override fun getItemCount(): Int {
            return photoList.size + albumOffset
        }

        fun notifyItemChanged(item: Picture) {
            notifyItemChanged(photoList.indexOf(item) + albumOffset)
        }

        fun getItem(position: Int): Picture {
            //减1是减去拍照按钮的位置。
            return photoList[position - albumOffset]
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is PhotoHolder) {
                holder.bindData(getItem(position))
            }
        }

        fun clearSelected() {
            selectedPictures.run {
                if (isNotEmpty()) {
                    initialSelected?.also { removeAll(it) }
                    val targetPositions = map { photoList.indexOf(it) }
                    clear()
                    initialSelected?.also { addAll(it) }
                    targetPositions.forEach {
                        notifyItemChanged(it + albumOffset)
                    }
                }
            }
        }

        fun isInitSelected(data: Picture): Boolean {
            return initialSelected?.contains(data) == true
        }
    }

    private inner class PhotoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.rlKelinPhotoSelectorChecker.setOnClickListener {
                listAdapter.getItem(layoutPosition).apply {
                    //如果被选中了的资源中没有当前的资源，那么就认为当前用户的目的是选中，否则就是取消选中。
                    val isSelected = !listAdapter.selectedPictures.contains(this)
                    if (isSelected && listAdapter.selectedPictures.size >= maxLength) {
                        Toast.makeText(applicationContext, "最多只能选择${maxLength}${if (albumType == AlbumType.PHOTO) "张" else "个"}$message", Toast.LENGTH_SHORT).show()
                    } else {
                        onPictureSelectedChanged(this, isSelected)
                        listAdapter.notifyItemChanged(layoutPosition)
                    }
                    if (isSingleSelector) {
                        onSelectDone()
                    }
                }
            }
            itemView.setOnClickListener {
                PhotoSelector.openPicturePreviewPage(requireActivity(), listOf(listAdapter.getItem(layoutPosition)))
            }
        }

        fun bindData(data: Picture) {
            itemView.also { iv ->
                Glide.with(iv.context)
                    .load(data.uri)
                    .apply(RequestOptions.centerCropTransform().placeholder(R.drawable.image_placeholder))
                    .into(iv.ivKelinPhotoSelectorPhotoView)
                val no = listAdapter.selectedPictures.indexOf(data)
                iv.pmKelinPhotoSelectorPhotoViewMask.isSelected = no >= 0
                val canOperation = !listAdapter.isInitSelected(data)
                iv.tvKelinPhotoSelectorChecker.run {
                    isSelected = no >= 0
                    text = if (no >= 0) {
                        (no + 1).toString()
                    } else {
                        null
                    }
                    isEnabled = canOperation
                }
                iv.rlKelinPhotoSelectorChecker.isEnabled = canOperation
                iv.tvKelinPhotoSelectorVideoDuration.apply {
                    visibility = if (data.type == PictureType.VIDEO) {
                        text = data.duration
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        }
    }

    private inner class TakePhotoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                if (listAdapter.selectedPictures.size >= maxLength) {
                    Toast.makeText(applicationContext, "最多只能选择${maxLength}${if (albumType == AlbumType.PHOTO) "张" else "个"}$message", Toast.LENGTH_SHORT).show()
                } else {
                    onTakePicture()
                }
            }
        }

        private fun onTakePicture() {
            if (albumType == AlbumType.VIDEO) {
                PhotoSelector.takeVideo(this@AlbumFragment) {
                    if (it != null) {
                        onPictureTook(it, true)
                    }
                }
            } else {
                PhotoSelector.takePhoto(this@AlbumFragment) {
                    if (it != null) {
                        onPictureTook(it, false)
                    }
                }
            }
        }
    }
}
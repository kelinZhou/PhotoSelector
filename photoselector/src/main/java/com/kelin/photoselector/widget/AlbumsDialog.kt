package com.kelin.photoselector.widget

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kelin.photoselector.R
import com.kelin.photoselector.databinding.HolderKelinPhotoSelectorAlbumBinding
import com.kelin.photoselector.model.Album

/**
 * **描述:** 相册弹窗。
 *
 * **创建人:** stephen
 *
 * **创建时间:** 2020/7/13 2:52 PM
 *
 * **版本:** v 1.0.0
 */
internal class AlbumsDialog(ctx: Context, private val onAlbumSelected: (album: Album) -> Unit) : Dialog(ctx, R.style.KelinPhotoSelectorBottomAnimDialog) {

    private var selectedPosition = 0

    private var albumsList:List<Album> = emptyList()

    private val listAdapter by lazy {
        object : RecyclerView.Adapter<AlbumHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumHolder {
                return AlbumHolder(HolderKelinPhotoSelectorAlbumBinding.inflate(LayoutInflater.from(context), parent, false))
            }

            override fun getItemCount(): Int {
                return albumsList.size
            }

            @SuppressLint("SetTextI18n")
            override fun onBindViewHolder(holder: AlbumHolder, position: Int) {
                holder.vb.also { vb ->
                    val album = albumsList[position]
                    vb.ivKelinPhotoSelectorPhotoView.load(album.cover.path){
                        placeholder(R.drawable.image_placeholder)
                    }
                    vb.tvKelinPhotoSelectorAlbumName.text = album.name
                    vb.tvKelinPhotoSelectorAlbumPath.text = album.path
                    vb.tvKelinPhotoSelectorCount.text = "共${album.pictures.size}个资源"
                    vb.ivKelinPhotoSelectorAlbumChecker.visibility = if (position == selectedPosition) {
                        View.VISIBLE
                    } else {
                        View.INVISIBLE
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_kelin_photo_selector_albums)
        val window = window
        if (window != null) {
            window.attributes = window.attributes.apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.BOTTOM
            }
        }
        setCancelable(true)
        findViewById<RecyclerView>(R.id.rlKelinPhotoSelectorAlbums).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
            itemAnimator = null
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun show(albums: List<Album>, selectedAlbum: String?) {
        if (albums.isNotEmpty()) {
            selectedPosition = albums.indexOfFirst { it.name == selectedAlbum }
            albumsList = albums
            listAdapter.notifyDataSetChanged()
            super.show()
        }
    }

    private inner class AlbumHolder(val vb: HolderKelinPhotoSelectorAlbumBinding) : RecyclerView.ViewHolder(vb.root) {
        init {
            itemView.setOnClickListener {
                val layoutPosition = layoutPosition
                if (layoutPosition != selectedPosition) {
                    val old = selectedPosition
                    selectedPosition = layoutPosition
                    listAdapter.notifyItemChanged(old)
                    listAdapter.notifyItemChanged(selectedPosition)
                    onAlbumSelected(albumsList[layoutPosition])
                }
                itemView.post { dismiss() }
            }
        }
    }
}
package com.kelin.photoselector.widget

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.kelin.photoselector.R
import com.kelin.photoselector.model.Album
import kotlinx.android.synthetic.main.dialog_kelin_photo_selector_albums.*
import kotlinx.android.synthetic.main.holder_kelin_photo_selector_album.view.*

/**
 * **描述:** 相册弹窗。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/7/13 2:52 PM
 *
 * **版本:** v 1.0.0
 */
internal class AlbumsDialog(ctx: Context, private val albums: List<Album>, private val selectedAlbum: String, private val onAlbumSelected: (album: Album) -> Unit) : Dialog(ctx, R.style.KelinPhotoSelectorBottomAnimDialog) {

    private var selectedPosition = albums.indexOfFirst { it.name == selectedAlbum }.let { if (it < 0) 0 else it }

    private val listAdapter by lazy {
        object : RecyclerView.Adapter<AlbumHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumHolder {
                return AlbumHolder(LayoutInflater.from(context).inflate(R.layout.holder_kelin_photo_selector_album, parent, false))
            }

            override fun getItemCount(): Int {
                return albums.size
            }

            @SuppressLint("SetTextI18n")
            override fun onBindViewHolder(holder: AlbumHolder, position: Int) {
                holder.itemView.also { iv ->
                    val album = albums[position]
                    Glide.with(iv.context)
                        .load(album.cover.path)
                        .apply(RequestOptions().centerCrop().placeholder(R.drawable.image_placeholder))
                        .into(iv.ivKelinPhotoSelectorPhotoView)
                    iv.tvKelinPhotoSelectorAlbumName.text = album.name
                    iv.tvKelinPhotoSelectorAlbumPath.text = album.path
                    iv.tvKelinPhotoSelectorCount.text = "共${album.pictures.size}个资源"
                    iv.ivKelinPhotoSelectorAlbumChecker.visibility = if (position == selectedPosition) {
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
        rlKelinPhotoSelectorAlbums.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
            itemAnimator = null
        }
    }


    private inner class AlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                val layoutPosition = layoutPosition
                if (layoutPosition != selectedPosition) {
                    val old = selectedPosition
                    selectedPosition = layoutPosition
                    listAdapter.notifyItemChanged(old)
                    listAdapter.notifyItemChanged(selectedPosition)
                    onAlbumSelected(albums[layoutPosition])
                }
                itemView.post { dismiss() }
            }
        }
    }
}
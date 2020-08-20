package com.kelin.photoselector.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.EventLogger
import com.kelin.photoselector.R
import kotlinx.android.synthetic.main.fragment_kelin_photo_selector_play_video.view.*
import java.io.File

/**
 * **描述:** 视频播放页面。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2020/8/19 5:35 PM
 *
 * **版本:** v 1.0.0
 */
internal class PlayVideoFragment : BasePhotoSelectorFragment() {

    companion object {

        private const val KEY_VIDEO_URI = "key_kelin_photo_selector_video_uri"

        fun configurationPlayVideoIntent(intent: Intent, uri: String) {
            intent.putExtra(KEY_VIDEO_URI, uri)
        }
    }

    private var player: ExoPlayer? = null
    /**
     * 用来记录用户是否手动按下了暂停按钮。
     */
    private var manualPause = false

    private val playerEventListener by lazy { PlayerEventListener() }

    private val uri by lazy {
        requireArguments().getString(KEY_VIDEO_URI)?.let { path ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || path.startsWith("http")) {
                Uri.parse(path)
            } else {
                Uri.fromFile(File(path))
            }
        } ?: throw NullPointerException("The uri must not be null!")
    }

    override val rootLayoutRes: Int
        get() = R.layout.fragment_kelin_photo_selector_play_video

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        player?.release()
        player = SimpleExoPlayer.Builder(requireContext()).build().apply {
            addAnalyticsListener(EventLogger(null))
            addListener(playerEventListener)
            playWhenReady = true
            prepare(ProgressiveMediaSource.Factory {
                DefaultDataSourceFactory(context, "exoplayer-codelab").createDataSource()
            }.createMediaSource(uri))
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.pvKelinPhotoSelectorVideoPlayer.also { pv ->
            pv.setControlDispatcher(object : DefaultControlDispatcher(){
                override fun dispatchSetPlayWhenReady(player: Player, playWhenReady: Boolean): Boolean {
                    manualPause = !playWhenReady //记录用户是否手动暂停了。
                    return super.dispatchSetPlayWhenReady(player, playWhenReady)
                }
            })
            pv.player = player
        }
    }

    override fun onResume() {
        super.onResume()
        if (!manualPause) {
            player?.playWhenReady = true
        }
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.apply {
            removeListener(playerEventListener)
            release()
        }
    }

    private inner class PlayerEventListener : Player.EventListener{
        override fun onPlayerError(error: ExoPlaybackException) {
            Toast.makeText(applicationContext, "播放失败", Toast.LENGTH_SHORT).show()
        }
    }
}
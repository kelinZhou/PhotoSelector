package com.kelin.photoselector.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Log
import com.kelin.photoselector.databinding.FragmentKelinPhotoSelectorPlayVideoBinding
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
internal class PlayVideoFragment : BasePhotoSelectorFragment<FragmentKelinPhotoSelectorPlayVideoBinding>() {

    companion object {

        private const val KEY_VIDEO_URI = "key_kelin_photo_selector_video_uri"

        fun configurationPlayVideoIntent(intent: Intent, uri: String) {
            intent.putExtra(KEY_VIDEO_URI, uri)
        }
    }

    private val exoPlayer: ExoPlayer by lazy { ExoPlayer.Builder(requireContext()).build() }
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

    override fun generateViewBinding(inflater: LayoutInflater, container: ViewGroup?, attachToParent: Boolean): FragmentKelinPhotoSelectorPlayVideoBinding {
        return  FragmentKelinPhotoSelectorPlayVideoBinding.inflate(inflater, container, attachToParent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        exoPlayer.apply {
            addAnalyticsListener(EventLogger())
            addListener(playerEventListener)
            playWhenReady = true
            setMediaSource(
                ProgressiveMediaSource.Factory{
                    DefaultDataSource(view.context, "exoplayer-codelab", false)
                }.createMediaSource(MediaItem.fromUri(uri))
            )
            prepare()
        }
        vb.pvKelinPhotoSelectorVideoPlayer.run {
            player = exoPlayer
            try {
                val controller = javaClass.getDeclaredField("controller").let {
                    it.isAccessible = true
                    it.get(this)
                }
                controller.javaClass.getDeclaredField("playPauseButton").let {
                    it.isAccessible = true
                    it.get(controller)as View
                }.setOnClickListener {
                    controller.javaClass.getDeclaredMethod("dispatchPlayPause", Player::class.java).also { method ->
                        val state = exoPlayer.playbackState
                        val wantPlay = state == Player.STATE_IDLE || state == Player.STATE_ENDED || !exoPlayer.playWhenReady
                        manualPause = !(wantPlay)
                        method.isAccessible = true
                        method.invoke(controller, exoPlayer)
                        Log.d("VideoPlayer", "${wantPlay}|${state == Player.STATE_IDLE || state == Player.STATE_ENDED || !exoPlayer.playWhenReady}|${exoPlayer.isPlaying}")
                    }
                }
            } catch (_: Exception) { }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("VideoPlayer", "onResume：${manualPause}")
        if (!manualPause) {
            exoPlayer.playWhenReady = true
            vb.pvKelinPhotoSelectorVideoPlayer.hideController()
        }
    }

    override fun onPause() {
        super.onPause()
        exoPlayer.playWhenReady = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        exoPlayer.apply {
            removeListener(playerEventListener)
            release()
        }
    }

    private inner class PlayerEventListener : Player.Listener{
        override fun onPlayerError(error: PlaybackException) {
            Toast.makeText(applicationContext, "播放失败", Toast.LENGTH_SHORT).show()
        }
    }
}
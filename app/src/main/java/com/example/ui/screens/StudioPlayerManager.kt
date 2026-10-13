package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class StudioPlayerManager(private val context: Context) {
    var exoPlayer: ExoPlayer? = null
        private set

    var currentSpeed: Float = 1.0f
        private set

    fun initializePlayer(
        videoUriString: String?,
        initialSpeed: Float = 1.0f,
        playWhenReady: Boolean = false,
        onPlayStateChange: (isPlaying: Boolean) -> Unit
    ): ExoPlayer {
        releasePlayer()
        currentSpeed = initialSpeed

        val player = ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playbackParameters = PlaybackParameters(initialSpeed)
            this.playWhenReady = playWhenReady

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    onPlayStateChange(isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        seekTo(0)
                        pause()
                        onPlayStateChange(false)
                    }
                }
            })

            if (!videoUriString.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(videoUriString)
                    val mediaItem = MediaItem.fromUri(uri)
                    setMediaItem(mediaItem)
                    prepare()
                } catch (_: Exception) {
                }
            }
        }

        exoPlayer = player
        return player
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun releasePlayer() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
    }
}

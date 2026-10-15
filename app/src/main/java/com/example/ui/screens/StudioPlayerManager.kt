package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class StudioPlayerManager(private val context: Context) {
    var exoPlayer by mutableStateOf<ExoPlayer?>(null)
        private set

    var currentSpeed by mutableFloatStateOf(1.0f)
        private set

    var isReady by mutableStateOf(false)
        private set

    fun initializePlayer(
        videoUriString: String?,
        initialSpeed: Float = 1.0f,
        playWhenReady: Boolean = false,
        onPlayStateChange: (isPlaying: Boolean) -> Unit
    ): ExoPlayer {
        releasePlayer()
        currentSpeed = initialSpeed
        isReady = false

        val player = ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playbackParameters = PlaybackParameters(initialSpeed)
            this.playWhenReady = playWhenReady

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    onPlayStateChange(isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isReady = true
                        }
                        Player.STATE_ENDED -> {
                            seekTo(0)
                            pause()
                            onPlayStateChange(false)
                        }
                        else -> {}
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

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
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
        try {
            exoPlayer?.stop()
            exoPlayer?.release()
        } catch (_: Exception) {
        } finally {
            exoPlayer = null
            isReady = false
        }
    }
}

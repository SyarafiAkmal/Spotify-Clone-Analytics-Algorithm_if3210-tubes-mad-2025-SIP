package com.example.purrytify.utils

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MusicPlayerManager private constructor() {
    private var mediaPlayer: MediaPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    private val _currentSongInfo = MutableStateFlow<SongInfo?>(null)
    val currentSongInfo: StateFlow<SongInfo?> = _currentSongInfo

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateProgressTask: Runnable

    data class SongInfo(
        val id: Int,
        val title: String,
        val artist: String,
        val artworkResId: Int
    )

    init {
        updateProgressTask = Runnable {
            mediaPlayer?.let {
                _currentPosition.value = it.currentPosition
                handler.postDelayed(updateProgressTask, 100) // Update every 100ms for smoother progress
            }
        }
    }

    fun loadSong(context: Context, resourceId: Int, title: String, artist: String, artworkResId: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, resourceId)
        mediaPlayer?.let { player ->
            _duration.value = player.duration
            _currentPosition.value = 0
            _currentSongInfo.value = SongInfo(resourceId, title, artist, artworkResId)

            player.setOnCompletionListener {
                // When the song ends
                _isPlaying.value = false
                _currentPosition.value = 0
                stopProgressTracking()
            }
        }
    }

    fun play() {
        mediaPlayer?.let {
            if (!_isPlaying.value) {
                it.start()
                _isPlaying.value = true
                startProgressTracking()
            }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (_isPlaying.value && it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                stopProgressTracking()
            }
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        _currentPosition.value = position
    }

    private fun startProgressTracking() {
        handler.post(updateProgressTask)
    }

    private fun stopProgressTracking() {
        handler.removeCallbacks(updateProgressTask)
    }

    fun release() {
        stopProgressTracking()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    companion object {
        @Volatile
        private var instance: MusicPlayerManager? = null

        fun getInstance(): MusicPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayerManager().also { instance = it }
            }
        }
    }
}
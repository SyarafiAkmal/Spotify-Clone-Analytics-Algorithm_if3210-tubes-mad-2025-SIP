package com.example.purrytify.utils

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import com.example.purrytify.data.local.db.entities.SongEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.MainActivity
import com.example.purrytify.models.OnlineSong
import com.example.purrytify.views.MusicDbViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.getValue

class MusicPlayerManager private constructor() {
    private var mediaPlayer: MediaPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _currentSongId = MutableStateFlow(0)
    val currentSongId: StateFlow<Int> = _currentSongId

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    private val _currentSongInfo = MutableStateFlow<SongEntity?>(null)
    val currentSongInfo: StateFlow<SongEntity?> = _currentSongInfo

    private val _songQueue: MutableList<SongEntity> = mutableListOf()
    val songQueue: List<SongEntity> get() = _songQueue

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateProgressTask: Runnable

    private val _startPos = MutableStateFlow(0)
    val startPos: StateFlow<Int> = _startPos

    private val _timeListened = MutableStateFlow(0)
    val timeListened: StateFlow<Int> = _timeListened

    init {
        updateProgressTask = Runnable {
            mediaPlayer?.let {
                _currentPosition.value = it.currentPosition
                handler.postDelayed(updateProgressTask, 100)
            }
        }
    }

    fun loadQueue() {
//        _songQueue.
    }

    fun resetTimeListened() {
        _timeListened.value = 0
    }

    fun loadSong(context: Context, song: SongEntity) {
        try {
            _currentSongId.value = song.id

            if (context is MainActivity) {
                val musicDB: MusicDbViewModel = context.getMusicDB()

                context.lifecycleScope.launch {
                    if (musicDB.getStatusSongById(song.id, "library") !== null) {
                        musicDB.updateStatus(song, "listened")
                    }
                    if (musicDB.isSongExistForUser(song)) {
                        context.updateRecentlyPlayedInHome(song)
                        musicDB.addTimesPlayed(song.id)
                        if(!musicDB.isUserSongActivityExist(currentSongInfo.value!!)) {
                            musicDB.registerSongActivity(song.id, DateTimeUtils.getCurrentTimeIso())
                        }
                        Toast.makeText(context, "played songId: ${song.id} ${musicDB.getSongUploader(song.id).timesPlayed} times",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }

            pause()
            val newPlayer = MediaPlayer.create(context, song.uri.toUri())

            if (newPlayer != null) {
                mediaPlayer?.release()
                mediaPlayer = newPlayer

                _duration.value = newPlayer.duration
                _currentPosition.value = 0
                _currentSongInfo.value = song

                newPlayer.setOnCompletionListener {
                    _timeListened.value += currentPosition.value - _startPos.value
                    _isPlaying.value = false
                    _currentPosition.value = 0
                    _startPos.value = currentPosition.value
                    stopProgressTracking()
                }

                play()
            } else {
                println("Error: Could not create MediaPlayer for URI: ${song.uri}")
            }
        } catch (e: Exception) {
            println("Error loading song: ${e.message}")
            e.printStackTrace()
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
                _timeListened.value += currentPosition.value - _startPos.value
                _startPos.value = currentPosition.value
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

    fun clearCurrentSong() {
        _currentSongInfo.value = null
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

    fun onlineToEntity(onlineSong: OnlineSong): SongEntity {
        return SongEntity(
            id = onlineSong.id,
            title = onlineSong.title,
            artist = onlineSong.artist,
            artworkURI =  onlineSong.artwork,
            uri = onlineSong.url,
            duration = parseDurationToMillis(onlineSong.duration)
        )
    }

    private fun parseDurationToMillis(durationString: String): Long {
        val parts = durationString.split(":")
        return try {
            val minutes = parts[0].toIntOrNull() ?: 0
            val seconds = parts.getOrNull(1)?.toIntOrNull() ?: 0
            (minutes * 60 + seconds) * 1000L
        } catch (e: Exception) {
            0L
        }
    }
}
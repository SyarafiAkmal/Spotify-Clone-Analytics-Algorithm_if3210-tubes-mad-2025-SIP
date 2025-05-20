package com.example.purrytify.ui.home

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.purrytify.api.ApiClient
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.models.OnlineSong
import com.example.purrytify.views.MusicDbViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _userRecentPlayed = MutableStateFlow<List<SongEntity>>(emptyList())
    val userRecentPlayed: StateFlow<List<SongEntity>> = _userRecentPlayed.asStateFlow()
    private val _userAllSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val userAllSongs: StateFlow<List<SongEntity>> = _userAllSongs.asStateFlow()
    private val musicDbViewModel = MusicDbViewModel(application)
    private var packageName: String = "com.example.purrytify"

    fun setPackageName(pkgName: String) {
        packageName = pkgName
    }

    fun addToRecentPlayed(song: SongEntity) {
        _userRecentPlayed.value = _userRecentPlayed.value.toMutableList().apply {
            // Remove existing song with the same ID if it exists
            removeAll { it.id == song.id }

            // Add the new song at the beginning of the list
            add(0, song)
        }
        musicDbViewModel.updateLastPlayed(song)
    }

    fun initData() {
        viewModelScope.launch {
            try {
                launch {
                    musicDbViewModel.allSongs.take(1).collect { songs ->
                        _userAllSongs.value = songs.take(10)
//                        Log.d("HomeViewModel", "Home All songs: ${songs.size}")
                    }
                }

                launch {
                    musicDbViewModel.recentSongs.take(1).collect { songs ->
                        _userRecentPlayed.value = songs.take(10)
//                        Log.d("HomeViewModel", "Home Recent songs: ${songs.size}")
                    }
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error initializing data", e)
                e.printStackTrace()

            }
        }
    }
}
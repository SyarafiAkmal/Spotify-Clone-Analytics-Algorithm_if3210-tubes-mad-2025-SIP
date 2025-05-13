package com.example.purrytify.ui.library

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.views.MusicDbViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private var _userLibrary = MutableStateFlow<List<SongEntity>>(emptyList())
    var userLibrary: StateFlow<List<SongEntity>> = _userLibrary.asStateFlow()
    private val _userLiked = MutableStateFlow<List<SongEntity>>(emptyList())
    val userLiked: StateFlow<List<SongEntity>> = _userLiked.asStateFlow()
    private val musicDbViewModel = MusicDbViewModel(application)
    private var packageName: String = "com.example.purrytify" // Default value
    private var userData: SharedPreferences? = null

    fun setPackageName(pkgName: String) {
        packageName = pkgName
    }

    fun setUserData(user: SharedPreferences) {
        userData = user
    }

    fun initData() {
        viewModelScope.launch {
            try {
                Log.d("LibraryViewModel", "Attempting to initialize data")
                val userEmail = "13522042@std.stei.itb.ac.id"

                launch {
                    // Get library songs
                    musicDbViewModel.allSongs.take(1).collect { songs ->
                        _userLibrary.value = songs

                    }
                }

                launch {
                    // Get liked songs
                    musicDbViewModel.likedSongs.take(1).collect { songs ->
                        _userLiked.value = songs
                        Log.d("LibraryViewModel", "Liked songs: ${songs.size}")
                    }
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error initializing data", e)
                e.printStackTrace()
            }
        }
    }

    fun addToLibrary(song: SongEntity) {
        _userLibrary.value = _userLibrary.value.toMutableList().apply {
            add(0, song)
        }
    }

    fun addToLiked(song: SongEntity) {
        _userLiked.value = _userLiked.value.toMutableList().apply {
            add(0, song)
        }
    }

    fun deleteFromLiked(song: SongEntity) {
        _userLiked.value = _userLiked.value.toMutableList().apply {
            remove(song)
        }
    }

    fun insertSong(song: SongEntity) {
        viewModelScope.launch {
            // Use your existing method to insert song
            musicDbViewModel.checkAndInsertSong(song)
            // Refresh library data
        }
    }
}
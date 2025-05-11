package com.example.purrytify.ui.library

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.viewmodel.MusicDbViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val _userLibrary = MutableStateFlow<List<SongEntity>>(emptyList())
    val userLibrary: StateFlow<List<SongEntity>> = _userLibrary.asStateFlow()
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
                    musicDbViewModel.librarySongs.take(1).collect { songs ->
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

                // Error handling

            }
        }
    }

    fun addSongToUserLibrary(song: SongEntity) {
        viewModelScope.launch {
            try {
                // Check if the song already exists in the library
                val isAlreadyInLibrary = _userLibrary.value.any {
                    it.title == song.title && it.artist == song.artist
                }

                if (!isAlreadyInLibrary) {
                    // Insert the song to the user's library
                    musicDbViewModel.insertSongToLibrary(song)
                    val currentList = _userLibrary.value
                    _userLibrary.value = currentList + song
                    // Refresh library data
                    initData()
                } else {
                    Log.d("LibraryViewModel", "Song already exists in user library: ${song.title}")
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error adding song to user library", e)
                e.printStackTrace()
            }
        }
    }

    fun insertSong(song: SongEntity) {
        viewModelScope.launch {
            // Use your existing method to insert song
            musicDbViewModel.insertSong(song, "13522042@std.stei.itb.ac.id")

            // Refresh library data
            initData()
        }
    }
}
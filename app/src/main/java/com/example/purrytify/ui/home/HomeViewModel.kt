package com.example.purrytify.ui.home

import android.app.Application
import android.content.SharedPreferences
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import android.util.Log
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.viewmodel.MusicDbViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _userRecentPlayed = MutableStateFlow<List<SongEntity>>(emptyList())
    val userRecentPlayed: StateFlow<List<SongEntity>> = _userRecentPlayed.asStateFlow()
    private val _userAllSongs = MutableStateFlow<List<SongEntity>>(emptyList())
    val userAllSongs: StateFlow<List<SongEntity>> = _userAllSongs.asStateFlow()
    private val musicDbViewModel = MusicDbViewModel(application)
    private var packageName: String = "com.example.purrytify"
    private var userData: SharedPreferences? = null

    fun setPackageName(pkgName: String) {
        packageName = pkgName
    }

    fun setUserData(user: SharedPreferences) {
        userData = user
    }

    fun addToRecentPlayed(song: SongEntity) {
        _userRecentPlayed.value = _userRecentPlayed.value.toMutableList().apply {
            // Remove existing song with the same ID if it exists
            removeAll { it.id == song.id }

            // Add the new song at the beginning of the list
            add(0, song)
        }
        musicDbViewModel.addRecentPlays(_userRecentPlayed.value, "13522042@std.stei.itb.ac.id")
    }

    fun initData() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Attempting to initialize data")
                val userEmail = "13522042@std.stei.itb.ac.id"

                launch {
                    musicDbViewModel.allSongs.take(1).collect { songs ->
                        _userAllSongs.value = songs
                        Log.d("HomeViewModel", "Home All songs: ${songs.size}")
                        songs.forEach{ song ->
                            Log.d("HomeViewModel", "Title-id: ${song.title}-${song.id}")
                        }
                    }
                }

                launch {
                    musicDbViewModel.recentSongs.take(1).collect { songs ->
                        _userRecentPlayed.value = songs
                        Log.d("HomeViewModel", "Home Recent songs: ${songs.size}")
                    }
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error initializing data", e)
                e.printStackTrace()

            }
        }
    }
}
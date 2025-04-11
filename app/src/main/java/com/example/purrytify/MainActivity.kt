package com.example.purrytify

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.purrytify.databinding.ActivityMainBinding
import com.example.purrytify.utils.MusicPlayerManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val musicPlayerManager = MusicPlayerManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)

        if (!isLoggedIn) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load all necessary data
        loadInitialData()
        setupMusicPlayer()
        setupNavigationAndListeners()
    }

    private fun loadInitialData() {
        // Simulate loading initial data
        // In a real app, this would likely involve database or network calls
        val defaultSongs = listOf(
            SongData(
                id = 1,
                title = "Tung Tung",
                artist = "Artist Name",
                albumArt = R.drawable.starboy_album,
                audioResource = R.raw.tung_tung
            ),
            // Add more songs as needed
        )

        // You might want to store these in a repository or ViewModel
        // For now, we'll just use the first song
        val defaultSong = defaultSongs.firstOrNull()
        defaultSong?.let { song ->
            musicPlayerManager.loadSong(
                this,
                song.audioResource,
                song.title,
                song.artist,
                song.albumArt
            )
        }
    }

    private fun setupMusicPlayer() {
        // Observe playback state changes
        lifecycleScope.launch {
            musicPlayerManager.isPlaying.collect { isPlaying ->
                updatePlayButtonIcon(isPlaying)
                updateMiniPlayerUI()
            }
        }

        // Optional: Observe current song changes
        lifecycleScope.launch {
            musicPlayerManager.currentSongInfo.collect { songInfo ->
                updateMiniPlayerUI()
            }
        }
    }

    fun toggleMiniPlayer(show: Boolean) {
        binding.miniPlayerContainer?.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun updateMiniPlayerUI() {
        val currentSong = musicPlayerManager.currentSongInfo.value
        currentSong?.let { song ->
            // Update mini player UI elements
            binding.miniPlayerSongTitle?.text = song.title
            binding.miniPlayerArtistName?.text = song.artist
            binding.miniPlayerAlbumCover?.setImageResource(song.artworkResId)
        }
    }

    private fun setupNavigationAndListeners() {
        // Set up play/pause button
        binding.playButton?.setOnClickListener {
            if (musicPlayerManager.isPlaying.value) {
                musicPlayerManager.pause()
                Toast.makeText(this, "Music paused", Toast.LENGTH_SHORT).show()
            } else {
                musicPlayerManager.play()
                Toast.makeText(this, "Music playing", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up mini player click
        binding.miniPlayerClick?.setOnClickListener {
            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            navController.navigate(R.id.navigation_track_view)
        }

        // Set up bottom navigation
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Add destination change listener for mini player visibility
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_home,
                R.id.navigation_library -> {
                    // Show mini player for these destinations
                    toggleMiniPlayer(true)
                }
                R.id.navigation_track_view,
                R.id.navigation_profile -> {
                    // Hide mini player for track view
                    toggleMiniPlayer(false)
                }
            }
        }

        // Setup bottom navigation with NavController
        navView.setupWithNavController(navController)
    }

    private fun updatePlayButtonIcon(isPlaying: Boolean) {
        binding.playButton?.setImageResource(
            if (isPlaying) R.drawable.pause else R.drawable.play
        )
    }

    // Data class to represent song information
    data class SongData(
        val id: Int,
        val title: String,
        val artist: String,
        val albumArt: Int,
        val audioResource: Int
    )
}
package com.example.purrytify

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.purrytify.data.local.db.entities.SongEntity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.purrytify.databinding.ActivityMainBinding
import com.example.purrytify.utils.MusicPlayerManager
import kotlinx.coroutines.flow.collect
import android.util.Log
import android.widget.LinearLayout
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.example.purrytify.viewmodel.AlbumItemView
import com.example.purrytify.viewmodel.MusicDbViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val musicPlayerManager = MusicPlayerManager.getInstance()
    private val musicDbViewModel: MusicDbViewModel by viewModels()
    val userSongs = MutableStateFlow<List<SongEntity>>(emptyList())

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
        lifecycleScope.launch {
            userSongs.value = listOf(
                SongEntity(
                    title = "Starboy",
                    artist = "The Weeknd, Daft Punk",
                    artworkURI = "android.resource://${packageName}/drawable/starboy_album",
                    uri = "android.resource://${packageName}/raw/starboy",
                    duration = 0
                ),
                SongEntity(
                    title = "Kiss of Life",
                    artist = "Sade",
                    artworkURI = "android.resource://${packageName}/drawable/album_kiss_of_life",
                    uri = "android.resource://${packageName}/raw/kiss_of_life",
                    duration = 0
                ),
                SongEntity(
                    title = "BEST INTEREST",
                    artist = "Tyler, The Creator",
                    artworkURI = "android.resource://${packageName}/drawable/album_best_interest",
                    uri = "android.resource://${packageName}/raw/kiss_of_life",
                    duration = 0
                )
            )

            // Seed the database first
//            val seedSong = SongEntity(
//                title = "Starboy",
//                artist = "The Weeknd",
//                artworkURI = "android.resource://${packageName}/drawable/starboy_album",
//                uri = "android.resource://${packageName}/raw/starboy",
//                duration = 0
//            )
            // musicDbViewModel.insertSong(seedSong, "13522042@std.stei.itb.ac.id")

            musicPlayerManager.clearCurrentSong()

            // Query to get songs from database according to user
            musicDbViewModel.allSongs.collect { songs ->
                val songsToLoad = if (songs.isEmpty()) {
                    listOf(
                        SongEntity(
                            title = "Tung Tung",
                            artist = "Artist Name",
                            artworkURI = "android.resource://${packageName}/drawable/starboy_album",
                            uri = "android.resource://${packageName}/raw/tung_tung",
                            duration = 0
                        )
                    )
                } else {
                    // Ensure songs are SongEntity
                    songs.map { song ->
                        SongEntity(
                            title = song.title,
                            artist = song.artist,
                            artworkURI = song.artworkURI ?: "",
                            uri = song.uri,
                            duration = song.duration
                        )
                    }
                }

                // Load the first song
//                 val defaultSong = seedSong
//                 defaultSong?.let { songEntity ->
//                   musicPlayerManager.loadSong(this@MainActivity, songEntity)
//                 }
            }
        }
    }

    private fun getDrawableResourceFromUri(uri: String): Int {
        return try {
            val resourceName = uri.substringAfterLast("/")
            val drawableResourceId = resources.getIdentifier(
                resourceName,
                "drawable",
                packageName
            )
            drawableResourceId.takeIf { it != 0 } ?: R.drawable.logo
        } catch (e: Exception) {
            R.drawable.logo
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
        val currentDestination = findNavController(R.id.nav_host_fragment_activity_main).currentDestination?.id

        // Check both conditions: if there's a song AND not on player screen
        if (currentSong != null && currentDestination != R.id.navigation_track_view && currentDestination != R.id.navigation_profile) {
            binding.miniPlayerSongTitle?.text = currentSong.title
            binding.miniPlayerArtistName?.text = currentSong.artist
            binding.miniPlayerAlbumCover?.setImageURI(currentSong.artworkURI.toUri())
            toggleMiniPlayer(true)
        } else {
            toggleMiniPlayer(false)
        }
    }

    private fun setupNavigationAndListeners() {
        // Set up play/pause button
        binding.playButton?.setOnClickListener {
            if (musicPlayerManager.isPlaying.value) {
                musicPlayerManager.pause()
//                Toast.makeText(this, "Music paused", Toast.LENGTH_SHORT).show()
            } else {
                musicPlayerManager.play()
//                Toast.makeText(this, "Music playing", Toast.LENGTH_SHORT).show()
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
                    updateMiniPlayerUI()
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
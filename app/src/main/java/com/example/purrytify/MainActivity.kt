package com.example.purrytify

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.purrytify.data.local.db.entities.SongEntity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.purrytify.databinding.ActivityMainBinding
import com.example.purrytify.utils.MusicPlayerManager
import android.util.Log
import kotlinx.coroutines.launch
import com.example.purrytify.viewmodel.MusicDbViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.purrytify.ui.home.HomeViewModel
import com.example.purrytify.ui.library.LibraryViewModel
import com.example.purrytify.ui.trackview.TrackViewDialogFragment
import com.example.purrytify.utils.ImageUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val musicPlayerManager = MusicPlayerManager.getInstance()
    private val homeViewModel: HomeViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()
    private val musicDBViewModel: MusicDbViewModel by viewModels()
    val userLibrary = MutableStateFlow<List<SongEntity>>(emptyList())

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

        loadInitialData()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setupLibraryObservation()
        setContentView(binding.root)
        setupNavigationAndListeners()
        setupMusicPlayer()
    }

    private fun loadInitialData() {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

            Toast.makeText(this@MainActivity, "${prefs.getString("email", "none")}", Toast.LENGTH_SHORT).show()

            val songsToInsert = listOf(
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
                    uri = "android.resource://${packageName}/raw/best_interest",
                    duration = 0
                )
            )

            // Insert each song
//            musicDbViewModel.insertSongs(songsToInsert, userEmail)

            musicPlayerManager.clearCurrentSong()
        }
    }



    fun updateRecentlyPlayedInHome(song: SongEntity) {
        homeViewModel.addToRecentPlayed(song)
    }

    private fun setupMusicPlayer() {
        lifecycleScope.launch {
            musicPlayerManager.isPlaying.collect { isPlaying ->
                updatePlayButtonIcon(isPlaying)
//                updateMiniPlayerUI()
            }
        }

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

        if (currentSong != null) {
            binding.miniPlayerSongTitle?.text = currentSong.title
            binding.miniPlayerArtistName?.text = currentSong.artist

            val resId = ImageUtils.loadImage(
                this,
                currentSong.artworkURI,
                binding.miniPlayerAlbumCover
            )
            binding.miniPlayerAlbumCover.setImageResource(resId)

            toggleMiniPlayer(true)
        } else {
            toggleMiniPlayer(false)
        }
    }

    private fun setupNavigationAndListeners() {
        binding.playButton?.setOnClickListener {
            if (musicPlayerManager.isPlaying.value) {
                musicPlayerManager.pause()
            } else {
                musicPlayerManager.play()
            }
        }

        binding.addToLibrary?.setOnClickListener {
            val currentSong = musicPlayerManager.currentSongInfo.value
            currentSong?.let { song ->
                // Convert MusicPlayerManager's current song to SongEntity if needed
                val songEntity = SongEntity(
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    artworkURI = song.artworkURI,
                    uri = song.uri,
                    duration = song.duration
                )

                // Add to library
                addSongToLibrary(songEntity)
            }
        }

        binding.miniPlayerClick?.setOnClickListener {
            // Show the full-screen dialog
            TrackViewDialogFragment().show(supportFragmentManager, "track_view_dialog")
        }

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_home,
                R.id.navigation_library -> {
//                    updateMiniPlayerUI()
                    if(musicPlayerManager.isPlaying.value){
                        toggleMiniPlayer(true)
                    }
                }
                R.id.navigation_profile -> {
                    toggleMiniPlayer(false)
                }
            }
        }

        navView.setupWithNavController(navController)
    }

    private fun updatePlayButtonIcon(isPlaying: Boolean) {
        binding.playButton?.setImageResource(
            if (isPlaying) R.drawable.pause else R.drawable.play
        )
    }

    private fun addSongToLibrary(song: SongEntity) {
        // Check if song is already in library to prevent duplicates
        lifecycleScope.launch {
            val isAlreadyInLibrary = userLibrary.value.any { it.title == song.title && it.artist == song.artist }

            if (!isAlreadyInLibrary) {
                // Insert the song into the library
                libraryViewModel.addSongToUserLibrary(song)

                // Show a toast to notify the user
            } else {
                // Optionally show a different toast if song is already in library
                Toast.makeText(
                    this@MainActivity,
                    "${song.title} is already in your Library",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun observeLibraryChanges() {
        lifecycleScope.launch {
            libraryViewModel.userLibrary.collect { library ->
                userLibrary.value = library
                Log.d("MainActivity", "Library updated: ${library.size} songs")
            }
        }
    }

    // Call this in onCreate after initializing viewModels
    private fun setupLibraryObservation() {
        libraryViewModel.initData()
        observeLibraryChanges()
    }

    //    private fun getDrawableResourceFromUri(songUri: String): Int {
//        return try {
//            val resourceName = songUri.substringAfterLast("/")
//            val drawableResourceId = resources.getIdentifier(
//                resourceName,
//                "drawable",
//                packageName
//            )
//            drawableResourceId.takeIf { it != 0 } ?: R.drawable.logo
//        } catch (e: Exception) {
//            R.drawable.logo
//        }
//    }

}
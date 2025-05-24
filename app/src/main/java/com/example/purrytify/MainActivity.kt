package com.example.purrytify

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.core.content.edit
import com.example.purrytify.api.ApiClient
import com.example.purrytify.models.Login
import com.example.purrytify.models.RefreshToken
import com.example.purrytify.models.Token
import com.example.purrytify.models.VerifyToken
import kotlinx.coroutines.launch
import com.example.purrytify.views.MusicDbViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.purrytify.ui.home.HomeViewModel
import com.example.purrytify.ui.library.LibraryViewModel
import com.example.purrytify.ui.profile.ProfileViewModel
import com.example.purrytify.ui.trackview.TrackViewDialogFragment
import com.example.purrytify.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val musicPlayerManager = MusicPlayerManager.getInstance()
    private val homeViewModel: HomeViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val musicDBViewModel: MusicDbViewModel by viewModels()
    val userLibrary = MutableStateFlow<List<SongEntity>>(emptyList())
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = TimeUnit.MINUTES.toMillis(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)

        if (!isLoggedIn) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        handler.post(periodicChecker)
        loadInitialData()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setupLibraryObservation()
        setContentView(binding.root)
        setupNavigationAndListeners()
        setupMusicPlayer()
    }

    private val periodicChecker = object : Runnable {
        override fun run() {
            verifyToken()
            handler.postDelayed(this, checkInterval)
        }
    }

    private fun verifyToken() {
        val prefs: SharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val token: String? = prefs.getString("access_token", "")

        lifecycleScope.launch {
            val verifyToken: VerifyToken? =
                try {
                    ApiClient.api.verifyToken("Bearer $token")
                } catch (e: Exception) {
                    Log.e("TokenRefresh", "Error verifying token: ${e.message}")
                    null
                }


            if (verifyToken === null) {
                profileViewModel.logout()
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                // Clear back stack so user can't go back to the app without logging in again
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                this@MainActivity.finish()
                handler.removeCallbacks(periodicChecker)
                Toast.makeText(applicationContext, "Session expired", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "${verifyToken.valid}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadInitialData() {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

            musicPlayerManager.loadQueue()
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
            }
        }

        lifecycleScope.launch {
            musicPlayerManager.currentSongInfo.collect { songInfo ->
                updateMiniPlayerUI()
            }
        }
    }

    fun getMusicDB(): MusicDbViewModel{
        return musicDBViewModel
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

                Toast.makeText(
                    this@MainActivity,
                    "This button is kinda useless now",
                    Toast.LENGTH_SHORT
                ).show()

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
                    if(musicPlayerManager.currentSongInfo.value !== null){
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

//    private fun addSongToLibrary(song: SongEntity) {
//        // Check if song is already in library to prevent duplicates
//        lifecycleScope.launch {
//            val isAlreadyInLibrary = libraryViewModel.userLibrary.value.any { it.title == song.title && it.artist == song.artist }
//
//            if (!isAlreadyInLibrary) {
//                // Insert the song into the library
////                libraryViewModel.addSongToUserLibrary(song)
//
//                // Show a toast to notify the user
//            } else {
//                // Optionally show a different toast if song is already in library
//                Toast.makeText(
//                    this@MainActivity,
//                    "${song.title} is already in your Library",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//    }

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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(periodicChecker)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "purrytify" && data.host == "song") {
            val songId = data.lastPathSegment?.toIntOrNull()
            if (songId != null) {
                // Navigate to the player or load the song using songId
                openTrackViewDialog(songId)
            } else {
                Toast.makeText(this@MainActivity, "Invalid song ID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openTrackViewDialog(songId: Int) {
        val fragment = TrackViewDialogFragment()

        // Optional: set songId as argument to the fragment if needed
        val bundle = Bundle().apply { putInt("song_id", songId) }
        fragment.arguments = bundle

        fragment.show(supportFragmentManager, "trackView")
    }

}
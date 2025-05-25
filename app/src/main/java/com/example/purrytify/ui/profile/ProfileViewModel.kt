package com.example.purrytify.ui.profile

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.purrytify.MainActivity
import com.example.purrytify.api.ApiClient
import com.example.purrytify.data.local.db.entities.ArtistEntity
import com.example.purrytify.data.local.db.entities.CapsuleEntity
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.models.FormattedSongStreak
import com.example.purrytify.models.Profile
import com.example.purrytify.models.SongStreak
import com.example.purrytify.utils.DateTimeUtils
import com.example.purrytify.viewmodel.CapsuleStatsView
import com.example.purrytify.views.MusicDbViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import java.lang.Exception
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    // LiveData for profile picture
    private val _profilePicture = MutableLiveData<Bitmap?>()
    val profilePicture: LiveData<Bitmap?> = _profilePicture

    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData for error handling
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    val musicDbViewModel = MusicDbViewModel(application)

    private val _username = MutableLiveData<String>("")
    val username: LiveData<String?> = _username

    private val _country = MutableLiveData<String>("")
    val country: LiveData<String?> = _country

    private val _userCapsule = MutableStateFlow<List<CapsuleStatsView>>(emptyList())
    val userCapsule: StateFlow<List<CapsuleStatsView>> = _userCapsule.asStateFlow()

    private val currentMonthCapsuleView: MutableLiveData<CapsuleStatsView> = MutableLiveData()

    val currentMonthCapsule: MutableLiveData<CapsuleEntity> = MutableLiveData()

    val currentMonthTopSongs: MutableList<SongEntity> = mutableListOf()

    val countriesMap = mapOf(
        "ID" to "Indonesia",
        "MY" to "Malaysia",
        "US" to "USA",
        "GB" to "UK",
        "CH" to "Switzerland",
        "DE" to "Germany",
        "BR" to "Brazil"
    )

    val reversedCountriesMap = mapOf(
        "Indonesia" to "ID",
        "Malaysia" to "MY",
        "USA" to "US",
        "UK" to "GB",
        "Switzerland" to "CH",
        "Germany" to "DE",
        "Brazil" to "BR"
    )

    // Function to load profile picture
    fun loadProfilePicture(pictureId: String, token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Make API call
                val response = ApiClient.api.getProfilePict(pictureId, "Bearer $token")

                // Convert response to bitmap
                val bitmap = BitmapFactory.decodeStream(response.byteStream())

                // Update LiveData with the bitmap
                _profilePicture.value = bitmap

            } catch (e: Exception) {
                _error.value = "Failed to load profile picture: ${e.message}"
                _profilePicture.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun isLastDayOfMonth(): Boolean {
        val today = LocalDate.now()
        val lastDayOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
        return today == lastDayOfMonth
    }

    // Function to load profile data from SharedPreferences and API
    fun loadProfileData(prefs: SharedPreferences) {
        // Get token and profile picture ID from SharedPreferences
        // Fetch user profile
        val token = prefs.getString("access_token", "") ?: ""

        viewModelScope.launch {
            val profile: Profile = ApiClient.api.getProfile("Bearer ${prefs.getString("access_token", "")}")

            if (token.isNotEmpty() && profile.profilePhoto.isNotEmpty()) {
                loadProfilePicture(profile.profilePhoto, token)
                _country.value = countriesMap[profile.location]
                _username.value = profile.username
            } else {
                _error.value = "Missing profile data. Please log in again."
            }

            launch {
                // Gather current month Capsule data
                // Reset everytime
                _userCapsule.value = mutableListOf<CapsuleStatsView>()

                val prefs: SharedPreferences = application.applicationContext.getSharedPreferences("app_prefs", MODE_PRIVATE)
                currentMonthCapsule.value = CapsuleEntity(
                    userEmail = prefs.getString("email", "")!!,
                    capsuleDate = DateTimeUtils.getCurrentTimeIso(),
                    minuteListened = TimeUnit.MILLISECONDS.toMinutes(musicDbViewModel.getUserActivity().timeListened.toLong()).toInt(),
                    topArtists = "",
                    topSongs = "",
                    songStreakInterval = "",
                    songStreakId = null
                )

                val topSongs = musicDbViewModel.getTopSongs()
                val songStreak: SongStreak? = musicDbViewModel.getStreakSong()

                currentMonthCapsule.value = currentMonthCapsule.value.copy(
                    topArtists = topSongs.joinToString(",") { it.artist },
                    topSongs = topSongs.joinToString(",") { it.id.toString() },
                    songStreakId = songStreak?.songId,
                    songStreakInterval = songStreak?.dateInterval.orEmpty()
                )

                if (isLastDayOfMonth()) {
                    currentMonthCapsule.value?.let { capsule ->
                        launch {
                            musicDbViewModel.registerCapsule(capsule)
                            // Optionally reset for next month
                            // resetCapsuleForNewMonth()
                        }
                    }
                }


                _userCapsule.value = _userCapsule.value + currentMonthCapsuleView.value

                // Get capsules from DB
                musicDbViewModel.getUserCapsules()?.forEach { capsule ->
                    val capsuleDate: String = OffsetDateTime.parse(capsule.capsuleDate).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                    val currentDate: String = OffsetDateTime.parse(DateTimeUtils.getCurrentTimeIso()).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                    if (capsuleDate !== currentDate) {
                        val topSongsList: MutableList<SongEntity> = mutableListOf()
                        val topArtistsList: MutableList<ArtistEntity> = mutableListOf()

                        capsule.topSongs.split(",").forEach { songId ->
                            topSongsList.add(musicDbViewModel.getSongById(songId.toInt())!!)
                        }

                        capsule.topArtists.split(",").forEach { artist ->
                            topArtistsList.add(ArtistEntity(
                                artistId = 0,
                                artistName = artist,
                                artistPicture = musicDbViewModel.getArtistPicture(artist)
                            ))
                        }

                        val capsule: CapsuleStatsView = CapsuleStatsView(application.applicationContext, capsule = capsule, topSongsList = topSongsList, topArtistsList = topArtistsList).apply {
                            setMonthYear()
                            setMinutes()
                            setTopArtist()
                            setTopSong()
                            val streakSong: SongEntity? = musicDbViewModel.getSongById(capsule.songStreakId)
                            setStreakInfo(streakSong)
                            setStreakImage(streakSong)
                        }
                        _userCapsule.value = _userCapsule.value + capsule
                    }
                }
            }
        }
    }

    fun logout(context: MainActivity) {
        val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_logged_in", false)
            remove("access_token")
            remove("refresh_token")
            remove("profile_pict")
            remove("user_id")
            remove("username")
            remove("email")
            remove("profile_pict")
            remove("createdAt")
            remove("country_code")
            apply()
        }

        // releases song
        context.musicPlayerManager.release()

    }

    suspend fun getStats(): List<Int>{
        val statsList = mutableListOf<Int>()

        statsList.add(
            musicDbViewModel.getSongStatusCount("library") +
            musicDbViewModel.getSongStatusCount("like") +
            musicDbViewModel.getSongStatusCount("listened")
        )

        statsList.add(
            musicDbViewModel.getSongStatusCount("like")
        )

        statsList.add(
            musicDbViewModel.getSongStatusCount("like") + musicDbViewModel.getSongStatusCount("listened")
        )

        return statsList
    }

}
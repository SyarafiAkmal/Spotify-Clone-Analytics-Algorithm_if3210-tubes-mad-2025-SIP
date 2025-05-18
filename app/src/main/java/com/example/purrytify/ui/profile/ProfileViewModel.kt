package com.example.purrytify.ui.profile

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.purrytify.api.ApiClient
import com.example.purrytify.views.MusicDbViewModel
import kotlinx.coroutines.launch
import java.lang.Exception

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

    private val musicDbViewModel = MusicDbViewModel(application)

    private val _username = MutableLiveData<String>("")
    val username: LiveData<String?> = _username

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

    // Function to load profile data from SharedPreferences and API
    fun loadProfileData(prefs: SharedPreferences) {
        // Get token and profile picture ID from SharedPreferences
        // Fetch user profile
        val token = prefs.getString("access_token", "") ?: ""
        val pictureId = prefs.getString("profile_pict", "") ?: ""

        if (token.isNotEmpty() && pictureId.isNotEmpty()) {
            loadProfilePicture(pictureId, token)
        } else {
            _error.value = "Missing profile data. Please log in again."
        }
    }

    fun logout() {
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
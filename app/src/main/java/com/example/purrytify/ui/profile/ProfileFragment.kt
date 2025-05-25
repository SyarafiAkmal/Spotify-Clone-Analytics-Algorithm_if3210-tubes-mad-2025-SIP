package com.example.purrytify.ui.profile

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.purrytify.LoginActivity
import com.example.purrytify.R
import com.example.purrytify.databinding.FragmentProfileBinding
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.LinearLayout
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.application
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.MainActivity
import com.example.purrytify.api.ApiClient
import com.example.purrytify.data.local.db.entities.ArtistEntity
import com.example.purrytify.data.local.db.entities.SongActivity
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.ui.global.TopGlobalSongsFragment
import com.example.purrytify.viewmodel.CapsuleStatsView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var profileViewModel: ProfileViewModel
    private var noConnectionView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize ViewModel
        profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        // Inflate layout
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setFragmentResultListener("profile_updated") { _, _ ->
            // Reload profile data when update happens
            loadProfileData()
        }

        // Check network connectivity first
        checkNetworkAndLoadProfile()

        // Set up observers
        setupObservers()

        // Set up button click listeners
        setupClickListeners()
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun checkNetworkAndLoadProfile() {
        try {
            val isConnected = isNetworkAvailable(requireContext())
            if (isConnected) {
                // Show main content, hide no connection layout
                binding.mainContentLayout.visibility = View.VISIBLE
                binding.noConnectionLayout.visibility = View.GONE

                // Load profile data
                loadProfileData()
            } else {
                // Hide main content, show no connection layout
                binding.mainContentLayout.visibility = View.GONE
                binding.noConnectionLayout.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error checking network: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        // Observe profile picture
        profileViewModel.profilePicture.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                binding.profileImage.setImageBitmap(bitmap)
            } else {
                // Set a placeholder image if bitmap is null
                binding.profileImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        // Observe loading state
        profileViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Show/hide loading indicator if you have one
            binding.progressBar?.isVisible = isLoading
        }

        // Observe errors
        profileViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadProfileData() {
        // Get SharedPreferences
        val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        viewLifecycleOwner.lifecycleScope.launch {
            val profile = ApiClient.api.getProfile("Bearer ${prefs.getString("access_token", "")}")

            // Refresh Session Profile data
            prefs.edit().apply {
                remove("profile_pict")
                remove("country_code")
                apply()
            }

//            Toast.makeText(requireContext(), "${profile.location}", Toast.LENGTH_SHORT).show()
            prefs.edit {
                putString("profile_pict", profile.profilePhoto)
                putString("country_code", profile.location)
            }
        }

        profileViewModel.loadProfileData(prefs)
        profileViewModel.username.observe(viewLifecycleOwner) { username ->
            binding.profileName.text = username
        }
        profileViewModel.country.observe(viewLifecycleOwner) { country ->
            binding.profileFrom.text = country
        }
        loadStats()

        // Clear existing views
        val soundCapsulePlaceholder = binding.soundCapsule

        // Create and add April capsule

        profileViewModel.currentMonthCapsule.observe(viewLifecycleOwner) { capsuleEntity ->
            if (capsuleEntity != null) {
                lifecycleScope.launch {
                    // Use capsuleEntity instead of .value to avoid null issues
                    val streakSong: SongEntity? = if (capsuleEntity.songStreakId != null) {
                        profileViewModel.musicDbViewModel.getSongById(capsuleEntity.songStreakId)
                    } else {
                        null
                    }

                    val topSongs = profileViewModel.musicDbViewModel.getTopSongs()

                    val topArtists: List<ArtistEntity> = topSongs.map { song ->
                        ArtistEntity(
                            artistId = 0,
                            artistName = song.artist, // Use song.artist instead of splitting title
                            artistPicture = song.artworkURI
                        )
                    }

                    // Use capsuleEntity parameter, not .value
                    val currentMonthCapsule = CapsuleStatsView(
                        requireContext(),
                        capsule = capsuleEntity, // Use the parameter
                        topSongsList = topSongs,
                        topArtistsList = topArtists
                    ).apply {
                        setMonthYear()
                        setMinutes()
                        setTopArtist()
                        setTopSong()
                        setStreakInfo(streakSong)
                        setStreakImage(streakSong)
                    }

                    // Add to your layout
                    soundCapsulePlaceholder.removeAllViews()
                    soundCapsulePlaceholder.addView(currentMonthCapsule)
                }
            }
        }


        // Add views to container
//        soundCapsulePlaceholder.addView(aprilCapsule)
//        soundCapsulePlaceholder.addView(marchCapsule)
    }

    private fun loadStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            val songStat = binding.tvSongsCount
            val likedStat = binding.tvLikedCount
            val listenedStat = binding.tvListenedCount
            val statsList: List<Int> = profileViewModel.getStats()

            songStat.text = statsList.get(0).toString()
            likedStat.text = statsList.get(1).toString()
            listenedStat.text = statsList.get(2).toString()
        }
    }

    private fun setupClickListeners() {
        // Logout button
        binding.btnLogout.setOnClickListener {
            // Handle logout
            profileViewModel.logout(requireActivity() as MainActivity)
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

            // Navigate back to LoginActivity
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        binding.btnEditProfile?.setOnClickListener {
            EditProfileFragment().show(parentFragmentManager, "edit_profile_dialog")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        noConnectionView = null
    }
}
package com.example.purrytify.ui.trackview

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.R
import com.example.purrytify.views.ShareSheet
import com.example.purrytify.api.ApiClient
import com.example.purrytify.api.PurrytifyAPI
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.databinding.FragmentTrackViewBinding
import com.example.purrytify.ui.dialog.DialogViewModel
import com.example.purrytify.ui.home.HomeViewModel
import com.example.purrytify.ui.library.LibraryViewModel
import com.example.purrytify.utils.ImageUtils
import com.example.purrytify.utils.MusicPlayerManager
import com.example.purrytify.views.MusicDbViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import java.io.IOException

class TrackViewDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentTrackViewBinding
    private var musicPlayerManager = MusicPlayerManager.getInstance()
    private var userIsSeeking = false
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var libraryViewModel: LibraryViewModel
    private lateinit var musicDBViewModel: MusicDbViewModel
    private lateinit var dialogViewModel: DialogViewModel
    private lateinit var purrytifyAPI: PurrytifyAPI
    private val allSongs = MutableStateFlow<List<SongEntity>>(emptyList())

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        // Set up the dialog to expand to full screen when opened
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = resources.displayMetrics.heightPixels
//                it.setBackgroundResource(R.drawable.no_corner_bottom_sheet_background)
                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrackViewBinding.inflate(inflater, container, false)
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        libraryViewModel = ViewModelProvider(requireActivity())[LibraryViewModel::class.java]
        musicDBViewModel = ViewModelProvider(requireActivity())[MusicDbViewModel::class.java]
        dialogViewModel = ViewModelProvider(this)[DialogViewModel::class.java]
        purrytifyAPI = ApiClient.api
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val targetSongId = arguments?.getInt("song_id")
        Log.d("TrackViewDialog", "Received songId: $targetSongId")

        if (targetSongId != null) {
            fetchAndPlaySong(targetSongId)
        }

        setupUI()
        loadSongs()
        setupButtons()
        observeMusicState()
        setupSeekBar()
        setupPlaybackControls()
    }

    private fun fetchAndPlaySong(songId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val onlineSong = purrytifyAPI.getSongById(songId)
                val songEntity = musicPlayerManager.onlineToEntity(onlineSong)

                Log.d("TrackViewDialog", "Fetched song: ${songEntity.title}")
                MusicPlayerManager.getInstance().loadSong(requireContext(), songEntity)

            } catch (e: retrofit2.HttpException) {
                Log.e("TrackViewDialog", "HTTP error fetching song", e)
                Toast.makeText(requireContext(), "Song not found on server", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Log.e("TrackViewDialog", "Network error fetching song", e)
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("TrackViewDialog", "Unknown error", e)
                Toast.makeText(requireContext(), "Failed to load song", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSongs() {
        viewLifecycleOwner.lifecycleScope.launch {
            musicDBViewModel.allSongs.collect { songs ->
                if (songs.isNotEmpty()) {
                    allSongs.value = songs
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            dismiss()
        }

        binding.btnDownload.setOnClickListener {
//            Toast.makeText(requireContext(), "${musicPlayerManager.currentSongInfo.value?.title}", Toast.LENGTH_SHORT).show()
            val currentSong: SongEntity = musicPlayerManager.currentSongInfo.value!!
            viewLifecycleOwner.lifecycleScope.launch {
                if (musicDBViewModel.isSongExistForUser(currentSong)) {
                    Toast.makeText(requireContext(), "Song is already downloaded", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Downloading..", Toast.LENGTH_SHORT).show()
                    val audioUrl = currentSong.uri
                    val artworkUrl = currentSong.artworkURI

                    dialogViewModel.downloadSong(
                        requireContext(),
                        currentSong,
                        audioUrl,
                        artworkUrl,
                        musicDBViewModel
                    )
                }
            }
        }

        binding.btnShare.setOnClickListener {
            val currentSong = musicPlayerManager.currentSongInfo.value
            if (currentSong != null) {
                val deepLink = "purrytify://song/${currentSong.id}"
                val sheet = ShareSheet(deepLink)
                sheet.show(parentFragmentManager, "Sharesheet")
            } else {
                Toast.makeText(requireContext(), "No song is playing", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupUI() {
        val currentSong = musicPlayerManager.currentSongInfo.value
        currentSong?.let { song ->
            val albumResId = ImageUtils.loadImage(
                requireContext(),
                song.artworkURI,
                binding.albumCover
            )

            lifecycleScope.launch {
                val currentSongLiked = musicDBViewModel.getStatusSongById(currentSong?.id!!, "like")
//                Toast.makeText(requireContext(), "${currentSongLiked?.id} song is from like", Toast.LENGTH_SHORT).show()
                // Now use currentSongLiked here
                if (currentSongLiked !== null) {
                    // The song is like
                    binding.btnFavorite.setImageResource(R.drawable.favorited_1)
                } else {
                    // The song is not liked
                    binding.btnFavorite.setImageResource(R.drawable.favorited_0)
                }
            }
            binding.albumCover.setImageResource(albumResId)
            binding.trackTitle.text = song.title
            binding.trackArtist.text = song.artist
        }
    }

    private fun observeMusicState() {
        lifecycleScope.launch {
            musicPlayerManager.currentSongInfo.collect { song ->
                if (song != null) {
                    setupUI()
                }
            }
        }
        lifecycleScope.launch {
            musicPlayerManager.isPlaying.collect { isPlaying ->
                updatePlayPauseButton(isPlaying)
            }
        }

        lifecycleScope.launch {
            musicPlayerManager.currentPosition.collect { position ->
                if (!userIsSeeking) {
                    updateSeekBarProgress(position)
                }
            }
        }

        lifecycleScope.launch {
            musicPlayerManager.duration.collect { duration ->
                binding.seekBar.max = duration
                updateTimeLabels(musicPlayerManager.currentPosition.value, duration)
            }
        }
    }

    private fun setupSeekBar() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateTimeLabels(progress, musicPlayerManager.duration.value)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                userIsSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    musicPlayerManager.seekTo(it.progress)
                    userIsSeeking = false
                }
            }
        })
    }

    private fun setupPlaybackControls() {
        binding.btnPlayPause.setOnClickListener {
            if (musicPlayerManager.isPlaying.value) {
                musicPlayerManager.pause()
            } else {
                musicPlayerManager.play(requireContext())
            }
        }

        binding.btnPrevious.setOnClickListener {
//            Toast.makeText(requireContext(), "${allSongs.value.size}", Toast.LENGTH_SHORT).show()
            val songId = musicPlayerManager.currentSongId.value - 1
            val newSongId = if (songId - 1 < 0) 0 else songId - 1
            if (allSongs.value.isNotEmpty() && newSongId < allSongs.value.size) {
                musicPlayerManager.loadSong(requireContext(), allSongs.value[newSongId])
            }
        }

        binding.btnNext.setOnClickListener {
            val songId = musicPlayerManager.currentSongId.value - 1
            if (allSongs.value.isNotEmpty()) {
                val newSongId = (songId + 1) % allSongs.value.size
                musicPlayerManager.loadSong(requireContext(), allSongs.value[newSongId])
            }
        }


        binding.btnFavorite.setOnClickListener {
//            Toast.makeText(requireContext(), "Favorite toggled", Toast.LENGTH_SHORT).show()
            updateAndToggleFavoriteState()
        }
    }

    private fun updateAndToggleFavoriteState() {
        val currentSongId = musicPlayerManager.currentSongInfo.value?.id
        lifecycleScope.launch {
            val currentSongLiked = musicDBViewModel.getStatusSongById(currentSongId!!, "like")
//            Toast.makeText(requireContext(), "${currentSongLiked === null}", Toast.LENGTH_SHORT).show()
            // Now use currentSongLiked here
            if (currentSongLiked === null) {
                // The song is liked
                binding.btnFavorite.setImageResource(R.drawable.favorited_1)
//                Toast.makeText(requireContext(), "${currentSongId} liked", Toast.LENGTH_SHORT).show()
                musicDBViewModel.updateStatus(musicPlayerManager.currentSongInfo.value!!, "like")
                libraryViewModel.addToLiked(musicPlayerManager.currentSongInfo.value!!)
            } else {
                // The song is not liked
                binding.btnFavorite.setImageResource(R.drawable.favorited_0)
//                Toast.makeText(requireContext(), "${currentSongId} listened", Toast.LENGTH_SHORT).show()
                musicDBViewModel.updateStatus(musicPlayerManager.currentSongInfo.value!!, "listened")
                libraryViewModel.deleteFromLiked(musicPlayerManager.currentSongInfo.value!!)
            }
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.pause_arrow else R.drawable.play_arrow
        )
    }

    private fun updateSeekBarProgress(position: Int) {
        binding.seekBar.progress = position
        updateTimeLabels(position, musicPlayerManager.duration.value)
    }

    private fun updateTimeLabels(position: Int, duration: Int) {
        binding.currentTime.text = formatTime(position)
        binding.totalTime.text = formatTime(duration)
    }

    private fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }
}


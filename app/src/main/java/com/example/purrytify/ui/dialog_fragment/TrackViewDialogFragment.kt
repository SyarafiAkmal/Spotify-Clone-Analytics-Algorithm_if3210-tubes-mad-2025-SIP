package com.example.purrytify.ui.trackview

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.R
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.databinding.FragmentTrackViewBinding
import com.example.purrytify.ui.home.HomeViewModel
import com.example.purrytify.utils.ImageUtils
import com.example.purrytify.utils.MusicPlayerManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TrackViewDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentTrackViewBinding
    private val musicPlayerManager = MusicPlayerManager.getInstance()
    private var userIsSeeking = false
    private lateinit var homeViewModel: HomeViewModel
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load songs from HomeViewModel
        loadSongs()

        setupUI()
        setupBackButton()
        observeMusicState()
        setupSeekBar()
        setupPlaybackControls()
    }

    private fun loadSongs() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.userAllSongs.collect { songs ->
                if (songs.isNotEmpty()) {
                    allSongs.value = songs
                }
            }
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            dismiss()
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
                musicPlayerManager.play()
            }
        }

        binding.btnPrevious.setOnClickListener {
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
            Toast.makeText(requireContext(), "Favorite toggled", Toast.LENGTH_SHORT).show()
        }

        binding.btnMore.setOnClickListener {
            Toast.makeText(requireContext(), "More options", Toast.LENGTH_SHORT).show()
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


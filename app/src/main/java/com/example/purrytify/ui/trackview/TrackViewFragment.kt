package com.example.purrytify.ui.trackview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.R
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.databinding.FragmentTrackViewBinding
import com.example.purrytify.ui.home.HomeViewModel
import com.example.purrytify.utils.ImageUtils
import com.example.purrytify.utils.MusicPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TrackViewFragment : Fragment() {
    private lateinit var binding: FragmentTrackViewBinding
    private val musicPlayerManager = MusicPlayerManager.getInstance()
    private var userIsSeeking = false
    private lateinit var homeViewModel: HomeViewModel
    private val allSongs = MutableStateFlow<List<SongEntity>>(emptyList())

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
        // Use HomeViewModel to get songs instead of MainActivity
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.userAllSongs.collect { songs ->
                if (songs.isNotEmpty()) {
                    allSongs.value = songs
//                    songs.forEach { song ->
//                        Toast.makeText(requireActivity(), "id:${song.id}", Toast.LENGTH_SHORT).show()
//                    }
                }
            }
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            // Navigate back to previous fragment
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupUI() {
        val currentSong = musicPlayerManager.currentSongInfo.value
        currentSong?.let { song ->
            // Use ImageUtils instead of directly setting URI
            Toast.makeText(requireActivity(), "${song.title}", Toast.LENGTH_SHORT).show()
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
            // Implement previous song logic
            var songId = musicPlayerManager.currentSongId.value - 1
            var newSongId = if (songId - 1 < 0) 0 else songId -1
            Toast.makeText(requireContext(), "${newSongId}", Toast.LENGTH_SHORT).show()
            musicPlayerManager.loadSong(requireContext(), allSongs.value.get(newSongId))
        }

        binding.btnNext.setOnClickListener {
            // Implement next song logic
            var songId = musicPlayerManager.currentSongId.value - 1
            var newSongId = (songId + 1) % allSongs.value.size
            Toast.makeText(requireContext(), "${newSongId}", Toast.LENGTH_SHORT).show()
            musicPlayerManager.loadSong(requireContext(), allSongs.value.get(newSongId))
        }

        binding.btnFavorite.setOnClickListener {
            // Implement favorite toggle logic
            Toast.makeText(requireContext(), "Favorite toggled", Toast.LENGTH_SHORT).show()
        }

        binding.btnMore.setOnClickListener {
            // Implement more options logic
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

    override fun onDestroy() {
        super.onDestroy()
    }
}
package com.example.purrytify.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.MainActivity
import com.example.purrytify.R
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.databinding.FragmentHomeBinding
import com.example.purrytify.utils.ImageUtils
import com.example.purrytify.viewmodel.AlbumItemView
import com.example.purrytify.viewmodel.LibraryItemView
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]

        homeViewModel.setPackageName(requireActivity().packageName)
        homeViewModel.initData()

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvRecentlyPlayed.visibility = View.VISIBLE
        binding.llRecentlyPlayed.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.userAllSongs.collect { songs ->
                if (songs.isNotEmpty()) {
                    injectNewSongs(songs)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.userRecentPlayed.collect { songs ->
                if (songs.isNotEmpty()) {
                    injectRecentSongs(songs)
                }
            }
        }
    }

    private fun injectNewSongs(songs: List<SongEntity>) {
        val newSongsPlaceholder: LinearLayout = binding.newSongsPlaceholder
        newSongsPlaceholder.removeAllViews()

        songs.forEach { song ->
            val albumView = AlbumItemView(requireContext()).apply {
                // Get access to the ImageView for direct bitmap setting if needed
                val albumImageView = this.findViewById<android.widget.ImageView>(R.id.album_image)

                // Use our improved image loading utility
                val resId = ImageUtils.loadImage(
                    requireContext(),
                    song.artworkURI,
                    albumImageView
                )

                // Set the resource ID (will be used if it's a drawable resource)
                // For file paths, the ImageView is already updated by the utility
                setAlbumImage(resId)
                setTitle(song.title)
                setArtist(song.artist)
                setSong(song)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.marginEnd = resources.getDimensionPixelSize(R.dimen.album_item_margin)
                layoutParams = params
            }

            albumView.setOnClickListener {
                albumView.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction {
                        albumView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()

                Toast.makeText(requireActivity(), "id: ${albumView.getSong()?.id}", Toast.LENGTH_SHORT).show()
                (requireActivity() as MainActivity).musicPlayerManager.loadSong(requireContext(), albumView.getSong()!!)
                (requireActivity() as MainActivity).musicPlayerManager.play()
            }

            newSongsPlaceholder.addView(albumView)
        }
    }

    private fun injectRecentSongs(songs: List<SongEntity>) {
        val recentlyPlayedPlaceholder: LinearLayout = binding.llRecentlyPlayed
        recentlyPlayedPlaceholder.removeAllViews()

        songs.forEach { song ->
            val libraryView = LibraryItemView(requireContext()).apply {
                // Get access to the ImageView
                val albumImageView = this.findViewById<android.widget.ImageView>(R.id.libraryItemCover)

                // Use our improved image loading utility
                val resId = ImageUtils.loadImage(
                    requireContext(),
                    song.artworkURI,
                    albumImageView
                )

                // Set the resource ID
                setAlbumImage(resId)
                setTitle(song.title)
                setArtist(song.artist)
                setSong(song)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.marginEnd = resources.getDimensionPixelSize(R.dimen.album_item_margin)
                layoutParams = params
            }

            libraryView.setOnClickListener {
                libraryView.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction {
                        libraryView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()

                Toast.makeText(requireContext(), "id:${libraryView.getSong()?.id}", Toast.LENGTH_SHORT).show()
                (requireActivity() as MainActivity).musicPlayerManager.loadSong(requireContext(), libraryView.getSong()!!)
                (requireActivity() as MainActivity).musicPlayerManager.play()
            }

            recentlyPlayedPlaceholder.addView(libraryView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
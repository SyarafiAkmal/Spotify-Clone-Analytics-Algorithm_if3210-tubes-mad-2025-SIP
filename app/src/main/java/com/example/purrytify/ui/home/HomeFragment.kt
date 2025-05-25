package com.example.purrytify.ui.home

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.MainActivity
import com.example.purrytify.R
import com.example.purrytify.api.ApiClient
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.databinding.FragmentHomeBinding
import com.example.purrytify.ui.global.TopGlobalSongsFragment
import com.example.purrytify.ui.global.TopLocalSongsFragment
import com.example.purrytify.utils.ImageUtils
import com.example.purrytify.views.AlbumItemView
import com.example.purrytify.views.LibraryItemView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                } else {
                    showEmptyState("You have no songs yet")
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

        setupCharts()
    }

    private fun setupCharts() {
        val topLocalSongs: LinearLayout = binding.topLocalSongs
        val topGlobalSongs: LinearLayout = binding.topGlobalSongs
        val prefs: SharedPreferences = requireActivity().getSharedPreferences("app_prefs", MODE_PRIVATE)
        binding.tvRecSongs.visibility = View.GONE
        binding.hsvRecSongs.visibility = View.GONE
        binding.recSongsPlaceholder.visibility = View.GONE

        lifecycleScope.launch {
            homeViewModel.userLikedSongs.collect { songs ->
                if (songs.size !== 0) {
                    binding.tvRecSongs.visibility = View.VISIBLE
                    binding.hsvRecSongs.visibility = View.VISIBLE
                    binding.recSongsPlaceholder.visibility = View.VISIBLE

                    val serverSongs: MutableList<SongEntity> = mutableListOf()

                    val onlineLocalSongs = withContext(Dispatchers.IO) {
                        ApiClient.api.getLocalSongs(prefs.getString("country_code", "")!!)
                    }

                    val onlineGlobalSongs = withContext(Dispatchers.IO) {
                        ApiClient.api.getGlobalSongs()
                    }

                    serverSongs.addAll(onlineGlobalSongs.map { onlineSong ->
                        SongEntity(
                            id = onlineSong.id,
                            title = onlineSong.title,
                            artist = onlineSong.artist,
                            artworkURI = onlineSong.artwork,
                            uri = onlineSong.url,
                            duration = 0
                        )
                    })

                    serverSongs.addAll(onlineLocalSongs.map { onlineSong ->
                        SongEntity(
                            id = onlineSong.id,
                            title = onlineSong.title,
                            artist = onlineSong.artist,
                            artworkURI = onlineSong.artwork,
                            uri = onlineSong.url,
                            duration = 0
                        )
                    })

                    val likedArtist: List<String> = songs
                        .flatMap { it.artist.split(", ").map { name -> name.trim() } }
                        .distinct()

                    val recSongs: MutableList<SongEntity> = mutableListOf()

                    val userSongIds = homeViewModel.userAllSongs.value.map { it.id }.toSet()

                    recSongs.addAll(
                        serverSongs
                            .filter { song ->
                                song.id !in userSongIds &&
                                        likedArtist.any { liked -> song.artist.contains(liked, ignoreCase = true) }
                            }
                            .distinctBy { it.id }.distinctBy { it.title }
                    )

                    recSongs.addAll(
                        homeViewModel.userAllSongs.value
                            .filter { song ->
                                song.id !in userSongIds &&
                                        likedArtist.any { liked -> song.artist.contains(liked, ignoreCase = true) }
                            }
                            .distinctBy { it.id }.distinctBy { it.title }
                    )


                    if (recSongs.isNotEmpty()) {
                        injectRecSongs(recSongs)
                    }
                }
            }
        }

        topLocalSongs.setOnClickListener {
            TopLocalSongsFragment().show(parentFragmentManager, "local_songs_dialog")
//            Toast.makeText(requireContext(), "${prefs.getString("country_code", "")} has ${homeViewModel.onlineLocalSongs.size} songs", Toast.LENGTH_SHORT).show()
        }

        topGlobalSongs.setOnClickListener {
            TopGlobalSongsFragment().show(parentFragmentManager, "global_songs_dialog")
//            Toast.makeText(requireContext(), "There is ${homeViewModel.onlineGlobalSongs.size} top global songs", Toast.LENGTH_SHORT).show()
        }

    }

    private fun injectRecSongs(songs: List<SongEntity>) {
        val recSongsPlaceholder: LinearLayout = binding.recSongsPlaceholder
        recSongsPlaceholder.removeAllViews()

        songs.forEach { song ->
            val albumView = AlbumItemView(requireContext()).apply {
                // Get access to the ImageView for direct bitmap setting if needed
                val albumImageView = this.findViewById<android.widget.ImageView>(R.id.album_image)

                // Set the resource ID (will be used if it's a drawable resource)
                // For file paths, the ImageView is already updated by the utility
//                setAlbumImage(resId)
                setTitle(song.title)
                setArtist(song.artist)
                setSong(song)
                ImageUtils.loadImage(requireContext(), song.artworkURI, albumImageView)

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
//                (requireActivity() as MainActivity).musicPlayerManager.play()
            }

            recSongsPlaceholder.addView(albumView)
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
                albumImageView.setImageResource(resId)
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
//                (requireActivity() as MainActivity).musicPlayerManager.play()
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

                Toast.makeText(requireContext(), "id:${song.id}", Toast.LENGTH_SHORT).show()
                (requireActivity() as MainActivity).musicPlayerManager.loadSong(requireContext(), libraryView.getSong()!!)
//                (requireActivity() as MainActivity).musicPlayerManager.play()
            }

            recentlyPlayedPlaceholder.addView(libraryView)
        }
    }

    private fun showEmptyState(message: String) {
        val emptySongsPlaceholder: LinearLayout = binding.emptySongsPlaceholder
//        val recentSongsPlaceholder: LinearLayout = binding.llRecentlyPlayed
        emptySongsPlaceholder.removeAllViews()

        // Create and add an empty state view
        val emptyStateView = TextView(requireContext()).apply {
            text = message
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = resources.getDimensionPixelSize(R.dimen.margin_large)
            }
        }

        emptySongsPlaceholder.addView(emptyStateView)
//        recentSongsPlaceholder.addView(emptyStateView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
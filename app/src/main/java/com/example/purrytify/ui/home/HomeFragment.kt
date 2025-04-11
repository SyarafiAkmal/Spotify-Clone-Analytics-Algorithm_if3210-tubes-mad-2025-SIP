package com.example.purrytify.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.MainActivity
import com.example.purrytify.R
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.databinding.FragmentHomeBinding
import com.example.purrytify.viewmodel.AlbumItemView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe songs from MainActivity using StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            (requireActivity() as MainActivity).userSongs.collect { songs ->
                if (songs.isNotEmpty()) {
                    injectSongs(songs)
                }
            }
        }
    }

    private fun injectSongs(songs: List<SongEntity>) {
        val newSongsPlaceholder: LinearLayout = binding.newSongsPlaceholder
        newSongsPlaceholder.removeAllViews()

        songs.forEach { song ->
            val albumView = AlbumItemView(requireContext()).apply {
                setAlbumImage(getDrawableResourceFromUri(song.artworkURI))
                setTitle(song.title)
                setArtist(song.artist)
                setSong(song)

                // Optional: Add margin between album views
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

                (requireActivity() as MainActivity).musicPlayerManager.loadSong(requireContext(), albumView.getSong()!!)
                (requireActivity() as MainActivity).musicPlayerManager.play()
            }

            newSongsPlaceholder.addView(albumView)
        }
    }

    private fun getDrawableResourceFromUri(uri: String): Int {
        val resourceName = uri.substringAfterLast("/")
        return resources.getIdentifier(resourceName, "drawable", requireActivity().packageName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
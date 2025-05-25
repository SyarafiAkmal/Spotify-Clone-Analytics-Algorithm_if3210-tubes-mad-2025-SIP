package com.example.purrytify.ui.global

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginLeft
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.MainActivity
import com.example.purrytify.R
import com.example.purrytify.api.ApiClient
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.databinding.FragmentTopLocalSongsBinding
import com.example.purrytify.ui.home.HomeViewModel
import com.example.purrytify.utils.ImageUtils
import com.example.purrytify.views.LibraryItemView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TopLocalSongsFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentTopLocalSongsBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTopLocalSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        loadGlobalSongs()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = true
                behavior.skipCollapsed = false
                // If you want it to be peekable (showing just the top portion first)
                behavior.peekHeight = resources.displayMetrics.heightPixels
                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }

        return dialog
    }

    private fun setupHeader() {
        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val monthYear = monthFormat.format(calendar.time)

        val headerText = binding.monthYearText
        headerText.text = "$monthYear • 1h 30min"

        val backButton = binding.btnBack
        backButton.setOnClickListener {
            dismiss()
        }

    }

    private fun loadGlobalSongs() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = prefs.getString("access_token", "") ?: ""

                if (token.isEmpty()) {
                    Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val onlineSongs = withContext(Dispatchers.IO) {
                    ApiClient.api.getLocalSongs(prefs.getString("country_code", "")!!)
                }

                val songEntities = onlineSongs.map { onlineSong ->
                    SongEntity(
                        id = onlineSong.id,
                        title = onlineSong.title,
                        artist = onlineSong.artist,
                        artworkURI = onlineSong.artwork,
                        uri = onlineSong.url,
                        duration = 0
                    )
                }

                injectGlobalSongs(songEntities)
            } catch (e: Exception) {
                Log.e("TopGlobalSongs", "Error loading songs: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun injectGlobalSongs(songs: List<SongEntity>) {
        val songsContainer: LinearLayout = binding.localOnlineSongs
        songsContainer.removeAllViews()

        songs.forEachIndexed { index, song ->
            // Create a horizontal layout for each song row
            val songRowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER_VERTICAL
                setPadding(50, 20, 50, 30)  // Add some padding around the row
            }

            // Add rank text
            val rankText = TextView(requireContext()).apply {
                text = "${index + 1}"
                textSize = 16f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 30  // Add space between rank and album art
                    gravity = Gravity.CENTER
                    width = 30.dpToPx(requireContext())  // Fixed width for rank
                }
                gravity = Gravity.CENTER  // Center the text within the TextView
            }
            songRowLayout.addView(rankText)

            // Album cover image
            val albumImageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    48.dpToPx(requireContext()),  // Fixed width for album art
                    48.dpToPx(requireContext())   // Fixed height for album art
                ).apply {
                    marginEnd = 16  // Add space between album art and song details
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            // Load the image
            ImageUtils.loadImage(requireContext(), song.artworkURI, albumImageView)
            songRowLayout.addView(albumImageView)

            // Song details container (title and artist)
            val songDetailsLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,  // Width 0 with weight will take remaining space
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setPadding(20, 0, 0, 0)
                    weight = 1f  // Take all available space
                }
            }

            // Song title
            val titleText = TextView(requireContext()).apply {
                text = song.title
                textSize = 16f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            songDetailsLayout.addView(titleText)

            // Artist name
            val artistText = TextView(requireContext()).apply {
                text = song.artist
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.bottom_nav_inactive))
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            songDetailsLayout.addView(artistText)

            // Add song details to the row
            songRowLayout.addView(songDetailsLayout)

            // Set click listener on the entire row
            songRowLayout.setOnClickListener {
                songRowLayout.animate()
                    .alpha(0.7f)
                    .setDuration(100)
                    .withEndAction {
                        songRowLayout.animate()
                            .alpha(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()

                // Play the song
                (requireActivity() as MainActivity).musicPlayerManager.loadSong(requireContext(), song)
//                (requireActivity() as MainActivity).musicPlayerManager.play()
            }

            // Add the complete row to the container
            songsContainer.addView(songRowLayout)
        }
    }

    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
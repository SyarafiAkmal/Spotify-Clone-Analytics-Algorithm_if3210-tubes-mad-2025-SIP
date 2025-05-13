package com.example.purrytify.ui.library

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.MainActivity
import com.example.purrytify.R
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.databinding.FragmentLibraryBinding
import com.example.purrytify.utils.ImageUtils
import com.example.purrytify.views.LibraryItemView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.google.android.material.bottomsheet.BottomSheetBehavior

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null

    private val binding get() = _binding!!
    private var currentTab: String = "All"
    private lateinit var libraryViewModel: LibraryViewModel
    private val PICK_AUDIO_REQUEST = 1
    private val PICK_ARTWORK_REQUEST = 2

    private var selectedAudioUri: Uri? = null
    private var selectedArtworkUri: Uri? = null
    private var isAudioUploaded = false
    private var extractedAlbumArt: Bitmap? = null // Add this field to store extracted album art

    private lateinit var titleEditText: EditText
    private lateinit var artistEditText: EditText
    private lateinit var durationTextView: TextView
    private lateinit var uploadStatusText: TextView
    private lateinit var uploadFileCard: CardView
    private lateinit var artworkFileCard: CardView
    private lateinit var mp3FileNameText: TextView
    private lateinit var uploadPhotoLayout: RelativeLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        libraryViewModel = ViewModelProvider(requireActivity())[LibraryViewModel::class.java]

        libraryViewModel.setPackageName(requireActivity().packageName)
        libraryViewModel.initData()

        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabButtons()
        setupAddButton()
        loadLibraryData()
    }

    private fun setupAddButton() {
        binding.btnAddLibrary.setOnClickListener {
            showAddSongDialog()
        }
    }

    private fun showAddSongDialog() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val uploadView = layoutInflater.inflate(R.layout.create_song, null)

        // Initialize UI Elements
        initializeDialogViews(uploadView)

        // Set up listeners
        setupFileUploadListeners()

        // Setup save and cancel buttons
        setupDialogButtons(bottomSheetDialog, uploadView)

        // Set content view and show dialog
        bottomSheetDialog.setContentView(uploadView)

        bottomSheetDialog.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)

            // Fix the behavior to prevent rounded corners
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        bottomSheetDialog.show()
    }

    private fun initializeDialogViews(view: View) {
        uploadFileCard = view.findViewById(R.id.cv_upload_file)
        artworkFileCard = view.findViewById(R.id.cv_upload_photo)
        titleEditText = view.findViewById(R.id.et_title)
        artistEditText = view.findViewById(R.id.et_artist)
        durationTextView = view.findViewById(R.id.durationText)
        mp3FileNameText = view.findViewById(R.id.mp3fileName)
        uploadPhotoLayout = view.findViewById(R.id.uploadPhoto)

        // Add a status TextView if it doesn't exist in your layout
        uploadStatusText = view.findViewById<TextView>(R.id.uploadStatus) ?: TextView(requireContext()).apply {
            id = R.id.uploadStatus
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
            setTextColor(Color.RED)
            (view as? ViewGroup)?.addView(this)
        }
    }

    private fun setupFileUploadListeners() {
        // Audio File Selection
        uploadFileCard.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*"
            }
            startActivityForResult(intent, PICK_AUDIO_REQUEST)
        }

        // Artwork File Selection
        artworkFileCard.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, PICK_ARTWORK_REQUEST)
        }
    }

    private fun setupDialogButtons(dialog: BottomSheetDialog, view: View) {
        val cancelButton = view.findViewById<AppCompatButton>(R.id.btn_cancel)
        val saveButton = view.findViewById<AppCompatButton>(R.id.btn_save)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            if (validateSongData()) {
                saveSong()
                dialog.dismiss()
            }
        }
    }

    private fun validateSongData(): Boolean {
        // Check if audio is uploaded
        if (!isAudioUploaded || selectedAudioUri == null) {
            showValidationError("Please select an audio file")
            return false
        }

        // Check if title and artist are filled
        val title = titleEditText.text.toString().trim()
        val artist = artistEditText.text.toString().trim()

        if (title.isEmpty()) {
            showValidationError("Title is required")
            titleEditText.requestFocus()
            return false
        }

        if (artist.isEmpty()) {
            showValidationError("Artist is required")
            artistEditText.requestFocus()
            return false
        }

        return true
    }

    private fun showValidationError(message: String) {
        uploadStatusText.apply {
            text = message
            visibility = View.VISIBLE
        }

        // Also show a toast for better visibility
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun saveSong() {
        try {
            val title = titleEditText.text.toString().trim()
            val artist = artistEditText.text.toString().trim()

            // Save audio file to internal storage
            val audioFilePath = saveFileToInternalStorage(selectedAudioUri!!, "audio")
            if (audioFilePath.isEmpty()) {
                Toast.makeText(requireContext(), "Failed to save audio file", Toast.LENGTH_SHORT).show()
                return
            }

            val artworkPath = when {
                // If user selected a custom image
                selectedArtworkUri != null -> {
                    saveFileToInternalStorage(selectedArtworkUri!!, "image")
                }
                // If song has embedded artwork
                extractedAlbumArt != null -> {
                    // Save the embedded album art
                    saveEmbeddedArtwork(extractedAlbumArt!!)
                }
                // No artwork available
                else -> {
                    ""
                }
            }

            // Get metadata for duration
            val metadata = extractMetadata(selectedAudioUri!!)

            // Create song entity and save to database
            val newSong = SongEntity(
                id = 0, // Database will assign auto-increment id
                title = title,
                artist = artist,
                duration = metadata.duration,
                uri = audioFilePath,
                artworkURI = artworkPath,
            )

            // Add to database using ViewModel
            viewLifecycleOwner.lifecycleScope.launch {
                libraryViewModel.insertSong(newSong)

                // Show success message
                Toast.makeText(requireContext(), "Song added to library", Toast.LENGTH_SHORT).show()

                // Refresh data
                libraryViewModel.addToLibrary(newSong)
            }
        } catch (e: Exception) {
            Log.e("LibraryFragment", "Error saving song", e)
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Save embedded artwork from bitmap to file
    private fun saveEmbeddedArtwork(bitmap: Bitmap): String {
        return try {
            // Create a unique filename with timestamp
            val timestamp = System.currentTimeMillis()
            val filename = "${timestamp}.jpg"

            // Create file in internal storage
            val file = File(requireContext().filesDir, filename)

            // Save the bitmap to file
            file.outputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
            }

            // Return our custom reference format
            "custom_artwork:$timestamp"
        } catch (e: Exception) {
            Log.e("LibraryFragment", "Error saving embedded artwork", e)
            "drawable/default_album_art"
        }
    }

    private fun updateUIWithMetadata(metadata: SongMetadata, fileName: String) {
        Log.d("UploadMusic", "title: ${metadata.title}, artist: ${metadata.artist}")

        // Update UI elements with metadata
        titleEditText.setText(metadata.title)
        artistEditText.setText(metadata.artist)
        durationTextView.text = formatDuration(metadata.duration)

        // Update the file name text
        mp3FileNameText.text = fileName

        // If we have album art, update the photo layout
        if (metadata.albumArt != null) {
            extractedAlbumArt = metadata.albumArt
            updateUploadPhotoWithBitmap(metadata.albumArt)
        }

        // Mark audio as uploaded
        isAudioUploaded = true
    }

    private fun updateUploadPhotoWithBitmap(bitmap: Bitmap) {
        try {
            // First, clear all existing views
            uploadPhotoLayout.removeAllViews()

            // Create and add the image view for the album art
            val albumArtImageView = ImageView(requireContext()).apply {
                id = View.generateViewId()
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(bitmap)
            }
            uploadPhotoLayout.addView(albumArtImageView)

            // Add the edit icon at the bottom right corner
            val editIconView = ImageView(requireContext()).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.album_item_margin),
                    resources.getDimensionPixelSize(R.dimen.album_item_margin)
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                    setMargins(8, 8, 8, 8)
                }
                setImageResource(R.drawable.edit)

                // Add click listener to replace the image
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "image/*"
                    }
                    startActivityForResult(intent, PICK_ARTWORK_REQUEST)
                }
            }
            uploadPhotoLayout.addView(editIconView)
        } catch (e: Exception) {
            Log.e("LibraryFragment", "Error updating upload photo with bitmap", e)
            Toast.makeText(requireContext(), "Error displaying album art", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFileToInternalStorage(uri: Uri, fileType: String): String {
        return try {
            // Determine file extension
            val mimeType = requireContext().contentResolver.getType(uri)
            val extension = when {
                fileType == "audio" -> ".mp3"
                fileType == "image" -> ".jpg"
                mimeType?.contains("image/") == true -> ".jpg"
                mimeType?.contains("audio/") == true -> ".mp3"
                else -> ""
            }

            // Create a unique filename with timestamp
            val timestamp = System.currentTimeMillis()
            val filename = "${timestamp}$extension"

            // Open input stream from the URI
            val inputStream = requireContext().contentResolver.openInputStream(uri)
                ?: throw IOException("Cannot open input stream")

            // For regular files, save to internal storage
            val file = File(requireContext().filesDir, filename)

            // Copy the file
            inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // For images, return our custom reference format
            if (fileType == "image") {
                return "custom_artwork:$timestamp"
            }

            // For audio files, return the file:// path
            "file://${file.absolutePath}"
        } catch (e: Exception) {
            Log.e("FileStorage", "Error saving file", e)

            // Show error message
            uploadStatusText.apply {
                text = "Error saving file: ${e.message}"
                setTextColor(Color.RED)
                visibility = View.VISIBLE
            }

            ""
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_AUDIO_REQUEST -> handleAudioResult(data)
                PICK_ARTWORK_REQUEST -> handleArtworkResult(data)
            }
        }
    }

    private fun handleAudioResult(data: Intent?) {
        data?.data?.let { uri ->
            try {
                // Persist permissions
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(uri, flags)

                selectedAudioUri = uri

                // Get the file name from the URI
                val fileName = getFileNameFromUri(uri)

                // Extract and display metadata
                val metadata = extractMetadata(uri)
                updateUIWithMetadata(metadata, fileName)
            } catch (e: Exception) {
                Log.e("LibraryFragment", "Error handling audio file", e)

                // Update UI to show error
                uploadStatusText.apply {
                    text = "Error: ${e.message}"
                    setTextColor(Color.RED)
                    visibility = View.VISIBLE
                }

                isAudioUploaded = false
                selectedAudioUri = null
            }
        } ?: run {
            // Uri is null
            uploadStatusText.apply {
                text = "Error: Unable to access selected file"
                setTextColor(Color.RED)
                visibility = View.VISIBLE
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        // Try to get the file name from the content resolver
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            if (nameIndex >= 0) it.getString(nameIndex) else "Selected File"
        } ?: "Selected File"
    }

    private fun handleArtworkResult(data: Intent?) {
        data?.data?.let { uri ->
            try {
                // Persist permissions
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(uri, flags)

                selectedArtworkUri = uri
                // Clear the extracted album art since user selected custom artwork
                extractedAlbumArt = null

                // Update the upload photo layout with the image
                updateUploadPhotoWithImage(uri)

            } catch (e: Exception) {
                Log.e("LibraryFragment", "Error handling artwork file", e)

                // Update UI to show error
                uploadStatusText.apply {
                    text = "Error with artwork: ${e.message}"
                    setTextColor(Color.RED)
                    visibility = View.VISIBLE
                }

                selectedArtworkUri = null
            }
        }
    }

    private fun updateUploadPhotoWithImage(imageUri: Uri) {
        try {
            // First, clear all existing views
            uploadPhotoLayout.removeAllViews()

            // Create and add the image view for the uploaded image
            val uploadedImageView = ImageView(requireContext()).apply {
                id = View.generateViewId()
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(imageUri)
            }
            uploadPhotoLayout.addView(uploadedImageView)

            // Add the edit icon at the bottom right corner
            val editIconView = ImageView(requireContext()).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.album_item_margin),
                    resources.getDimensionPixelSize(R.dimen.album_item_margin)
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                    setMargins(8, 8, 8, 8)
                }
                setImageResource(R.drawable.edit)
                setColorFilter(Color.WHITE)

                // Add click listener to replace the image
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "image/*"
                    }
                    startActivityForResult(intent, PICK_ARTWORK_REQUEST)
                }
            }
            uploadPhotoLayout.addView(editIconView)

        } catch (e: Exception) {
            Log.e("LibraryFragment", "Error updating upload photo layout", e)
            Toast.makeText(requireContext(), "Error displaying image", Toast.LENGTH_SHORT).show()
        }
    }

    // Enhanced method to extract album art from URI
    private fun extractMetadata(uri: Uri): SongMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(requireContext(), uri)

            // Extract basic metadata
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown Title"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

            // Extract album art
            val albumArtBytes = retriever.embeddedPicture
            val albumArt = if (albumArtBytes != null) {
                try {
                    BitmapFactory.decodeByteArray(albumArtBytes, 0, albumArtBytes.size)
                } catch (e: Exception) {
                    Log.e("MetadataExtraction", "Error decoding album art", e)
                    null
                }
            } else {
                null
            }

            SongMetadata(title, artist, duration, albumArt)
        } catch (e: Exception) {
            Log.e("MetadataExtraction", "Error extracting metadata", e)

            // Mark as not uploaded due to error
            isAudioUploaded = false

            // Show error in UI
            uploadStatusText.apply {
                text = "Error reading file metadata: ${e.message}"
                setTextColor(Color.RED)
                visibility = View.VISIBLE
            }

            SongMetadata("Unknown Title", "Unknown Artist", 0L, null)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e("MetadataExtraction", "Error releasing retriever", e)
            }
        }
    }

    private fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    // Enhanced data class to hold metadata including album art
    data class SongMetadata(
        val title: String,
        val artist: String,
        val duration: Long,
        val albumArt: Bitmap?
    )

    private fun setupTabButtons() {
        // Set initial button states
        updateButtonStates()

        // Button click listeners
        binding.btnAll.setOnClickListener {
            if (currentTab != "All") {
                currentTab = "All"
                updateButtonStates()
                loadLibraryData()
            }
        }

        binding.btnLiked.setOnClickListener {
            if (currentTab != "Liked") {
                currentTab = "Liked"
                updateButtonStates()
                loadLibraryData()
            }
        }

        binding.btnDownloaded.setOnClickListener {
            if (currentTab != "Downloaded") {
                currentTab = "Downloaded"
                updateButtonStates()
                loadLibraryData()
            }
        }
    }

    private fun loadLibraryData() {
        viewLifecycleOwner.lifecycleScope.launch {
            when (currentTab) {
                "All" -> {
                    libraryViewModel.userLibrary.collect { songs ->
                        if (songs.isNotEmpty()) {
                            injectSongs(songs)
                        } else {
                            showEmptyState("No library songs yet")
                        }
                    }
                }
                "Liked" -> {
                    libraryViewModel.userLiked.collect { songs ->
                        if (songs.isNotEmpty()) {
                            injectSongs(songs)
                        } else {
                            showEmptyState("No liked songs yet")
                        }
                    }
                }
                "Downloaded" -> {
                    libraryViewModel.userLiked.collect { songs ->
                        if (songs.isNotEmpty()) {
                            injectSongs(songs)
                        } else {
                            showEmptyState("No downloaded songs yet")
                        }
                    }
                }
            }
        }
    }

    private fun updateButtonStates() {
        // Reset all buttons to unselected state
        binding.btnAll.apply {
            backgroundTintList = ColorStateList.valueOf(
                if (currentTab == "All") getColor(R.color.green_spotify) else getColor(R.color.dark_gray)
            )
            setTextColor(
                if (currentTab == "All") getColor(R.color.black) else getColor(R.color.white)
            )
        }

        binding.btnLiked.apply {
            backgroundTintList = ColorStateList.valueOf(
                if (currentTab == "Liked") getColor(R.color.green_spotify) else getColor(R.color.dark_gray)
            )
            setTextColor(
                if (currentTab == "Liked") getColor(R.color.black) else getColor(R.color.white)
            )
        }

        binding.btnDownloaded.apply {
            backgroundTintList = ColorStateList.valueOf(
                if (currentTab == "Downloaded") getColor(R.color.green_spotify) else getColor(R.color.dark_gray)
            )
            setTextColor(
                if (currentTab == "Downloaded") getColor(R.color.black) else getColor(R.color.white)
            )
        }
    }

    private fun getColor(colorRes: Int): Int {
        return ContextCompat.getColor(requireContext(), colorRes)
    }

    private fun showEmptyState(message: String) {
        val librarySongsPlaceholder: LinearLayout = binding.librarySongsPlaceholder
        librarySongsPlaceholder.removeAllViews()

        // Create and add an empty state view
        val emptyStateView = TextView(requireContext()).apply {
            text = message
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.margin_large)
            }
        }

        librarySongsPlaceholder.addView(emptyStateView)
    }

    private fun injectSongs(songs: List<SongEntity>) {
        val librarySongsPlaceholder: LinearLayout = binding.librarySongsPlaceholder
        librarySongsPlaceholder.removeAllViews()

        songs.forEach { song ->
            val libraryView = LibraryItemView(requireContext()).apply {
                // Get access to the album image view
                val albumImageView = findViewById<ImageView>(R.id.libraryItemCover)

                // Use our improved image loading utility - needs to be updated to handle custom artwork format
                val resId = ImageUtils.loadImage(
                    requireContext(),
                    song.artworkURI,
                    albumImageView
                )

                // Set the resource ID (the actual image may already be set by the utility)
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

                (requireActivity() as MainActivity).musicPlayerManager.loadSong(requireContext(), libraryView.getSong()!!)
                (requireActivity() as MainActivity).musicPlayerManager.play()
            }

            librarySongsPlaceholder.addView(libraryView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
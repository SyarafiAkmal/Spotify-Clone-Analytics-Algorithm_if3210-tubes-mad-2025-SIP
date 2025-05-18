package com.example.purrytify.ui.dialog

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.views.MusicDbViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class DialogViewModel : ViewModel() {

    // LiveData for download progress and status
    private val _downloadProgress = MutableLiveData<Float>()
    val downloadProgress: LiveData<Float> = _downloadProgress

    private val _downloadStatus = MutableLiveData<DownloadStatus>()
    val downloadStatus: LiveData<DownloadStatus> = _downloadStatus

    // Download state class
    sealed class DownloadStatus {
        object Idle : DownloadStatus()
        object Downloading : DownloadStatus()
        data class Success(val song: SongEntity) : DownloadStatus()
        data class Error(val message: String) : DownloadStatus()
    }

    fun downloadSong(
        context: Context,
        song: SongEntity,
        audioUrl: String,
        artworkUrl: String,
        musicDbViewModel: MusicDbViewModel
    ) {
        // Set initial state
        _downloadStatus.value = DownloadStatus.Downloading
        _downloadProgress.value = 0f

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Update UI with initial progress
                withContext(Dispatchers.Main) {
                    _downloadProgress.value = 0.1f
                }

                // Download and save the audio file (50% of progress)
                val audioFilePath = downloadFile(context, audioUrl, "audio") { progress ->
                    withContext(Dispatchers.Main) {
                        _downloadProgress.value = progress * 0.5f  // 50% weight for audio
                    }
                }

                // Download and save the artwork (30% of progress)
                val artworkPath = downloadFile(context, artworkUrl, "image") { progress ->
                    withContext(Dispatchers.Main) {
                        _downloadProgress.value = 0.5f + (progress * 0.3f)  // 30% weight for artwork
                    }
                }

                // Create a new song entity with local paths
                val downloadedSong: SongEntity = SongEntity(
                    title = song.title,
                    artist = song.artist,
                    uri = audioFilePath,
                    artworkURI = artworkPath,
                    duration = 0,
                )

                // Save to database (20% of progress)
                withContext(Dispatchers.Main) {
                    _downloadProgress.value = 0.8f
                }

                // Insert the song into the database
                musicDbViewModel.checkAndInsertSong(downloadedSong)

                // Notify completion on main thread
                withContext(Dispatchers.Main) {
                    _downloadProgress.value = 1.0f
                    _downloadStatus.value = DownloadStatus.Success(downloadedSong)
                }
                Toast.makeText(context, "Download Completed", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e("DialogViewModel", "Error downloading song: ${e.message}", e)

                // Notify error on main thread
                withContext(Dispatchers.Main) {
                    _downloadStatus.value = DownloadStatus.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    private suspend fun downloadFile(
        context: Context,
        url: String,
        fileType: String,
        onProgressUpdate: suspend (Float) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        try {
            // Create a unique filename with timestamp
            val timestamp = System.currentTimeMillis()
            val extension = if (fileType == "audio") ".mp3" else ".jpg"
            val filename = "${timestamp}$extension"

            // Connect to URL
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 30000  // 30 second timeout
            connection.readTimeout = 30000
            connection.connect()

            // Check if the server returned a successful response code
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
            }

            // Get file size for progress tracking
            val fileLength = connection.contentLength.toLong()

            // Get input stream from connection
            val input = BufferedInputStream(connection.inputStream)

            // Create output file in internal storage
            val file = File(context.filesDir, filename)

            // Create output stream to file
            val output = FileOutputStream(file)

            // Create buffer for reading data
            val data = ByteArray(4096)  // Larger buffer for better performance
            var total: Long = 0
            var count: Int

            // Read data from input and write to output
            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                // Update progress
                if (fileLength > 0) {
                    onProgressUpdate(total.toFloat() / fileLength.toFloat())
                }
                output.write(data, 0, count)
            }

            // Flush and close streams
            output.flush()
            output.close()
            input.close()

            // Return the path in the appropriate format
            if (fileType == "image") {
                "custom_artwork:$timestamp"
            } else {
                "file://${file.absolutePath}"
            }
        } catch (e: Exception) {
            Log.e("DialogViewModel", "Error downloading file from $url: ${e.message}", e)
            throw e
        }
    }

    fun resetDownloadState() {
        _downloadStatus.value = DownloadStatus.Idle
        _downloadProgress.value = 0f
    }
}
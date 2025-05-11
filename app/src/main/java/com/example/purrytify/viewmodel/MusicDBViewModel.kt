package com.example.purrytify.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.local.db.AppDatabase
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.data.local.db.entities.SongUploader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.net.toUri
import androidx.lifecycle.application
import com.example.purrytify.data.local.db.entities.LibraryEntity
import com.example.purrytify.data.local.db.entities.LibraryStatus
import com.example.purrytify.data.local.db.entities.RecentPlaysEntity
import kotlinx.coroutines.flow.forEach

class MusicDbViewModel(application: Application) : AndroidViewModel(application) {
    private val songDao = AppDatabase.Companion.getDatabase(application).songDao()
    val userEmail :String? = application.getSharedPreferences("app_prefs", MODE_PRIVATE).getString("email", "none")
    val allSongs: Flow<List<SongEntity>> =
        songDao.getSongsByUser(userEmail!!).map { entities ->
            entities.map { entity ->
                Toast.makeText(
                    application.applicationContext,
                    "Library songs: ${entity.id}, ${entity.title} ",
                    Toast.LENGTH_SHORT
                ).show()
                SongEntity(
                    id = entity.id,
                    title = entity.title,
                    artist = entity.artist,
                    duration = entity.duration,
                    uri = entity.uri,
                    artworkURI = entity.artworkURI ?: ""
                )
            }
        }

    val recentSongs: Flow<List<SongEntity>> =
        songDao.getRecentSongs(userEmail!!).map { entities ->
            entities.map { entity ->
                SongEntity(
                    id = entity.id,
                    title = entity.title,
                    artist = entity.artist,
                    duration = entity.duration,
                    uri = entity.uri,
                    artworkURI = entity.artworkURI ?: ""
                )
            }
        }

    val librarySongs: Flow<List<SongEntity>> =
        songDao.getLibrarySongs(userEmail!!).map { entities ->
            entities.map { entity ->
                SongEntity(
                    id = entity.id,
                    title = entity.title,
                    artist = entity.artist,
                    duration = entity.duration,
                    uri = entity.uri,
                    artworkURI = entity.artworkURI ?: ""
                )
            }
        }


    val likedSongs: Flow<List<SongEntity>> =
        songDao.getLikedSongs("13522042@std.stei.itb.ac.id").map { entities ->
            entities.map { entity ->
                SongEntity(
                    id = entity.id,
                    title = entity.title,
                    artist = entity.artist,
                    duration = entity.duration,
                    uri = entity.uri,
                    artworkURI = entity.artworkURI ?: ""
                )
            }
        }

    fun addRecentPlays(
        songs: List<SongEntity>,
        userEmail: String = "13522042@std.stei.itb.ac.id"
    ) {
        viewModelScope.launch {
            try {
                // Process each song
                songs.forEach { song ->
                    // First, get the song ID
                    val songId = songDao.getSongId(song.title, song.artist)

                    // Check if the recent play already exists
                    val exists = songDao.isRecentPlayExists(userEmail, songId)

                    if (!exists) {
                        // Create and insert the recent play entry
                        val recentPlay = RecentPlaysEntity(
                            userEmail = userEmail,
                            songId = songId
                        )
                        songDao.insertRecentPlay(recentPlay)

                        Log.d("MusicDbViewModel", "Added recent play for song: ${song.title}")
                    } else {
                        Log.d("MusicDbViewModel", "Recent play already exists for song: ${song.title}")
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicDbViewModel", "Error adding recent plays", e)
            }
        }
    }

    fun extractAndSaveArtwork(context: Context, uri: Uri): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val art = retriever.embeddedPicture
            if (art != null) {
                val filename = "artwork_${System.currentTimeMillis()}.jpg"
                val file = File(context.filesDir, filename)
                file.writeBytes(art)
                file.absolutePath
            } else null
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }


    fun insertSong(song: SongEntity, userEmail: String){
        viewModelScope.launch {
            val entity = SongEntity(
                title = song.title,
                artist = song.artist,
                duration = song.duration,
                uri = song.uri,
                artworkURI = song.artworkURI
            )
            val newId = songDao.insertSong(entity).toInt()
            Log.d("MusicViewModel", "Inserted id:${newId}")
            songDao.registerUserToSong(userEmail, newId)
        }
    }

    fun insertSongToLibrary(song: SongEntity){
        viewModelScope.launch {
            val entity = LibraryEntity(
                songId = song.id,
                userEmail = userEmail!!,
                libraryStatus = LibraryStatus.LIBRARY
            )
            val response = songDao.insertToLibrary(entity)
            Toast.makeText(
                application.applicationContext,
                "Added ${entity.songId} to Library",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun insertSongs(songs: List<SongEntity>, userEmail: String){
        songs.forEach { song ->
            insertSong(song, userEmail)
        }
    }

    fun checkAndInsertSong(
        context: Context,
        song: SongEntity,
        userEmail: String,
        onExists: () -> Unit
    ) {
        viewModelScope.launch {
            val existsForUser = songDao.isSongExistsForUser(song.title, song.artist, userEmail)
            val exists = songDao.isSongExists(song.title, song.artist)

            if (existsForUser) {
                onExists()
            } else if (exists) {
                val songId = songDao.getSongId(song.title, song.artist)
                val registerUploader = SongUploader(
                    uploaderEmail = userEmail,
                    songId = songId
                )
                songDao.registerUserToSong(registerUploader.uploaderEmail, registerUploader.songId)
            } else {
                val savedArtworkPath = extractAndSaveArtwork(context, song.uri.toUri()) ?: ""

                val entity = SongEntity(
                    title = song.title,
                    artist = song.artist,
                    duration = song.duration,
                    uri = song.uri,
                    artworkURI = savedArtworkPath
                )
                val newId = songDao.insertSong(entity).toInt()
                songDao.registerUserToSong(userEmail, newId)
            }
        }
    }

}
package com.example.purrytify.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
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

class MusicDbViewModel(application: Application) : AndroidViewModel(application) {
    private val songDao = AppDatabase.Companion.getDatabase(application).songDao()

    val allSongs: Flow<List<SongEntity>> =
        songDao.getSongsByUser("13522042@std.stei.itb.ac.id").map { entities ->
            entities.map { entity ->
                SongEntity(
                    title = entity.title,
                    artist = entity.artist,
                    duration = entity.duration,
                    uri = entity.uri,
                    artworkURI = entity.artworkURI ?: ""
                )
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
            songDao.registerUserToSong(userEmail, newId)
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
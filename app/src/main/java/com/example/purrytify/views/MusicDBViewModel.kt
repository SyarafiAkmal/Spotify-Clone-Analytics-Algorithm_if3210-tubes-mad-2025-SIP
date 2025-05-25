package com.example.purrytify.views

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.local.db.AppDatabase
import com.example.purrytify.data.local.db.entities.CapsuleEntity
import com.example.purrytify.data.local.db.entities.SongActivity
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.data.local.db.entities.SongUploader
import com.example.purrytify.data.local.db.entities.UserActivity
import com.example.purrytify.models.FormattedSongStreak
import com.example.purrytify.models.SongStreak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
//import com.example.purrytify.data.local.db.entities.LibraryStatus
import com.example.purrytify.utils.DateTimeUtils
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class MusicDbViewModel(application: Application) : AndroidViewModel(application) {
    private val songDao = AppDatabase.Companion.getDatabase(application).songDao()
    val userEmail :String? = application.getSharedPreferences("app_prefs", MODE_PRIVATE).getString("email", "none")

    val allSongs: Flow<List<SongEntity>> =
        songDao.getSongsByUser(userEmail!!).map { entities ->
//            Toast.makeText(
//                application.applicationContext,
//                "${entities.size} songs",
//                Toast.LENGTH_SHORT
//            ).show()
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

    val recentSongs: Flow<List<SongEntity>> =
        songDao.getRecentlyPlayedSongs(userEmail!!).map { entities ->
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
        songDao.getLikedSongs(userEmail!!).map { entities ->
//            Toast.makeText(
//                application.applicationContext,
//                "${entities.size} liked songs",
//                Toast.LENGTH_SHORT
//            ).show()
            entities.map { entity ->
//                Toast.makeText(
//                    application.applicationContext,
//                    "${entity.id} ${entity.title}",
//                    Toast.LENGTH_SHORT
//                ).show()
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

    suspend fun registerSongActivity(songId: Int, date: String) {
        try {
            songDao.registerSongActivity(userEmail!!, songId, date)
        } catch (e: Exception) {
            Log.e("RegisterSongActivity", "${e.message}", e)
        }
    }

    suspend fun isUserSongActivityExist(song: SongEntity): Boolean {
        val songDate: String? = songDao.getUserSongActivity(userEmail!!, song.id)?.date
        if (songDate !== null) {
            val dateTime = OffsetDateTime.parse(songDate).format(DateTimeFormatter.ofPattern("dd MMMM"))
            val currentDate = OffsetDateTime.parse(DateTimeUtils.getCurrentTimeIso()).format(DateTimeFormatter.ofPattern("dd MMMM"))
            return dateTime == currentDate
        }
        return false
    }

    suspend fun getStreakSong(): SongStreak? {
        val streak: SongStreak? = songDao.getStreakSong(userEmail!!)
        return streak?.copy(
            dateInterval = songDao.getStreakInterval(userEmail, streak.songId)
        )
    }

    suspend fun registerUserActivity() {
        try {
            songDao.registerUserActivity(userEmail!!)
            Toast.makeText(application.applicationContext, "Register User Succeed", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("RegisterUserActivity", "${e.message}", e)
        }
    }

    suspend fun isUserActivityExist(): Boolean {
        return try {
            songDao.isUserActivityExist(userEmail!!)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUserActivity(): UserActivity {
        return songDao.getUserActivity(userEmail!!)
    }

    suspend fun getSongById(songId: Int?): SongEntity? {
        if (songId !== null) {
            return songDao.getSongById(songId)
        }
        return null
    }

    suspend fun getSongUploader(songId: Int): SongUploader {
        return songDao.getSongUploader(songId, userEmail!!)
    }

    suspend fun getTopSongs(): List<SongEntity> {
        return songDao.getTopSongs(userEmail!!)
    }

    suspend fun getUserSongsActivity(): List<SongActivity> {
        return songDao.getUserSongsActivity(userEmail!!)
    }

    suspend fun registerCapsule(capsule: CapsuleEntity) {
        songDao.registerCapsule(
            userEmail = capsule.userEmail,
            capsuleDate = DateTimeUtils.getCurrentTimeIso(),
            minuteListened = capsule.minuteListened,
            topArtists = capsule.topArtists,
            topSongs = capsule.topSongs,
            songStreakInterval = capsule.songStreakInterval,
            songStreakId = capsule.songStreakId!!
        )
    }

    suspend fun getArtistPicture(artist: String): String {
        return songDao.getArtistPicture(artist)
    }

    suspend fun addTimesPlayed(songId: Int) {
        songDao.addTimesPlayed(userEmail!!, songId)
    }

    suspend fun updateTimeListened(timeListened: Int) {
        songDao.addUserTimeListened(userEmail!!, timeListened)
    }

    suspend fun getUserCapsules(): List<CapsuleEntity>? {
        return try {
            songDao.getCapsuleByUser(userEmail!!)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStatusSongById(id: Int, status: String): SongEntity? {
        return try {
            songDao.getStatusSongById(userEmail!!, id, status)
        } catch (e: Exception) {
            Log.e("MusicDbViewModel", "Error getting ${status} song: ${e.message}", e)
            null
        }
    }

    fun updateStatus(song: SongEntity, status: String) {
        viewModelScope.launch {
            try {
                songDao.updateLibraryStatus(
                    userEmail!!,
                    song.id,
                    status
                )

            } catch (e: Exception) {
                Log.e("MusicDbViewModel", "Error updating status", e)
            }
        }
    }

    fun updateLastPlayed(song: SongEntity) {
        viewModelScope.launch {
            try {
                songDao.updateLastPlayed(
                    userEmail!!,
                    song.id,
                    DateTimeUtils.getCurrentTimeIso()
                    )
            } catch (e: Exception) {
                Log.e("MusicDbViewModel", "Error updating last played", e)
            }
        }
    }

    fun insertSong(song: SongEntity){
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
            songDao.registerUserToSong(userEmail!!, newId, DateTimeUtils.getEmptyTimeValue(), "Library")
        }
    }

    fun insertSongs(songs: List<SongEntity>){
        songs.forEach { song ->
            insertSong(song)
        }
    }

    suspend fun getSongStatusCount(status: String): Int {
        return songDao.getSongsStatusCount(userEmail!!, status)
    }

    fun checkAndInsertSong(song: SongEntity) {
        viewModelScope.launch {
            try {
                val existsForUser = songDao.isSongExistsForUser(song.title, song.artist, song.id, userEmail!!)
                val exists = songDao.isSongExists(song.title, song.artist)

                if (existsForUser) {
                    Log.d("MusicDbViewModel", "Song already exists for user: ${song.title}")
                } else if (exists) {
                    try {
                        val songId = songDao.getSongId(song.title, song.artist)
                        songDao.registerUserToSong(
                            userEmail!!,
                            songId,
                            "library",
                            DateTimeUtils.getEmptyTimeValue(),
                        )
                        Log.d("MusicDbViewModel", "Registered existing song to user: ${song.title}")
                    } catch (e: Exception) {
                        Log.e("MusicDbViewModel", "Error registering existing song", e)
                        throw e
                    }
                } else {
                    try {
                        val entity = SongEntity(
                            title = song.title,
                            artist = song.artist,
                            duration = song.duration,
                            uri = song.uri,
                            artworkURI = song.artworkURI
                        )
                        val newId = songDao.insertSong(entity).toInt()
                        Log.d("MusicDbViewModel", "Inserted new song: ${song.title}, ID: $newId")

                        songDao.registerUserToSong(
                            userEmail!!,
                            newId,
                            "library",
                            DateTimeUtils.getEmptyTimeValue(),
                        )
                    } catch (e: Exception) {
                        Log.e("MusicDbViewModel", "Error inserting new song", e)
                        throw e
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicDbViewModel", "Error in checkAndInsertSong", e)
                throw e
            }
        }
    }

    suspend fun isSongExistForUser(song: SongEntity): Boolean {
        return songDao.isSongExistsForUser(song.title, song.artist, song.id, userEmail!!)
    }

}
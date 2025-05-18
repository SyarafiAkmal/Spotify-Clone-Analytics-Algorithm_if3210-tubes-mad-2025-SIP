package com.example.purrytify.data.local.db

import android.util.Log
import androidx.room.*
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.data.local.db.entities.SongUploader
import com.example.purrytify.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT EXISTS(SELECT 1 FROM song_entity AS s JOIN song_uploader AS su WHERE s.title = :title AND s.artist = :artist AND su.uploaderEmail = :userEmail AND s.id = su.songId)")
    suspend fun isSongExistsForUser(title: String, artist: String, userEmail: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM song_entity WHERE title = :title AND artist = :artist)")
    suspend fun isSongExists(title: String, artist: String): Boolean

    @Query("INSERT INTO song_uploader (uploaderEmail, songId, libraryStatus, lastPlayed) VALUES (:uploader, :songId, :libraryStatus, :lastPlayed)")
    suspend fun registerUserToSong(
        uploader: String,
        songId: Int,
        libraryStatus: String = "library",
        lastPlayed: String = DateTimeUtils.getEmptyTimeValue()
    )

    @Query("""
    SELECT s.* FROM song_entity AS s
    JOIN song_uploader AS su ON s.id = su.songId
    WHERE su.uploaderEmail = :userEmail AND su.lastPlayed != :emptyTimeValue
    ORDER BY su.lastPlayed DESC
    LIMIT :limit
    """)
    fun getRecentlyPlayedSongs(
        userEmail: String,
        limit: Int = 10,
        emptyTimeValue: String = DateTimeUtils.getEmptyTimeValue()
    ): Flow<List<SongEntity>>

    @Query("""
    SELECT su.* FROM song_uploader AS su
    WHERE uploaderEmail = :userEmail
    """)
    fun getSongUploader(
        userEmail: String
    ): Flow<List<SongUploader>>

    @Query("UPDATE song_uploader SET lastPlayed = :lastPlayed WHERE uploaderEmail = :uploader AND songId = :songId")
    suspend fun updateLastPlayed(uploader: String, songId: Int, lastPlayed: String = DateTimeUtils.getCurrentTimeIso())

    @Query("UPDATE song_uploader SET libraryStatus = :libraryStatus WHERE uploaderEmail = :uploader AND songId = :songId")
    suspend fun updateLibraryStatus(uploader: String, songId: Int, libraryStatus: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Query("""
        SELECT s.* FROM song_entity AS s
        JOIN song_uploader AS su ON s.id = su.songId
        WHERE su.uploaderEmail = :userEmail
        ORDER BY s.id ASC
    """)
    fun getSongsByUser(userEmail: String): Flow<List<SongEntity>>

    @Query("""
        SELECT s.* FROM song_entity AS s
        JOIN song_uploader AS su ON s.id = su.songId
        WHERE su.uploaderEmail = :userEmail AND su.libraryStatus = 'like'
    """)
    fun getLikedSongs(userEmail: String): Flow<List<SongEntity>>

    @Query("""
        SELECT s.* FROM song_entity AS s
        JOIN song_uploader AS su ON s.id = su.songId
        WHERE su.uploaderEmail = :userEmail AND su.libraryStatus = :status AND su.songId = :id
    """)
    suspend fun getStatusSongById(userEmail: String, id: Int, status: String): SongEntity?

    @Query("""
    SELECT COUNT(s.id) FROM song_entity AS s
    JOIN song_uploader AS su ON s.id = su.songId
    WHERE su.uploaderEmail = :userEmail AND su.libraryStatus = :libraryStatus
    """)
    suspend fun getSongsStatusCount(userEmail: String, libraryStatus: String): Int

    @Query("SELECT id FROM song_entity WHERE title = :title AND artist = :artist")
    suspend fun getSongId(title: String, artist: String): Int
}
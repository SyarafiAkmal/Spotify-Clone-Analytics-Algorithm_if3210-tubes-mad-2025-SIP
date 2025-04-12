package com.example.purrytify.data.local.db

import android.util.Log
import androidx.room.*
import com.example.purrytify.data.local.db.entities.RecentPlaysEntity
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.data.local.db.entities.SongUploader
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT EXISTS(SELECT 1 FROM song_entity AS s JOIN song_uploader AS su WHERE s.title = :title AND s.artist = :artist AND su.uploaderEmail = :userEmail AND s.id = su.songId)")
    suspend fun isSongExistsForUser(title: String, artist: String, userEmail: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM song_entity WHERE title = :title AND artist = :artist)")
    suspend fun isSongExists(title: String, artist: String): Boolean

    @Query("INSERT INTO song_uploader (uploaderEmail, songId) VALUES (:uploader, :songId)")
    suspend fun registerUserToSong(uploader: String, songId: Int)

    @Query("""
        SELECT EXISTS(SELECT 1 FROM recent_entity AS re WHERE re.userEmail = :userEmail AND re.songId = :songId)
    """)
    suspend fun isRecentPlayExists(userEmail: String, songId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentPlay(recentPlay: RecentPlaysEntity)

    @Query("INSERT INTO recent_entity (userEmail, songId) VALUES (:userEmail, :songId)")
    suspend fun addToRecentPlays(userEmail: String, songId: Int)

    @Query("""
        SELECT s.* FROM song_entity AS s
        JOIN song_uploader AS su ON s.id = su.songId
        WHERE su.uploaderEmail = :userEmail
    """)
    fun getSongsByUser(userEmail: String): Flow<List<SongEntity>>

    @Query("""
        SELECT s.* FROM song_entity AS s
        JOIN recent_entity AS re ON s.id = re.songId
        WHERE re.userEmail = :userEmail
    """)
    fun getRecentSongs(userEmail: String): Flow<List<SongEntity>>

    @Query("""
        SELECT s.* FROM song_entity AS s
        JOIN library_entity AS le ON s.id = le.songId
        WHERE le.userEmail = :userEmail AND le.libraryStatus = 'library'
    """)
    fun getLibrarySongs(userEmail: String): Flow<List<SongEntity>>

    @Query("""
        SELECT s.* FROM song_entity AS s
        JOIN library_entity AS le ON s.id = le.songId
        WHERE le.userEmail = :userEmail AND le.libraryStatus = 'like'
    """)
    fun getLikedSongs(userEmail: String): Flow<List<SongEntity>>

    @Query("""
        SELECT s.* FROM song_entity AS s
        JOIN library_entity AS le ON s.id = le.songId
        WHERE le.userEmail = :userEmail AND le.libraryStatus = 'listened'
    """)
    fun getListenedSongs(userEmail: String): Flow<List<SongEntity>>

    @Query("SELECT id FROM song_entity WHERE title = :title AND artist = :artist")
    suspend fun getSongId(title: String, artist: String): Int
}
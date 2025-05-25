package com.example.purrytify.data.local.db

import android.util.Log
import androidx.room.*
import com.example.purrytify.data.local.db.entities.CapsuleEntity
import com.example.purrytify.data.local.db.entities.SongActivity
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.data.local.db.entities.SongUploader
import com.example.purrytify.data.local.db.entities.UserActivity
import com.example.purrytify.models.SongStreak
import com.example.purrytify.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {


    // SONG ENTITY
    @Query("""
        SELECT EXISTS(SELECT 1 FROM song_entity AS s JOIN song_uploader AS su 
        WHERE s.title = :title AND s.artist = :artist AND su.uploaderEmail = :userEmail AND s.id = su.songId AND s.id = :songId)
    """)
    suspend fun isSongExistsForUser(title: String, artist: String, songId: Int, userEmail: String): Boolean

    @Query("""
        SELECT EXISTS(SELECT 1 FROM song_entity 
        WHERE title = :title AND artist = :artist)
    """)
    suspend fun isSongExists(title: String, artist: String): Boolean

    @Query("""
        SELECT su.* FROM song_uploader AS su 
        WHERE su.uploaderEmail = :userEmail AND su.songId = :songId
    """)
    suspend fun getSongUploader(songId: Int, userEmail: String): SongUploader

    @Query("""
        INSERT INTO song_uploader (uploaderEmail, songId, libraryStatus, lastPlayed, timesPlayed) 
        VALUES (:uploader, :songId, :libraryStatus, :lastPlayed, :timesPlayed)
        """)
    suspend fun registerUserToSong(
        uploader: String,
        songId: Int,
        libraryStatus: String = "library",
        lastPlayed: String = DateTimeUtils.getEmptyTimeValue(),
        timesPlayed: Int = 0
    )

    @Query("""
        UPDATE song_uploader SET timesPlayed = timesPlayed + 1
        WHERE songId = :songId AND uploaderEmail = :userEmail
    """)
    suspend fun addTimesPlayed(userEmail: String, songId: Int)

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
        SELECT s.* FROM song_entity AS s
        JOIN song_uploader AS su ON s.id = su.songId
        WHERE su.uploaderEmail = :userEmail AND su.timesPlayed != 0
        ORDER BY su.timesPlayed DESC
    """)
    suspend fun getTopSongs(userEmail: String): List<SongEntity>

    @Query("""
        SELECT s.artist FROM song_entity AS s
        JOIN song_uploader AS su ON s.id = su.songId
        WHERE su.uploaderEmail = :userEmail AND su.timesPlayed != 0
        ORDER BY su.timesPlayed DESC
    """)
    suspend fun getTopArtist(userEmail: String): List<String>

    @Query("""
        SELECT s.artworkURI FROM song_entity AS s
        WHERE s.artist = :artist
        LIMIT 1
    """)
    suspend fun getArtistPicture(artist: String): String

    @Query("""
    SELECT su.* FROM song_uploader AS su
    WHERE uploaderEmail = :userEmail
    """)
    fun getSongUploader(
        userEmail: String
    ): Flow<List<SongUploader>>

    @Query("""
        UPDATE song_uploader SET lastPlayed = :lastPlayed 
        WHERE uploaderEmail = :uploader AND songId = :songId
    """)
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

    @Query("""
        SELECT id FROM song_entity 
        WHERE title = :title AND artist = :artist
        """)
    suspend fun getSongId(title: String, artist: String): Int

    @Query("""
        SELECT * FROM song_entity 
        WHERE id = :songId
        """)
    suspend fun getSongById(songId: Int): SongEntity


    // USER SONG ACTIVITY
    @Query("""
        INSERT INTO song_activity (songListener, songId, date)
        VALUES (:userEmail, :songId, :date)
    """)
    suspend fun registerSongActivity(userEmail: String, songId: Int, date: String)

    @Query("""
        INSERT INTO user_activity (userEmail, timeListened)
        VALUES (:userEmail, :timeListened)
    """)
    suspend fun registerUserActivity(userEmail: String, timeListened: Int = 0)

    @Query("""
        SELECT se.id as songId, saj.frequency as frequency FROM song_entity AS se JOIN
        (SELECT sa.songId, COUNT(*) as frequency 
        FROM song_activity AS sa 
        WHERE sa.songListener = :userEmail
        GROUP BY sa.songId
        HAVING COUNT(*) >= 2
        ORDER BY COUNT(*) DESC
        LIMIT 1) AS saj ON saj.songId = se.id
    """)
    suspend fun getStreakSong(userEmail: String): SongStreak?

    @Query("""
        SELECT MIN(sa.date) || ',' || MAX(sa.date) as dateInterval
        FROM song_activity AS sa 
        WHERE sa.songListener = :userEmail AND sa.songId = :songId
    """)
    suspend fun getStreakInterval(userEmail: String, songId: Int): String?

    @Query("""
        UPDATE user_activity SET timeListened = timeListened + :timeAdd 
        WHERE userEmail = :userEmail
    """)
    suspend fun addUserTimeListened(userEmail: String, timeAdd: Int)

    @Query("""
        SELECT sa.* FROM song_activity AS sa WHERE sa.songListener = :userEmail AND sa.songId = :songId
    """)
    suspend fun getUserSongActivity(userEmail: String, songId: Int): SongActivity?

    @Query("""
        SELECT sa.* FROM song_activity AS sa WHERE sa.songListener = :userEmail
    """)
    suspend fun getUserSongsActivity(userEmail: String): List<SongActivity>

    @Query("""
        SELECT EXISTS(SELECT 1 FROM user_activity AS ua WHERE ua.userEmail = :userEmail)
    """)
    suspend fun isUserActivityExist(userEmail: String): Boolean

    @Query("""
        SELECT ua.* FROM user_activity AS ua WHERE ua.userEmail = :userEmail
    """)
    suspend fun getUserActivity(userEmail: String): UserActivity


    // CAPSULE
    @Query("""
        SELECT c.* FROM capsule_entity AS c
        WHERE c.userEmail = :userEmail
        ORDER BY c.capsuleDate DESC
        LIMIT :limit
    """)
    suspend fun getCapsuleByUser(userEmail: String, limit: Int = 3): List<CapsuleEntity>

    @Query("""
        INSERT INTO capsule_entity (userEmail, capsuleDate, minuteListened, topArtists, topSongs, songStreakInterval, songStreakId)
        VALUES (:userEmail, :capsuleDate, :minuteListened, :topArtists, :topSongs, :songStreakInterval, :songStreakId)
    """)
    suspend fun registerCapsule(
        userEmail: String,
        capsuleDate: String,
        minuteListened: Int,
        topArtists: String,
        topSongs: String,
        songStreakInterval: String,
        songStreakId: Int
    )

}
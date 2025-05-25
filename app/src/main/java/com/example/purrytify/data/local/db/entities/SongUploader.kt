package com.example.purrytify.data.local.db.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "song_uploader",
    primaryKeys = ["uploaderEmail", "songId"]
)
data class SongUploader(
    val uploaderEmail: String,
    val songId: Int,
    val lastPlayed: String, // ISO string of time format, important for recently played
    val libraryStatus: String, // Liked, Listened, by default it's library but all songs go to library
    val timesPlayed: Int
)

package com.example.purrytify.data.local.db.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "song_entity"
)
data class SongEntity(
    @PrimaryKey(
        autoGenerate = true
    ) val id: Int = 0,
    val title: String,
    val artist: String,
    val artworkURI: String, // path ke cover lagu @drawable/starboy
    val uri: String, // path ke file mp3 di local storage
    val duration : Long
)
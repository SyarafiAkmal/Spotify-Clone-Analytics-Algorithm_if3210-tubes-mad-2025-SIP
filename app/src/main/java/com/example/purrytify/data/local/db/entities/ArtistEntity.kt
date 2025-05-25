package com.example.purrytify.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "artist_entity"
)
data class ArtistEntity (
    @PrimaryKey(autoGenerate = true)
    val artistId: Int = 0,
    val artistName: String,
    val artistPicture: String
)
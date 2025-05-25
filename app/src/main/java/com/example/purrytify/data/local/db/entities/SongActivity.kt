package com.example.purrytify.data.local.db.entities

import androidx.room.Entity

@Entity(
    tableName = "song_activity",
    primaryKeys = ["songListener", "songId", "date"]
)
data class SongActivity (
    val songListener: String,
    val songId: Int,
    val date: String // ISO
)
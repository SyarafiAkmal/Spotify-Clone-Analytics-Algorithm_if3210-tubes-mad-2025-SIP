package com.example.purrytify.data.local.db.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "capsule_entity",
    primaryKeys = ["userEmail", "capsuleDate"]
)
data class CapsuleEntity(
    val userEmail: String,
    val capsuleDate: String, // format is ISO
    val minuteListened: Int,
    val topArtists: String, // format is artist1, artist2, ...
    val topSongs: String, // format is songId1, songId2, ...
    val songStreakInterval: String, // ISO : date1, date2
    val songStreakId: Int?
)
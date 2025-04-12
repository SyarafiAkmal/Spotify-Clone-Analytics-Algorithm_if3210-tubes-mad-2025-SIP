package com.example.purrytify.data.local.db.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_entity",
    primaryKeys = ["userEmail", "songId"]
)
data class RecentPlaysEntity(
    val userEmail: String,
    val songId: Int,
)
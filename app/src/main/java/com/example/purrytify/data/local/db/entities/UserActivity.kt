package com.example.purrytify.data.local.db.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_activity"
)
data class UserActivity (
    @PrimaryKey
    val userEmail: String,
    val timeListened: Int // In milliseconds
)
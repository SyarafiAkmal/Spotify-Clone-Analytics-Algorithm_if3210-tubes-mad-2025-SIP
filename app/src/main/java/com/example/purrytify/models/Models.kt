package com.example.purrytify.models

import com.example.purrytify.data.local.db.entities.SongEntity
import okhttp3.MultipartBody

data class Profile (
    val id: Int,
    val username: String,
    val email: String,
    val location: String,
    val profilePhoto: String,
    val createdAt: String,
    val updatedAt: String
)

data class Login (
    val email: String,
    val password: String
)

data class Token (
    val accessToken: String,
    val refreshToken: String
)

data class Message (
    val message: String
)

data class User (
    val id: Int,
    val username: String
)

data class EditProfile (
    val location: String? = null,
    val profilePhoto: MultipartBody.Part? = null
)

data class VerifyToken (
    val valid: Boolean,
    val user: User
)

data class RefreshToken (
    val refreshToken: String
)

data class OnlineSong (
    val id: Int,
    val title: String,
    val artist: String,
    val artwork: String,
    val url: String,
    val duration: String,
    val country: String,
    val rank: String,
    val createdAt: String,
    val updatedAt: String
)

data class SongStreak (
    val songId: Int,
    val frequency: Int,
    val dateInterval: String? = ""
)

data class FormattedSongStreak (
    val song: SongEntity,
    val frequency: Int,
    val dateInterval: String? = ""
)
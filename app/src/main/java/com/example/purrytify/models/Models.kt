package com.example.purrytify.models

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

data class User (
    val id: Int,
    val username: String
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
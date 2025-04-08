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
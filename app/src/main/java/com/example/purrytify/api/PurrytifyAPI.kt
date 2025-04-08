package com.example.purrytify.api

import com.example.purrytify.models.Login
import com.example.purrytify.models.Profile
import com.example.purrytify.models.Token
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface PurrytifyAPI {

    @POST("api/login")
    suspend fun login(@Body loginRequest: Login): Token

    @GET("api/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Profile
}
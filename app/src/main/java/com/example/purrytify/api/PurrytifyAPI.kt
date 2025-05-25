package com.example.purrytify.api

import com.example.purrytify.models.EditProfile
import com.example.purrytify.models.Login
import com.example.purrytify.models.Message
import com.example.purrytify.models.OnlineSong
import com.example.purrytify.models.Profile
import com.example.purrytify.models.RefreshToken
import com.example.purrytify.models.Token
import com.example.purrytify.models.VerifyToken
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.Part

interface PurrytifyAPI {

    @POST("api/login")
    suspend fun login(@Body loginRequest: Login): Token

    @GET("api/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Profile

    @GET("uploads/profile-picture/{picturePath}")
    suspend fun getProfilePict(
        @Path("picturePath") pictureId: String,
        @Header("Authorization") token: String
    ): ResponseBody

    @GET("api/top-songs/{countryCode}")
    suspend fun getLocalSongs(
        @Path("countryCode") countryCode: String
    ): List<OnlineSong>

    @GET("api/top-songs/global")
    suspend fun getGlobalSongs(): List<OnlineSong>

    @GET("api/verify-token")
    suspend fun verifyToken(
        @Header("Authorization") token: String
    ): VerifyToken

    @POST("api/refresh-token")
    suspend fun refreshToken(@Body refreshToken: RefreshToken): Token

    @Multipart
    @PATCH("api/profile")
    suspend fun editProfile(
        @Part("location") location: RequestBody,
        @Part profilePhoto: MultipartBody.Part?,
        @Header("Authorization") token: String
    ): Response<Message>

    @GET("/api/songs/{id}")
    suspend fun getSongById(@Path("id") id: Int): OnlineSong
}
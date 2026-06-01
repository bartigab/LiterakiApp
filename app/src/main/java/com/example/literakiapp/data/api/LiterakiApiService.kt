package com.example.literakiapp.data.api

import com.example.literakiapp.data.model.ApiCurrentUser
import com.example.literakiapp.data.model.ApiGame
import com.example.literakiapp.data.model.ApiMove
import com.example.literakiapp.data.model.AuthRequest
import com.example.literakiapp.data.model.AuthResponse
import com.example.literakiapp.data.model.BasicMoveRequest
import com.example.literakiapp.data.model.ExchangeTilesMoveRequest
import com.example.literakiapp.data.model.PlaceTilesMoveRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LiterakiApiService {
    @POST("api/v1/auth")
    suspend fun authenticateGuest(@Body request: AuthRequest): Response<AuthResponse>

    @GET("api/v1/me")
    suspend fun getCurrentUser(): Response<ApiCurrentUser>

    @GET("api/v1/games")
    suspend fun getGames(): Response<List<ApiGame>>

    @POST("api/v1/games")
    suspend fun createGame(): Response<ApiGame>

    @GET("api/v1/games/{id}")
    suspend fun getGame(@Path("id") id: Int): Response<ApiGame>

    @POST("api/v1/games/{id}/join")
    suspend fun joinGame(@Path("id") id: Int): Response<ApiGame>

    @POST("api/v1/games/{id}/start")
    suspend fun startGame(@Path("id") id: Int): Response<ApiGame>

    @POST("api/v1/games/{id}/moves")
    suspend fun passTurn(
        @Path("id") id: Int,
        @Body request: BasicMoveRequest
    ): Response<ApiMove>

    @POST("api/v1/games/{id}/moves")
    suspend fun resignGame(
        @Path("id") id: Int,
        @Body request: BasicMoveRequest
    ): Response<ApiMove>

    @POST("api/v1/games/{id}/moves")
    suspend fun placeTiles(
        @Path("id") id: Int,
        @Body request: PlaceTilesMoveRequest
    ): Response<ApiMove>

    @POST("api/v1/games/{id}/moves")
    suspend fun exchangeTiles(
        @Path("id") id: Int,
        @Body request: ExchangeTilesMoveRequest
    ): Response<ApiMove>
}


package com.example.literakiapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRequest(
    val username: String
)

@JsonClass(generateAdapter = true)
data class BasicMoveRequest(
    @Json(name = "move_type") val moveType: String
)

@JsonClass(generateAdapter = true)
data class PlaceTilesMoveRequest(
    @Json(name = "move_type") val moveType: String = "place_tiles",
    val tiles: List<ApiTilePlacement>
)

@JsonClass(generateAdapter = true)
data class ExchangeTilesMoveRequest(
    @Json(name = "move_type") val moveType: String = "exchange_tiles",
    val tiles: List<String>
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val user: ApiUser,
    val token: String
)

@JsonClass(generateAdapter = true)
data class ApiUser(
    val id: Int,
    val username: String
)

@JsonClass(generateAdapter = true)
data class ApiCurrentUser(
    val id: Int,
    val username: String,
    @Json(name = "games_played") val gamesPlayed: Int,
    @Json(name = "games_won") val gamesWon: Int,
    @Json(name = "total_score") val totalScore: Int
)

@JsonClass(generateAdapter = true)
data class ApiGame(
    val id: Int,
    val status: String,
    val board: Map<String, String> = emptyMap(),
    val players: List<ApiPlayer> = emptyList(),
    @Json(name = "current_turn_user_id") val currentTurnUserId: Int?,
    @Json(name = "winner_id") val winnerId: Int?,
    @Json(name = "started_at") val startedAt: String?,
    @Json(name = "finished_at") val finishedAt: String?,
    val moves: List<ApiMove> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiPlayer(
    val id: Int,
    val username: String,
    val score: Int,
    val position: Int,
    @Json(name = "passed_turns_count") val passedTurnsCount: Int,
    val rack: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ApiMove(
    val id: Int,
    @Json(name = "game_id") val gameId: Int,
    @Json(name = "user_id") val userId: Int,
    @Json(name = "move_type") val moveType: String,
    val tiles: List<ApiMoveTile> = emptyList(),
    val words: List<String> = emptyList(),
    val score: Int,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class ApiMoveTile(
    val letter: String,
    val x: Int? = null,
    val y: Int? = null
)

@JsonClass(generateAdapter = true)
data class ApiTilePlacement(
    val letter: String,
    val x: Int,
    val y: Int
)


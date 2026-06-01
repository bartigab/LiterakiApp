package com.example.literakiapp.data.repository

import com.example.literakiapp.data.SavedSession
import com.example.literakiapp.data.SessionStore
import com.example.literakiapp.data.api.LiterakiApiService
import com.example.literakiapp.data.model.ApiCurrentUser
import com.example.literakiapp.data.model.ApiGame
import com.example.literakiapp.data.model.ApiTilePlacement
import com.example.literakiapp.data.model.AuthRequest
import com.example.literakiapp.data.model.BasicMoveRequest
import com.example.literakiapp.data.model.ExchangeTilesMoveRequest
import com.example.literakiapp.data.model.PlaceTilesMoveRequest
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException

open class AppException(message: String) : Exception(message)

class SessionExpiredException(
    message: String = "Sesja wygasła. Zaloguj się ponownie."
) : AppException(message)

class LiterakiRepository(
    private val apiService: LiterakiApiService,
    private val sessionStore: SessionStore
) {
    fun hasSavedSession(): Boolean = sessionStore.hasActiveSession()

    fun getSavedSession(): SavedSession? = sessionStore.getSession()

    fun logout() {
        sessionStore.clearSession()
    }

    suspend fun authenticateGuest(username: String): ApiCurrentUser {
        val authResponse = executeRequest(
            request = { apiService.authenticateGuest(AuthRequest(username.trim())) },
            defaultMessage = "Nie udało się zalogować jako gość."
        )

        sessionStore.saveSession(
            SavedSession(
                userId = authResponse.user.id,
                username = authResponse.user.username,
                token = authResponse.token
            )
        )

        return getCurrentUser()
    }

    suspend fun getCurrentUser(): ApiCurrentUser = executeAuthorizedRequest(
        request = { apiService.getCurrentUser() },
        defaultMessage = "Nie udało się pobrać danych użytkownika."
    )

    suspend fun getGames(): List<ApiGame> = executeAuthorizedRequest(
        request = { apiService.getGames() },
        defaultMessage = "Nie udało się pobrać listy gier."
    )

    suspend fun createGame(): ApiGame = executeAuthorizedRequest(
        request = { apiService.createGame() },
        defaultMessage = "Nie udało się utworzyć gry."
    )

    suspend fun getGame(gameId: Int): ApiGame = executeAuthorizedRequest(
        request = { apiService.getGame(gameId) },
        defaultMessage = "Nie udało się pobrać szczegółów gry."
    )

    suspend fun joinGame(gameId: Int): ApiGame = executeAuthorizedRequest(
        request = { apiService.joinGame(gameId) },
        defaultMessage = "Nie udało się dołączyć do gry."
    )

    suspend fun startGame(gameId: Int): ApiGame = executeAuthorizedRequest(
        request = { apiService.startGame(gameId) },
        defaultMessage = "Nie udało się rozpocząć gry."
    )

    suspend fun passTurn(gameId: Int): ApiGame {
        executeAuthorizedRequest(
            request = { apiService.passTurn(gameId, BasicMoveRequest(moveType = "pass")) },
            defaultMessage = "Nie udało się pominąć tury."
        )
        return getGame(gameId)
    }

    suspend fun resignGame(gameId: Int): ApiGame {
        executeAuthorizedRequest(
            request = { apiService.resignGame(gameId, BasicMoveRequest(moveType = "resign")) },
            defaultMessage = "Nie udało się poddać gry."
        )
        return getGame(gameId)
    }

    suspend fun placeTiles(gameId: Int, tiles: List<ApiTilePlacement>): ApiGame {
        executeAuthorizedRequest(
            request = { apiService.placeTiles(gameId, PlaceTilesMoveRequest(tiles = tiles)) },
            defaultMessage = "Nie udało się zagrać słowa."
        )
        return getGame(gameId)
    }

    suspend fun exchangeTiles(gameId: Int, letters: List<String>): ApiGame {
        executeAuthorizedRequest(
            request = { apiService.exchangeTiles(gameId, ExchangeTilesMoveRequest(tiles = letters)) },
            defaultMessage = "Nie udało się wymienić liter."
        )
        return getGame(gameId)
    }

    private suspend fun <T> executeAuthorizedRequest(
        request: suspend () -> Response<T>,
        defaultMessage: String
    ): T = executeRequest(
        request = request,
        defaultMessage = defaultMessage,
        clearSessionOnUnauthorized = true
    )

    private suspend fun <T> executeRequest(
        request: suspend () -> Response<T>,
        defaultMessage: String,
        clearSessionOnUnauthorized: Boolean = false
    ): T {
        try {
            val response = request()
            if (response.isSuccessful) {
                return response.body() ?: throw AppException("Serwer zwrócił pustą odpowiedź.")
            }

            val errorMessage = parseErrorMessage(response.errorBody()?.string(), defaultMessage)
            if (clearSessionOnUnauthorized && response.code() == 401) {
                sessionStore.clearSession()
                throw SessionExpiredException(errorMessage)
            }

            throw AppException(errorMessage)
        } catch (exception: SessionExpiredException) {
            throw exception
        } catch (exception: IOException) {
            throw AppException("Brak połączenia z serwerem. Sprawdź Internet i spróbuj ponownie.")
        }
    }

    private fun parseErrorMessage(rawBody: String?, fallbackMessage: String): String {
        if (rawBody.isNullOrBlank()) {
            return fallbackMessage
        }

        return try {
            val json = JSONObject(rawBody)

            when {
                json.has("errors") -> {
                    val errors = json.getJSONArray("errors")
                    buildString {
                        for (index in 0 until errors.length()) {
                            if (index > 0) append('\n')
                            append(errors.getString(index))
                        }
                    }.ifBlank { fallbackMessage }
                }

                json.has("error") -> json.getString("error").ifBlank { fallbackMessage }
                else -> fallbackMessage
            }
        } catch (_: Exception) {
            fallbackMessage
        }
    }
}


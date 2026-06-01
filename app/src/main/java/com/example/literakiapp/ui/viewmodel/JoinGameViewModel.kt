package com.example.literakiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.literakiapp.data.model.ApiGame
import com.example.literakiapp.data.repository.AppException
import com.example.literakiapp.data.repository.LiterakiRepository
import com.example.literakiapp.data.repository.SessionExpiredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JoinGameUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val joinedGame: ApiGame? = null,
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false
)

class JoinGameViewModel(
    private val repository: LiterakiRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(JoinGameUiState())
    val uiState: StateFlow<JoinGameUiState> = _uiState.asStateFlow()

    fun onCodeChanged(value: String) {
        val digitsOnly = value.filter(Char::isDigit).take(8)
        _uiState.update {
            it.copy(
                code = digitsOnly,
                errorMessage = null,
                sessionExpired = false,
                joinedGame = null
            )
        }
    }

    fun joinGame() {
        if (uiState.value.isLoading) return

        val gameId = uiState.value.code.toIntOrNull()
        if (gameId == null || gameId <= 0) {
            _uiState.update { it.copy(errorMessage = "Podaj poprawny kod lobby (numer gry).") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, sessionExpired = false) }
            try {
                val game = repository.joinGame(gameId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        joinedGame = game,
                        errorMessage = null
                    )
                }
            } catch (exception: SessionExpiredException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message,
                        sessionExpired = true
                    )
                }
            } catch (exception: AppException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message
                    )
                }
            }
        }
    }

    companion object {
        fun factory(repository: LiterakiRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return JoinGameViewModel(repository) as T
                }
            }
    }
}


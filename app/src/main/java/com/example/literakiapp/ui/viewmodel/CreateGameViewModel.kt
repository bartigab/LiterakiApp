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

data class CreateGameUiState(
    val isLoading: Boolean = false,
    val createdGame: ApiGame? = null,
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false
)

class CreateGameViewModel(
    private val repository: LiterakiRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateGameUiState())
    val uiState: StateFlow<CreateGameUiState> = _uiState.asStateFlow()

    fun createGame() {
        if (uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, sessionExpired = false) }
            try {
                val game = repository.createGame()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        createdGame = game,
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun factory(repository: LiterakiRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CreateGameViewModel(repository) as T
                }
            }
    }
}


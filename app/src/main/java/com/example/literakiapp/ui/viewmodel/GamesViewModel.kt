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

data class GamesUiState(
    val currentUserId: Int? = null,
    val games: List<ApiGame> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false
)

class GamesViewModel(
    private val repository: LiterakiRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GamesUiState())
    val uiState: StateFlow<GamesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, sessionExpired = false) }
            try {
                val currentUser = repository.getCurrentUser()
                val games = repository.getGames()
                    .sortedByDescending { it.id }
                _uiState.update {
                    it.copy(
                        currentUserId = currentUser.id,
                        games = games,
                        isLoading = false,
                        errorMessage = null,
                        sessionExpired = false
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
                    return GamesViewModel(repository) as T
                }
            }
    }
}


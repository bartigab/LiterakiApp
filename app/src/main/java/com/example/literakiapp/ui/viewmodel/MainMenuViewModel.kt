package com.example.literakiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.literakiapp.data.model.ApiCurrentUser
import com.example.literakiapp.data.repository.AppException
import com.example.literakiapp.data.repository.LiterakiRepository
import com.example.literakiapp.data.repository.SessionExpiredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainMenuUiState(
    val user: ApiCurrentUser? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false
)

class MainMenuViewModel(
    private val repository: LiterakiRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainMenuUiState())
    val uiState: StateFlow<MainMenuUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, sessionExpired = false) }
            try {
                val user = repository.getCurrentUser()
                _uiState.update {
                    it.copy(
                        user = user,
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

    fun logout() {
        repository.logout()
        _uiState.update { MainMenuUiState(isLoading = false, sessionExpired = true) }
    }

    companion object {
        fun factory(repository: LiterakiRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainMenuViewModel(repository) as T
                }
            }
    }
}


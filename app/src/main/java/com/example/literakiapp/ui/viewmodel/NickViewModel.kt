package com.example.literakiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.literakiapp.data.repository.AppException
import com.example.literakiapp.data.repository.LiterakiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NickUiState(
    val nick: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginCompleted: Boolean = false
)

class NickViewModel(
    private val repository: LiterakiRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NickUiState())
    val uiState: StateFlow<NickUiState> = _uiState.asStateFlow()

    fun onNickChanged(value: String) {
        _uiState.update {
            it.copy(
                nick = value,
                errorMessage = null,
                loginCompleted = false
            )
        }
    }

    fun signInAsGuest() {
        val trimmedNick = uiState.value.nick.trim()
        if (trimmedNick.length < 3) {
            _uiState.update { it.copy(errorMessage = "Nick musi mieć co najmniej 3 znaki") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.authenticateGuest(trimmedNick)
                _uiState.update { it.copy(isLoading = false, loginCompleted = true) }
            } catch (exception: AppException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Nie udało się zalogować."
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
                    return NickViewModel(repository) as T
                }
            }
    }
}


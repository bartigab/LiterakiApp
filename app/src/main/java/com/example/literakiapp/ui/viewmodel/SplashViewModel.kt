package com.example.literakiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.literakiapp.data.repository.LiterakiRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SplashDestination {
    NICK,
    MAIN_MENU
}

data class SplashUiState(
    val isLoading: Boolean = true,
    val destination: SplashDestination? = null
)

class SplashViewModel(
    private val repository: LiterakiRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        resolveStartDestination()
    }

    private fun resolveStartDestination() {
        viewModelScope.launch {
            delay(1400)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    destination = if (repository.hasSavedSession()) {
                        SplashDestination.MAIN_MENU
                    } else {
                        SplashDestination.NICK
                    }
                )
            }
        }
    }

    companion object {
        fun factory(repository: LiterakiRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SplashViewModel(repository) as T
                }
            }
    }
}


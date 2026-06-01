package com.example.literakiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.literakiapp.data.model.ApiGame
import com.example.literakiapp.data.model.ApiTilePlacement
import com.example.literakiapp.data.repository.AppException
import com.example.literakiapp.data.repository.LiterakiRepository
import com.example.literakiapp.data.repository.SessionExpiredException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class GameDetailsUiState(
    val currentUserId: Int? = null,
    val game: ApiGame? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSubmittingMove: Boolean = false,
    val isStartingGame: Boolean = false,
    val draftTiles: Map<String, String> = emptyMap(),
    val selectedRackIndices: Set<Int> = emptySet(),
    val errorMessage: String? = null,
    val sessionExpired: Boolean = false
)

class GameDetailsViewModel(
    private val repository: LiterakiRepository,
    private val gameId: Int
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameDetailsUiState())
    val uiState: StateFlow<GameDetailsUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null
    private var autoRefreshJob: Job? = null

    init {
        refresh()
        startAutoRefresh()
    }

    fun refresh() {
        refreshInternal(showLoading = uiState.value.game == null, preserveMessage = false)
    }

    fun startGame() {
        if (uiState.value.isStartingGame) return

        viewModelScope.launch {
            _uiState.update { it.copy(isStartingGame = true, errorMessage = null, sessionExpired = false) }
            try {
                val game = repository.startGame(gameId)
                _uiState.update {
                    it.copy(
                        game = game,
                        draftTiles = emptyMap(),
                        isStartingGame = false,
                        errorMessage = null
                    )
                }
            } catch (exception: SessionExpiredException) {
                _uiState.update {
                    it.copy(
                        isStartingGame = false,
                        errorMessage = exception.message,
                        sessionExpired = true
                    )
                }
            } catch (exception: AppException) {
                _uiState.update {
                    it.copy(
                        isStartingGame = false,
                        errorMessage = exception.message
                    )
                }
            }
        }
    }

    fun onDraftTileChanged(x: Int, y: Int, value: String) {
        val key = boardCellKey(x, y)
        val normalizedValue = normalizeDraftLetter(value)

        _uiState.update {
            if (it.game?.board?.containsKey(key) == true) {
                it
            } else {
                val updatedDraftTiles = it.draftTiles.toMutableMap().apply {
                    if (normalizedValue.isBlank()) {
                        remove(key)
                    } else {
                        put(key, normalizedValue)
                    }
                }

                it.copy(
                    draftTiles = updatedDraftTiles,
                    errorMessage = null
                )
            }
        }
    }

    fun onRackTileSelectionChanged(index: Int) {
        _uiState.update { state ->
            val rack = currentRack(state)
            if (index !in rack.indices) {
                state
            } else {
                val updatedSelection = state.selectedRackIndices.toMutableSet().apply {
                    if (!add(index)) {
                        remove(index)
                    }
                }

                state.copy(
                    selectedRackIndices = updatedSelection,
                    errorMessage = null
                )
            }
        }
    }

    fun clearDraftTiles() {
        _uiState.update {
            it.copy(
                draftTiles = emptyMap(),
                errorMessage = null
            )
        }
    }

    fun passTurn() {
        submitGameAction { repository.passTurn(gameId) }
    }

    fun exchangeSelectedTiles() {
        val state = uiState.value

        if (state.draftTiles.isNotEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Przed wymianą liter wyczyść szkic z planszy.")
            }
            return
        }

        val rack = currentRack(state)
        val selectedLetters = state.selectedRackIndices
            .sorted()
            .mapNotNull(rack::getOrNull)
            .map(::normalizeDraftLetter)
            .filter(String::isNotBlank)

        if (selectedLetters.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Zaznacz co najmniej jedną literę do wymiany.")
            }
            return
        }

        submitGameAction(
            successMessage = "Wymieniono ${selectedLetters.size} liter(y). Otrzymałeś nowe litery i straciłeś kolejkę."
        ) {
            repository.exchangeTiles(gameId, selectedLetters)
        }
    }

    fun resignGame() {
        submitGameAction { repository.resignGame(gameId) }
    }

    fun placeTiles() {
        val state = uiState.value
        val tiles = buildTilePlacements(state.draftTiles)

        if (tiles.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Wpisz co najmniej jedną literę bezpośrednio na planszy.") }
            return
        }

        val myRack = state.game
            ?.players
            ?.firstOrNull { it.id == state.currentUserId }
            ?.rack
            .orEmpty()

        if (!rackContainsAllLetters(myRack, tiles.map(ApiTilePlacement::letter))) {
            _uiState.update { it.copy(errorMessage = "Szkic używa liter, których nie ma aktualnie w Twoim racku.") }
            return
        }

        val validation = validateTilePlacementMove(
            board = state.game?.board.orEmpty(),
            tiles = tiles
        )
        if (!validation.isValid) {
            _uiState.update { it.copy(errorMessage = validation.errorMessage) }
            return
        }

        submitGameAction {
            repository.placeTiles(gameId, tiles)
        }
    }

    private fun refreshInternal(
        showLoading: Boolean,
        preserveMessage: Boolean
    ) {
        if (refreshJob?.isActive == true) return

        refreshJob = viewModelScope.launch {
            val shouldShowLoading = showLoading && uiState.value.game == null
            _uiState.update {
                it.copy(
                    isLoading = if (shouldShowLoading) true else it.isLoading,
                    isRefreshing = !shouldShowLoading && it.game != null,
                    errorMessage = if (preserveMessage) it.errorMessage else null,
                    sessionExpired = false
                )
            }

            try {
                val currentUser = repository.getCurrentUser()
                val game = repository.getGame(gameId)
                _uiState.update {
                    it.copy(
                        currentUserId = currentUser.id,
                        game = game,
                        draftTiles = sanitizeDraftTiles(game.board, it.draftTiles),
                        selectedRackIndices = sanitizeSelectedRackIndices(game, currentUser.id, it.selectedRackIndices),
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = if (preserveMessage) it.errorMessage else null,
                        sessionExpired = false
                    )
                }
            } catch (exception: SessionExpiredException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = exception.message,
                        sessionExpired = true
                    )
                }
            } catch (exception: AppException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = if (preserveMessage) it.errorMessage ?: exception.message else exception.message
                    )
                }
            }
        }
    }

    private fun startAutoRefresh() {
        if (autoRefreshJob?.isActive == true) return

        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(5_000)

                val state = uiState.value
                if (state.game == null || state.isLoading || state.isSubmittingMove || state.isStartingGame) {
                    continue
                }

                refreshInternal(showLoading = false, preserveMessage = true)
            }
        }
    }

    private fun submitGameAction(
        successMessage: String? = null,
        action: suspend () -> ApiGame
    ) {
        if (uiState.value.isSubmittingMove) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmittingMove = true,
                    isRefreshing = false,
                    errorMessage = null,
                    sessionExpired = false
                )
            }
            try {
                val currentUser = repository.getCurrentUser()
                val game = action()
                _uiState.update {
                    it.copy(
                        currentUserId = currentUser.id,
                        game = game,
                        draftTiles = emptyMap(),
                        selectedRackIndices = emptySet(),
                        isLoading = false,
                        isRefreshing = false,
                        isSubmittingMove = false,
                        errorMessage = successMessage
                    )
                }
            } catch (exception: SessionExpiredException) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        isSubmittingMove = false,
                        errorMessage = exception.message,
                        sessionExpired = true
                    )
                }
            } catch (exception: AppException) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        isSubmittingMove = false,
                        errorMessage = exception.message
                    )
                }
            }
        }
    }

    override fun onCleared() {
        autoRefreshJob?.cancel()
        refreshJob?.cancel()
        super.onCleared()
    }

    private fun currentRack(state: GameDetailsUiState): List<String> = state.game
        ?.players
        ?.firstOrNull { it.id == state.currentUserId }
        ?.rack
        .orEmpty()

    private fun sanitizeSelectedRackIndices(
        game: ApiGame,
        currentUserId: Int?,
        selectedRackIndices: Set<Int>
    ): Set<Int> {
        val rackSize = game.players
            .firstOrNull { it.id == currentUserId }
            ?.rack
            ?.size
            ?: 0

        return selectedRackIndices.filterTo(linkedSetOf()) { it in 0 until rackSize }
    }

    companion object {
        fun factory(
            repository: LiterakiRepository,
            gameId: Int
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GameDetailsViewModel(repository, gameId) as T
            }
        }
    }
}


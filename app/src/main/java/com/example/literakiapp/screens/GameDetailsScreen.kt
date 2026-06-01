package com.example.literakiapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.literakiapp.data.model.ApiGame
import com.example.literakiapp.data.model.ApiMove
import com.example.literakiapp.data.model.ApiMoveTile
import com.example.literakiapp.data.model.ApiPlayer
import com.example.literakiapp.ui.viewmodel.BOARD_SIZE
import com.example.literakiapp.ui.viewmodel.GameDetailsUiState
import com.example.literakiapp.ui.viewmodel.boardCellKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailsScreen(
    uiState: GameDetailsUiState,
    onRefresh: () -> Unit,
    onStartGame: () -> Unit,
    onPassTurn: () -> Unit,
    onResignGame: () -> Unit,
    onRackTileSelectionChanged: (Int) -> Unit,
    onExchangeTiles: () -> Unit,
    onTileChanged: (Int, Int, String) -> Unit,
    onClearDraft: () -> Unit,
    onPlaceTiles: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.game?.let { "Gra #${it.id}" } ?: "Szczegóły gry") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                },
                actions = {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Odśwież",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null && uiState.game == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            uiState.game != null -> {
                val game = uiState.game
                val myPlayer = game.players.firstOrNull { it.id == uiState.currentUserId }
                val isMyTurn = game.currentTurnUserId == uiState.currentUserId
                val canStart = game.status == "waiting" && game.players.size == 2 && !uiState.isStartingGame
                val canPlay = game.status == "active" && isMyTurn && !uiState.isSubmittingMove

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { GameHeaderCard(game = game) }

                    if (uiState.errorMessage != null) {
                        item { ErrorMessageCard(message = uiState.errorMessage) }
                    }

                    if (!myPlayer?.rack.isNullOrEmpty()) {
                        item {
                            RackCard(
                                player = myPlayer!!,
                                selectedIndices = uiState.selectedRackIndices,
                                canSelect = canPlay,
                                onTileSelectionChanged = onRackTileSelectionChanged
                            )
                        }
                    }

                    if (game.status == "active") {
                        item {
                            TurnActionsCard(
                                isMyTurn = isMyTurn,
                                isSubmittingMove = uiState.isSubmittingMove,
                                placedTilesCount = uiState.draftTiles.size,
                                selectedRackTilesCount = uiState.selectedRackIndices.size,
                                onClearDraft = onClearDraft,
                                onPlaceTiles = onPlaceTiles,
                                onExchangeTiles = onExchangeTiles,
                                onPassTurn = onPassTurn,
                                onResignGame = onResignGame,
                                canPlay = canPlay
                            )
                        }
                    }

                    item {
                        BoardCard(
                            board = game.board,
                            draftTiles = uiState.draftTiles,
                            canEdit = canPlay,
                            onTileChanged = onTileChanged
                        )
                    }

                    item {
                        StatusCard(
                            game = game,
                            isStartingGame = uiState.isStartingGame,
                            currentUserId = uiState.currentUserId,
                            onStartGame = onStartGame,
                            canStart = canStart
                        )
                    }

                    item {
                        PlayersCard(
                            players = game.players,
                            currentUserId = uiState.currentUserId
                        )
                    }

                    item { MovesCard(moves = game.moves) }
                }
            }
        }
    }
}

@Composable
private fun ErrorMessageCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun GameHeaderCard(game: ApiGame) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = "Gra #${game.id}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Kod: ${game.id} • Status: ${game.status} • Ruchy: ${game.moves.size}",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun StatusCard(
    game: ApiGame,
    isStartingGame: Boolean,
    currentUserId: Int?,
    onStartGame: () -> Unit,
    canStart: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = when (game.status) {
                    "waiting" -> "Lobby"
                    "active" -> "Aktywna gra"
                    "finished" -> "Koniec gry"
                    else -> "Status"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            when {
                game.status == "waiting" && game.players.size < 2 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Czekamy na 2. gracza. Kod lobby: ${game.id}", fontSize = 13.sp)
                    }
                }

                game.status == "waiting" && game.players.size == 2 -> {
                    Text("2 graczy gotowych. Możesz wystartować grę.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onStartGame,
                        enabled = canStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isStartingGame) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Start gry")
                        }
                    }
                }

                game.status == "active" -> {
                    val currentPlayerName = game.players.firstOrNull { it.id == game.currentTurnUserId }?.username ?: "-"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Tura: ${if (game.currentTurnUserId == currentUserId) "Ty" else currentPlayerName}",
                            fontSize = 13.sp
                        )
                    }
                }

                game.status == "finished" -> {
                    val winnerName = game.players.firstOrNull { it.id == game.winnerId }?.username ?: "brak"
                    Text("Zwycięzca: $winnerName", fontSize = 13.sp)
                }

                else -> {
                    Text("Odśwież ekran, aby sprawdzić najnowszy stan.", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun PlayersCard(players: List<ApiPlayer>, currentUserId: Int?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Gracze",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            players.forEachIndexed { index, player ->
                Text(
                    text = "${index + 1}. ${if (player.id == currentUserId) "Ty" else player.username} • ${player.score} pkt • poz. ${player.position}",
                    fontSize = 13.sp
                )
                if (index != players.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun RackCard(
    player: ApiPlayer,
    selectedIndices: Set<Int>,
    canSelect: Boolean,
    onTileSelectionChanged: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Twój rack",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            val rack = player.rack.orEmpty()
            if (rack.isEmpty()) {
                Text(
                    text = "Brak liter",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rack.forEachIndexed { index, letter ->
                        FilterChip(
                            selected = index in selectedIndices,
                            onClick = { onTileSelectionChanged(index) },
                            enabled = canSelect,
                            label = {
                                Text(
                                    text = letter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (canSelect) {
                        "Zaznacz litery do wymiany. Wymiana daje nowe litery, ale kończy Twoją turę."
                    } else {
                        "Rack przeciwnika lub brak Twojej tury — zaznaczanie wyłączone."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TurnActionsCard(
    isMyTurn: Boolean,
    isSubmittingMove: Boolean,
    placedTilesCount: Int,
    selectedRackTilesCount: Int,
    onClearDraft: () -> Unit,
    onPlaceTiles: () -> Unit,
    onExchangeTiles: () -> Unit,
    onPassTurn: () -> Unit,
    onResignGame: () -> Unit,
    canPlay: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Ruch",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isMyTurn) {
                    "Jedna linia, bez luk. Pierwszy ruch przez środek; kolejne muszą łączyć się z planszą."
                } else {
                    "Czekasz na ruch przeciwnika."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = buildString {
                    append(if (placedTilesCount == 0) "Szkic pusty" else "Szkic: $placedTilesCount pól")
                    append(" • ")
                    append(if (selectedRackTilesCount == 0) "Wymiana: 0 liter" else "Wymiana: $selectedRackTilesCount liter")
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onPlaceTiles,
                modifier = Modifier.fillMaxWidth(),
                enabled = canPlay && placedTilesCount > 0
            ) {
                if (isSubmittingMove) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Zatwierdź ruch")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onClearDraft,
                    modifier = Modifier.weight(1f),
                    enabled = canPlay && placedTilesCount > 0
                ) {
                    Text("Wyczyść")
                }

                FilledTonalButton(
                    onClick = onExchangeTiles,
                    modifier = Modifier.weight(1f),
                    enabled = canPlay && selectedRackTilesCount > 0
                ) {
                    Text("Losuj nowe")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onPassTurn,
                    modifier = Modifier.weight(1f),
                    enabled = canPlay
                ) {
                    Text("Pomiń")
                }

                FilledTonalButton(
                    onClick = onResignGame,
                    modifier = Modifier.weight(1f),
                    enabled = canPlay,
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Poddaj")
                }
            }
        }
    }
}

@Composable
private fun BoardCard(
    board: Map<String, String>,
    draftTiles: Map<String, String>,
    canEdit: Boolean,
    onTileChanged: (Int, Int, String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Plansza",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (canEdit) {
                    "Plansza skaluje się do ekranu. Zajęte pola są zablokowane."
                } else if (board.isEmpty()) {
                    "Pusta plansza — środek oznaczono gwiazdką."
                } else {
                    "Aktualny stan planszy 15×15."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                for (y in 0 until BOARD_SIZE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        for (x in 0 until BOARD_SIZE) {
                            val key = boardCellKey(x, y)
                            BoardCell(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                x = x,
                                y = y,
                                placedLetter = board[key],
                                draftLetter = draftTiles[key],
                                isEditable = canEdit && !board.containsKey(key),
                                onLetterChange = { onTileChanged(x, y, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardCell(
    modifier: Modifier = Modifier,
    x: Int,
    y: Int,
    placedLetter: String?,
    draftLetter: String?,
    isEditable: Boolean,
    onLetterChange: (String) -> Unit
) {
    val isCenter = x == 7 && y == 7
    val hasPlacedLetter = !placedLetter.isNullOrBlank()
    val hasDraftLetter = !draftLetter.isNullOrBlank()

    val backgroundColor = when {
        hasPlacedLetter -> MaterialTheme.colorScheme.primaryContainer
        hasDraftLetter -> MaterialTheme.colorScheme.secondaryContainer
        isCenter -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        hasPlacedLetter -> MaterialTheme.colorScheme.onPrimaryContainer
        hasDraftLetter -> MaterialTheme.colorScheme.onSecondaryContainer
        isCenter -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = if (isEditable) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.extraSmall
            ),
        contentAlignment = Alignment.Center
    ) {
        val mainFontSize = when {
            maxWidth < 18.dp -> 7.sp
            maxWidth < 20.dp -> 8.sp
            maxWidth < 24.dp -> 9.sp
            maxWidth < 28.dp -> 10.sp
            else -> 12.sp
        }
        val helperFontSize = when {
            maxWidth < 18.dp -> 6.sp
            maxWidth < 24.dp -> 7.sp
            else -> 9.sp
        }

        when {
            hasPlacedLetter -> {
                Text(
                    text = placedLetter.orEmpty(),
                    color = contentColor,
                    fontSize = mainFontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            isEditable -> {
                BasicTextField(
                    value = draftLetter.orEmpty(),
                    onValueChange = onLetterChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = contentColor,
                        fontSize = mainFontSize,
                        fontWeight = if (hasDraftLetter) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Text
                    ),
                    cursorBrush = SolidColor(contentColor),
                    modifier = Modifier.fillMaxSize(),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!hasDraftLetter && isCenter) {
                                Text(
                                    text = "★",
                                    color = contentColor,
                                    fontSize = helperFontSize,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            else -> {
                Text(
                    text = if (isCenter) "★" else "",
                    color = contentColor,
                    fontSize = helperFontSize,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MovesCard(moves: List<ApiMove>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Ostatnie ruchy",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (moves.isEmpty()) {
                Text(
                    text = "Brak ruchów.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            } else {
                moves.takeLast(6).reversed().forEachIndexed { index, move ->
                    val tilesDescription = move.describeTiles()
                    Text(
                        text = "#${move.id} • ${move.moveType} • +${move.score} pkt$tilesDescription",
                        fontSize = 13.sp
                    )
                    if (index != moves.takeLast(6).reversed().lastIndex) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

private fun ApiMove.describeTiles(): String {
    if (tiles.isEmpty()) return ""

    return tiles.joinToString(prefix = " • ", separator = ", ") { tile ->
        tile.describe()
    }
}

private fun ApiMoveTile.describe(): String {
    return if (x != null && y != null) {
        "$letter($x,$y)"
    } else {
        letter
    }
}


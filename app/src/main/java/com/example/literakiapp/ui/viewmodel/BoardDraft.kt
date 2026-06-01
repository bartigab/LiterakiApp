package com.example.literakiapp.ui.viewmodel

import com.example.literakiapp.data.model.ApiTilePlacement

const val BOARD_SIZE = 15
const val BOARD_CENTER_INDEX = BOARD_SIZE / 2

data class PlacementValidationResult(
    val errorMessage: String? = null,
    val createdWords: List<String> = emptyList()
) {
    val isValid: Boolean get() = errorMessage == null
}

private data class BoardPosition(
    val x: Int,
    val y: Int
)

private enum class WordDirection {
    HORIZONTAL,
    VERTICAL;

    fun opposite(): WordDirection = when (this) {
        HORIZONTAL -> VERTICAL
        VERTICAL -> HORIZONTAL
    }
}

private data class CreatedWord(
    val word: String,
    val positions: List<BoardPosition>,
    val direction: WordDirection
)

fun boardCellKey(x: Int, y: Int): String = "$x,$y"

fun normalizeDraftLetter(value: String): String = value.uppercase().filter(Char::isLetter).take(1)

fun sanitizeDraftTiles(
    board: Map<String, String>,
    draftTiles: Map<String, String>
): Map<String, String> = draftTiles
    .mapNotNull { (key, value) ->
        val normalizedValue = normalizeDraftLetter(value)
        val coordinates = parseBoardCellKey(key) ?: return@mapNotNull null

        when {
            board.containsKey(key) -> null
            coordinates.first !in 0 until BOARD_SIZE -> null
            coordinates.second !in 0 until BOARD_SIZE -> null
            normalizedValue.isBlank() -> null
            else -> key to normalizedValue
        }
    }
    .toMap()

fun buildTilePlacements(draftTiles: Map<String, String>): List<ApiTilePlacement> = draftTiles
    .mapNotNull { (key, value) ->
        val normalizedValue = normalizeDraftLetter(value)
        val coordinates = parseBoardCellKey(key) ?: return@mapNotNull null

        if (normalizedValue.isBlank()) {
            null
        } else {
            ApiTilePlacement(
                letter = normalizedValue,
                x = coordinates.first,
                y = coordinates.second
            )
        }
    }
    .sortedWith(compareBy<ApiTilePlacement> { it.y }.thenBy { it.x })

fun rackContainsAllLetters(rack: List<String>, letters: Collection<String>): Boolean {
    val rackCounts = rack
        .map(::normalizeDraftLetter)
        .filter(String::isNotBlank)
        .groupingBy { it }
        .eachCount()
        .toMutableMap()

    letters
        .map(::normalizeDraftLetter)
        .filter(String::isNotBlank)
        .forEach { letter ->
            val remaining = rackCounts[letter] ?: return false
            if (remaining <= 0) {
                return false
            }
            rackCounts[letter] = remaining - 1
        }

    return true
}

fun validateTilePlacementMove(
    board: Map<String, String>,
    tiles: List<ApiTilePlacement>
): PlacementValidationResult {
    if (tiles.isEmpty()) {
        return PlacementValidationResult(errorMessage = "Wpisz co najmniej jedną literę bezpośrednio na planszy.")
    }

    val normalizedBoard = board.entries
        .mapNotNull { (key, value) ->
            val coordinates = parseBoardCellKey(key) ?: return@mapNotNull null
            val letter = normalizeDraftLetter(value)
            if (letter.isBlank()) {
                null
            } else {
                BoardPosition(coordinates.first, coordinates.second) to letter
            }
        }
        .toMap()

    val newTiles = linkedMapOf<BoardPosition, String>()
    for (tile in tiles) {
        val position = BoardPosition(tile.x, tile.y)
        val letter = normalizeDraftLetter(tile.letter)

        if (position.x !in 0 until BOARD_SIZE || position.y !in 0 until BOARD_SIZE) {
            return PlacementValidationResult(errorMessage = "Nie można położyć litery poza planszą 15×15.")
        }

        if (letter.isBlank()) {
            return PlacementValidationResult(errorMessage = "Każde dokładane pole musi zawierać pojedynczą literę.")
        }

        if (normalizedBoard.containsKey(position)) {
            return PlacementValidationResult(errorMessage = "Nie można nadpisywać istniejących liter na planszy.")
        }

        if (newTiles.put(position, letter) != null) {
            return PlacementValidationResult(errorMessage = "Nie można położyć dwóch liter na tym samym polu w jednym ruchu.")
        }
    }

    val positions = newTiles.keys.toList()
    val sameRow = positions.map(BoardPosition::y).distinct().size == 1
    val sameColumn = positions.map(BoardPosition::x).distinct().size == 1

    if (positions.size > 1 && !sameRow && !sameColumn) {
        return PlacementValidationResult(errorMessage = "Wszystkie litery dołożone w jednym ruchu muszą leżeć w jednej linii poziomej albo pionowej.")
    }

    val mainDirection = when {
        positions.size <= 1 -> null
        sameRow -> WordDirection.HORIZONTAL
        sameColumn -> WordDirection.VERTICAL
        else -> null
    }

    if (mainDirection != null && hasGapBetweenPlacedTiles(positions, normalizedBoard, mainDirection)) {
        return PlacementValidationResult(errorMessage = "Między literami dołożonymi w jednym ruchu nie może być pustych pól.")
    }

    if (normalizedBoard.isEmpty()) {
        val center = BoardPosition(BOARD_CENTER_INDEX, BOARD_CENTER_INDEX)
        if (center !in newTiles.keys) {
            return PlacementValidationResult(errorMessage = "Pierwsze słowo musi przechodzić przez pole startowe na środku planszy.")
        }
    } else {
        val connectsToBoard = newTiles.keys.any { position ->
            neighboringPositions(position).any(normalizedBoard::containsKey)
        }

        if (!connectsToBoard) {
            return PlacementValidationResult(errorMessage = "Nowy ruch musi łączyć się z literami, które już znajdują się na planszy.")
        }
    }

    val combinedBoard = normalizedBoard + newTiles
    if (!isConnectedStructure(combinedBoard.keys)) {
        return PlacementValidationResult(errorMessage = "Po ruchu wszystkie litery na planszy muszą tworzyć jedną wspólną, połączoną strukturę.")
    }

    val createdWords = collectCreatedWords(
        newTilePositions = newTiles.keys,
        combinedBoard = combinedBoard,
        mainDirection = mainDirection
    )

    if (createdWords.isEmpty()) {
        return PlacementValidationResult(errorMessage = "Ruch musi utworzyć co najmniej jedno nowe słowo zawierające nowo dołożoną literę.")
    }

    return PlacementValidationResult(createdWords = createdWords.map(CreatedWord::word))
}

private fun hasGapBetweenPlacedTiles(
    positions: List<BoardPosition>,
    board: Map<BoardPosition, String>,
    direction: WordDirection
): Boolean {
    return when (direction) {
        WordDirection.HORIZONTAL -> {
            val y = positions.first().y
            val minX = positions.minOf(BoardPosition::x)
            val maxX = positions.maxOf(BoardPosition::x)
            (minX..maxX).any { x -> BoardPosition(x, y) !in positions && BoardPosition(x, y) !in board }
        }

        WordDirection.VERTICAL -> {
            val x = positions.first().x
            val minY = positions.minOf(BoardPosition::y)
            val maxY = positions.maxOf(BoardPosition::y)
            (minY..maxY).any { y -> BoardPosition(x, y) !in positions && BoardPosition(x, y) !in board }
        }
    }
}

private fun collectCreatedWords(
    newTilePositions: Collection<BoardPosition>,
    combinedBoard: Map<BoardPosition, String>,
    mainDirection: WordDirection?
): List<CreatedWord> {
    val uniqueWords = linkedMapOf<String, CreatedWord>()
    val newTileSet = newTilePositions.toSet()

    fun register(position: BoardPosition, direction: WordDirection, allowSingleLetter: Boolean = false) {
        val word = buildWordAt(position, direction, combinedBoard) ?: return
        if (word.positions.none(newTileSet::contains)) return
        if (word.positions.size == 1 && !allowSingleLetter) return

        val key = buildString {
            append(direction.name)
            append(':')
            append(word.positions.joinToString(";") { "${it.x},${it.y}" })
        }
        uniqueWords.putIfAbsent(key, word)
    }

    if (mainDirection != null) {
        register(newTileSet.first(), mainDirection)
        val crossDirection = mainDirection.opposite()
        newTileSet.forEach { position -> register(position, crossDirection) }
    } else {
        val position = newTileSet.first()
        register(position, WordDirection.HORIZONTAL)
        register(position, WordDirection.VERTICAL)

        if (uniqueWords.isEmpty()) {
            register(position, WordDirection.HORIZONTAL, allowSingleLetter = true)
        }
    }

    return uniqueWords.values.toList()
}

private fun buildWordAt(
    origin: BoardPosition,
    direction: WordDirection,
    board: Map<BoardPosition, String>
): CreatedWord? {
    if (origin !in board) return null

    val stepX = if (direction == WordDirection.HORIZONTAL) 1 else 0
    val stepY = if (direction == WordDirection.VERTICAL) 1 else 0

    var start = origin
    while (BoardPosition(start.x - stepX, start.y - stepY) in board) {
        start = BoardPosition(start.x - stepX, start.y - stepY)
    }

    val positions = mutableListOf<BoardPosition>()
    var current = start
    while (current in board) {
        positions += current
        current = BoardPosition(current.x + stepX, current.y + stepY)
    }

    if (positions.isEmpty()) return null

    val word = positions.joinToString(separator = "") { position -> board.getValue(position) }
    return CreatedWord(word = word, positions = positions, direction = direction)
}

private fun isConnectedStructure(positions: Collection<BoardPosition>): Boolean {
    if (positions.isEmpty()) return true

    val allPositions = positions.toSet()
    val queue = ArrayDeque<BoardPosition>()
    val visited = mutableSetOf<BoardPosition>()
    queue.add(allPositions.first())

    while (queue.isNotEmpty()) {
        val position = queue.removeFirst()
        if (!visited.add(position)) continue

        neighboringPositions(position)
            .filter(allPositions::contains)
            .forEach(queue::addLast)
    }

    return visited.size == allPositions.size
}

private fun neighboringPositions(position: BoardPosition): List<BoardPosition> = listOf(
    BoardPosition(position.x - 1, position.y),
    BoardPosition(position.x + 1, position.y),
    BoardPosition(position.x, position.y - 1),
    BoardPosition(position.x, position.y + 1)
)


private fun parseBoardCellKey(key: String): Pair<Int, Int>? {
    val parts = key.split(',')
    if (parts.size != 2) return null

    val x = parts[0].toIntOrNull() ?: return null
    val y = parts[1].toIntOrNull() ?: return null
    return x to y
}

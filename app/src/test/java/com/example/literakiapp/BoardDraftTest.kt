package com.example.literakiapp

import com.example.literakiapp.data.model.ApiTilePlacement
import com.example.literakiapp.ui.viewmodel.boardCellKey
import com.example.literakiapp.ui.viewmodel.buildTilePlacements
import com.example.literakiapp.ui.viewmodel.normalizeDraftLetter
import com.example.literakiapp.ui.viewmodel.rackContainsAllLetters
import com.example.literakiapp.ui.viewmodel.sanitizeDraftTiles
import com.example.literakiapp.ui.viewmodel.validateTilePlacementMove
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardDraftTest {
    @Test
    fun normalizeDraftLetter_keepsOnlySingleUppercaseLetter() {
        assertEquals("Ą", normalizeDraftLetter("ąb1"))
        assertEquals("", normalizeDraftLetter("1?"))
    }

    @Test
    fun sanitizeDraftTiles_removesOccupiedOutOfRangeAndBlankEntries() {
        val sanitized = sanitizeDraftTiles(
            board = mapOf(boardCellKey(7, 7) to "K"),
            draftTiles = mapOf(
                boardCellKey(7, 7) to "a",
                boardCellKey(8, 7) to "o",
                boardCellKey(15, 7) to "t",
                "bad-key" to "z",
                boardCellKey(9, 7) to ""
            )
        )

        assertEquals(mapOf(boardCellKey(8, 7) to "O"), sanitized)
    }

    @Test
    fun buildTilePlacements_returnsSortedCoordinates() {
        val placements = buildTilePlacements(
            mapOf(
                boardCellKey(9, 7) to "t",
                boardCellKey(7, 7) to "k",
                boardCellKey(8, 7) to "ó"
            )
        )

        assertEquals(listOf("K", "Ó", "T"), placements.map { it.letter })
        assertEquals(listOf(7, 8, 9), placements.map { it.x })
        assertEquals(listOf(7, 7, 7), placements.map { it.y })
    }

    @Test
    fun rackContainsAllLetters_checksLetterCounts() {
        assertTrue(rackContainsAllLetters(listOf("K", "O", "T"), listOf("K", "O")))
        assertFalse(rackContainsAllLetters(listOf("K", "O", "T"), listOf("K", "K")))
    }

    @Test
    fun validateTilePlacementMove_acceptsFirstWordThroughCenter() {
        val result = validateTilePlacementMove(
            board = emptyMap(),
            tiles = word("KOT", startX = 6, startY = 7)
        )

        assertTrue(result.isValid)
        assertEquals(listOf("KOT"), result.createdWords)
    }

    @Test
    fun validateTilePlacementMove_rejectsFirstWordOutsideCenter() {
        val result = validateTilePlacementMove(
            board = emptyMap(),
            tiles = word("KOT", startX = 1, startY = 1)
        )

        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun validateTilePlacementMove_rejectsMoveWithGapBetweenPlacedTiles() {
        val result = validateTilePlacementMove(
            board = emptyMap(),
            tiles = listOf(
                ApiTilePlacement(letter = "K", x = 6, y = 7),
                ApiTilePlacement(letter = "T", x = 8, y = 7)
            )
        )

        assertFalse(result.isValid)
    }

    @Test
    fun validateTilePlacementMove_rejectsMoveThatDoesNotConnectToBoard() {
        val result = validateTilePlacementMove(
            board = mapOf(
                boardCellKey(6, 7) to "K",
                boardCellKey(7, 7) to "O",
                boardCellKey(8, 7) to "T"
            ),
            tiles = word("DOM", startX = 0, startY = 0)
        )

        assertFalse(result.isValid)
    }

    @Test
    fun validateTilePlacementMove_rejectsLettersInDifferentDirections() {
        val result = validateTilePlacementMove(
            board = mapOf(boardCellKey(6, 7) to "K", boardCellKey(7, 7) to "O", boardCellKey(8, 7) to "T"),
            tiles = listOf(
                ApiTilePlacement(letter = "M", x = 9, y = 7),
                ApiTilePlacement(letter = "A", x = 9, y = 8)
            )
        )

        assertFalse(result.isValid)
    }

    @Test
    fun validateTilePlacementMove_acceptsExtendingExistingWord() {
        val result = validateTilePlacementMove(
            board = mapOf(
                boardCellKey(7, 7) to "A",
                boardCellKey(8, 7) to "K"
            ),
            tiles = listOf(
                ApiTilePlacement(letter = "M", x = 6, y = 7)
            )
        )

        assertTrue(result.isValid)
        assertEquals(listOf("MAK"), result.createdWords)
    }

    @Test
    fun validateTilePlacementMove_acceptsPlacementRegardlessOfDictionaryBecauseApiValidatesWords() {
        val result = validateTilePlacementMove(
            board = mapOf(
                boardCellKey(7, 7) to "K"
            ),
            tiles = listOf(
                ApiTilePlacement(letter = "X", x = 8, y = 7)
            )
        )

        assertTrue(result.isValid)
        assertEquals(listOf("KX"), result.createdWords)
    }

    private fun word(
        value: String,
        startX: Int,
        startY: Int,
        horizontal: Boolean = true
    ): List<ApiTilePlacement> = value.mapIndexed { index, letter ->
        ApiTilePlacement(
            letter = letter.toString(),
            x = startX + if (horizontal) index else 0,
            y = startY + if (horizontal) 0 else index
        )
    }
}

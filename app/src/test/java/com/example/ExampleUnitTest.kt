package com.example

import com.example.data.model.Difficulty
import com.example.engine.SudokuEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testPuzzleGenerationAndSolutionValidity() {
    val puzzle = SudokuEngine.generatePuzzle(Difficulty.EASY, seed = 12345L)
    assertEquals(9, puzzle.initialBoard.size)
    assertEquals(9, puzzle.solution.size)

    // Check solution has no errors
    val errors = SudokuEngine.checkErrors(puzzle.solution)
    assertTrue("Solution should have zero errors", errors.isEmpty())
    assertTrue("Solution should be full", SudokuEngine.isBoardFull(puzzle.solution))

    // Check initial board has some empty cells
    var emptyCount = 0
    for (r in 0 until 9) {
      for (c in 0 until 9) {
        if (puzzle.initialBoard[r][c] == 0) emptyCount++
      }
    }
    assertTrue("Puzzle should have empty cells to solve", emptyCount > 0)
  }

  @Test
  fun testDailyChallengeDeterminism() {
    val daily1 = SudokuEngine.generateDailyPuzzle("2026-09-20")
    val daily2 = SudokuEngine.generateDailyPuzzle("2026-09-20")
    assertEquals(daily1.initialBoard, daily2.initialBoard)
    assertEquals(daily1.solution, daily2.solution)
  }

  @Test
  fun testHintSystem() {
    val puzzle = SudokuEngine.generatePuzzle(Difficulty.EASY, seed = 999L)
    val hint = SudokuEngine.findHint(puzzle.initialBoard, puzzle.solution)
    assertNotNull("Hint should be generated for incomplete board", hint)
    assertTrue(hint!!.row in 0..8)
    assertTrue(hint.col in 0..8)
    assertTrue(hint.solutionValue in 1..9)
    assertTrue(hint.explanation.isNotBlank())
  }

  @Test
  fun testPasswordHashingConsistency() {
    val hash1 = com.example.data.repository.SudokuRepository.hashPassword("sudoku123")
    val hash2 = com.example.data.repository.SudokuRepository.hashPassword("sudoku123")
    val hashWrong = com.example.data.repository.SudokuRepository.hashPassword("wrongpassword")

    assertEquals(hash1, hash2)
    assertNotEquals(hash1, hashWrong)
    assertEquals(64, hash1.length) // SHA-256 hex length
  }

  @Test
  fun testUsernameFormatting() {
    val raw = "  PlayerOne  "
    val clean = raw.trim().lowercase()
    assertEquals("playerone", clean)
  }

  @Test
  fun testGiveUpSolutionFillsBoardCorrectly() {
    val puzzle = SudokuEngine.generatePuzzle(Difficulty.MEDIUM, seed = 54321L)
    // Simulating give up filling board with solution
    val revealedBoard = puzzle.initialBoard.mapIndexed { r, row ->
      row.mapIndexed { c, value ->
        if (value != 0) value else puzzle.solution[r][c]
      }
    }
    assertEquals(puzzle.solution, revealedBoard)
    assertTrue(SudokuEngine.isBoardFull(revealedBoard))
    assertTrue(SudokuEngine.checkErrors(revealedBoard).isEmpty())
  }
}


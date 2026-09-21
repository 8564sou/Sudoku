package com.example.engine

import com.example.data.model.Difficulty
import com.example.data.model.HintInfo
import com.example.data.model.HintTechnique
import kotlin.random.Random

object SudokuEngine {

    const val GRID_SIZE = 9
    const val BOX_SIZE = 3

    data class GeneratedPuzzle(
        val initialBoard: List<List<Int>>,
        val solution: List<List<Int>>,
        val difficulty: Difficulty
    )

    fun generatePuzzle(difficulty: Difficulty, seed: Long? = null): GeneratedPuzzle {
        val random = if (seed != null) Random(seed) else Random.Default
        val solution = generateSolvedBoard(random)
        val puzzle = removeClues(solution, difficulty.cluesCount, random)
        return GeneratedPuzzle(
            initialBoard = puzzle,
            solution = solution,
            difficulty = difficulty
        )
    }

    fun generateDailyPuzzle(dateString: String): GeneratedPuzzle {
        // Deterministic seed based on date (e.g. "2026-09-20")
        val seed = dateString.hashCode().toLong()
        val random = Random(seed)
        // Rotate difficulty by day of week or day of month
        val dayNumber = dateString.takeLast(2).toIntOrNull() ?: 1
        val difficulty = when (dayNumber % 3) {
            0 -> Difficulty.EASY
            1 -> Difficulty.MEDIUM
            else -> Difficulty.HARD
        }
        return generatePuzzle(difficulty, seed)
    }

    private fun generateSolvedBoard(random: Random): List<List<Int>> {
        val board = Array(GRID_SIZE) { IntArray(GRID_SIZE) }
        fillDiagonalBoxes(board, random)
        solve(board, random)
        return board.map { it.toList() }
    }

    private fun fillDiagonalBoxes(board: Array<IntArray>, random: Random) {
        for (i in 0 until GRID_SIZE step BOX_SIZE) {
            fillBox(board, i, i, random)
        }
    }

    private fun fillBox(board: Array<IntArray>, startRow: Int, startCol: Int, random: Random) {
        val numbers = (1..GRID_SIZE).shuffled(random)
        var idx = 0
        for (r in 0 until BOX_SIZE) {
            for (c in 0 until BOX_SIZE) {
                board[startRow + r][startCol + c] = numbers[idx++]
            }
        }
    }

    fun solve(board: Array<IntArray>, random: Random = Random.Default): Boolean {
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (board[r][c] == 0) {
                    val candidates = (1..GRID_SIZE).shuffled(random)
                    for (num in candidates) {
                        if (isValidPlacement(board, r, c, num)) {
                            board[r][c] = num
                            if (solve(board, random)) return true
                            board[r][c] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    fun isValidPlacement(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (i in 0 until GRID_SIZE) {
            if (board[row][i] == num && i != col) return false
            if (board[i][col] == num && i != row) return false
        }
        val startRow = (row / BOX_SIZE) * BOX_SIZE
        val startCol = (col / BOX_SIZE) * BOX_SIZE
        for (r in 0 until BOX_SIZE) {
            for (c in 0 until BOX_SIZE) {
                val currRow = startRow + r
                val currCol = startCol + c
                if (currRow != row && currCol != col && board[currRow][currCol] == num) {
                    return false
                }
            }
        }
        return true
    }

    private fun removeClues(solution: List<List<Int>>, targetClues: Int, random: Random): List<List<Int>> {
        val board = solution.map { it.toIntArray() }.toTypedArray()
        val cells = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                cells.add(Pair(r, c))
            }
        }
        cells.shuffle(random)

        var cluesRemaining = GRID_SIZE * GRID_SIZE
        for ((r, c) in cells) {
            if (cluesRemaining <= targetClues) break
            val temp = board[r][c]
            board[r][c] = 0

            // Copy board and verify single unique solution
            val copy = board.map { it.clone() }.toTypedArray()
            val solutionsCount = countSolutions(copy, 0)
            if (solutionsCount != 1) {
                board[r][c] = temp // put back if removes uniqueness
            } else {
                cluesRemaining--
            }
        }
        return board.map { it.toList() }
    }

    private fun countSolutions(board: Array<IntArray>, count: Int): Int {
        var currentCount = count
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (board[r][c] == 0) {
                    for (num in 1..GRID_SIZE) {
                        if (isValidPlacement(board, r, c, num)) {
                            board[r][c] = num
                            currentCount = countSolutions(board, currentCount)
                            board[r][c] = 0
                            if (currentCount >= 2) return currentCount
                        }
                    }
                    return currentCount
                }
            }
        }
        return currentCount + 1
    }

    fun getCandidates(board: List<List<Int>>, row: Int, col: Int): Set<Int> {
        if (board[row][col] != 0) return emptySet()
        val candidates = (1..9).toMutableSet()
        for (i in 0 until GRID_SIZE) {
            candidates.remove(board[row][i])
            candidates.remove(board[i][col])
        }
        val startRow = (row / BOX_SIZE) * BOX_SIZE
        val startCol = (col / BOX_SIZE) * BOX_SIZE
        for (r in 0 until BOX_SIZE) {
            for (c in 0 until BOX_SIZE) {
                candidates.remove(board[startRow + r][startCol + c])
            }
        }
        return candidates
    }

    fun findHint(currentBoard: List<List<Int>>, solution: List<List<Int>>): HintInfo? {
        // 1. Check for any cells that currently violate Sudoku rules (error detection)
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                val currentVal = currentBoard[r][c]
                if (currentVal != 0 && currentVal != solution[r][c]) {
                    return HintInfo(
                        row = r,
                        col = c,
                        solutionValue = solution[r][c],
                        technique = HintTechnique.LOGICAL_DEDUCTION,
                        explanation = "Row ${r + 1}, Column ${c + 1} has a conflicting number. Correcting this cell will unlock other moves."
                    )
                }
            }
        }

        // 2. Look for Naked Singles (cell with exactly 1 possible candidate)
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (currentBoard[r][c] == 0) {
                    val candidates = getCandidates(currentBoard, r, c)
                    if (candidates.size == 1) {
                        val num = candidates.first()
                        return HintInfo(
                            row = r,
                            col = c,
                            solutionValue = num,
                            technique = HintTechnique.NAKED_SINGLE,
                            explanation = "Cell at Row ${r + 1}, Column ${c + 1} can only be $num because all other digits (1-9) already appear in its row, column, or 3x3 box."
                        )
                    }
                }
            }
        }

        // 3. Look for Hidden Singles in 3x3 Boxes
        for (boxRow in 0 until GRID_SIZE step BOX_SIZE) {
            for (boxCol in 0 until GRID_SIZE step BOX_SIZE) {
                for (num in 1..GRID_SIZE) {
                    val possiblePositions = mutableListOf<Pair<Int, Int>>()
                    for (r in 0 until BOX_SIZE) {
                        for (c in 0 until BOX_SIZE) {
                            val currR = boxRow + r
                            val currC = boxCol + c
                            if (currentBoard[currR][currC] == 0 && getCandidates(currentBoard, currR, currC).contains(num)) {
                                possiblePositions.add(Pair(currR, currC))
                            }
                        }
                    }
                    if (possiblePositions.size == 1) {
                        val (targetR, targetC) = possiblePositions.first()
                        val boxIndex = (boxRow / BOX_SIZE) * 3 + (boxCol / BOX_SIZE) + 1
                        return HintInfo(
                            row = targetR,
                            col = targetC,
                            solutionValue = num,
                            technique = HintTechnique.HIDDEN_SINGLE_BOX,
                            explanation = "In 3x3 block #$boxIndex, the number $num can only be placed in Row ${targetR + 1}, Column ${targetC + 1}."
                        )
                    }
                }
            }
        }

        // 4. Look for Hidden Singles in Rows
        for (r in 0 until GRID_SIZE) {
            for (num in 1..GRID_SIZE) {
                val possibleCols = mutableListOf<Int>()
                for (c in 0 until GRID_SIZE) {
                    if (currentBoard[r][c] == 0 && getCandidates(currentBoard, r, c).contains(num)) {
                        possibleCols.add(c)
                    }
                }
                if (possibleCols.size == 1) {
                    val targetC = possibleCols.first()
                    return HintInfo(
                        row = r,
                        col = targetC,
                        solutionValue = num,
                        technique = HintTechnique.HIDDEN_SINGLE_ROW,
                        explanation = "In Row ${r + 1}, the number $num has only one valid position: Column ${targetC + 1}."
                    )
                }
            }
        }

        // 5. Look for Hidden Singles in Columns
        for (c in 0 until GRID_SIZE) {
            for (num in 1..GRID_SIZE) {
                val possibleRows = mutableListOf<Int>()
                for (r in 0 until GRID_SIZE) {
                    if (currentBoard[r][c] == 0 && getCandidates(currentBoard, r, c).contains(num)) {
                        possibleRows.add(r)
                    }
                }
                if (possibleRows.size == 1) {
                    val targetR = possibleRows.first()
                    return HintInfo(
                        row = targetR,
                        col = c,
                        solutionValue = num,
                        technique = HintTechnique.HIDDEN_SINGLE_COL,
                        explanation = "In Column ${c + 1}, the number $num has only one valid position: Row ${targetR + 1}."
                    )
                }
            }
        }

        // 6. Fallback: Find the empty cell with the smallest candidate pool
        var bestCell: Pair<Int, Int>? = null
        var minCandidates = 10
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (currentBoard[r][c] == 0) {
                    val candidates = getCandidates(currentBoard, r, c)
                    if (candidates.isNotEmpty() && candidates.size < minCandidates) {
                        minCandidates = candidates.size
                        bestCell = Pair(r, c)
                    }
                }
            }
        }

        bestCell?.let { (r, c) ->
            val correctNum = solution[r][c]
            return HintInfo(
                row = r,
                col = c,
                solutionValue = correctNum,
                technique = HintTechnique.LOGICAL_DEDUCTION,
                explanation = "Examine Row ${r + 1}, Column ${c + 1}. Eliminating options from intersecting lines reveals that $correctNum is the correct entry."
            )
        }

        return null
    }

    fun isBoardFull(board: List<List<Int>>): Boolean {
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (board[r][c] == 0) return false
            }
        }
        return true
    }

    fun checkErrors(board: List<List<Int>>): List<Pair<Int, Int>> {
        val errors = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                val num = board[r][c]
                if (num != 0) {
                    var conflict = false
                    // Row
                    for (i in 0 until GRID_SIZE) {
                        if (i != c && board[r][i] == num) conflict = true
                    }
                    // Col
                    for (i in 0 until GRID_SIZE) {
                        if (i != r && board[i][c] == num) conflict = true
                    }
                    // Box
                    val startRow = (r / BOX_SIZE) * BOX_SIZE
                    val startCol = (c / BOX_SIZE) * BOX_SIZE
                    for (br in 0 until BOX_SIZE) {
                        for (bc in 0 until BOX_SIZE) {
                            val currR = startRow + br
                            val currC = startCol + bc
                            if ((currR != r || currC != c) && board[currR][currC] == num) {
                                conflict = true
                            }
                        }
                    }
                    if (conflict) errors.add(Pair(r, c))
                }
            }
        }
        return errors
    }
}

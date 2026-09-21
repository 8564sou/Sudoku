package com.example.data.model

enum class Difficulty(val title: String, val cluesCount: Int) {
    EASY("Easy", 38),
    MEDIUM("Medium", 32),
    HARD("Hard", 26),
    EXPERT("Expert", 22);

    companion object {
        fun fromString(value: String): Difficulty {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

data class SudokuCell(
    val row: Int,
    val col: Int,
    val value: Int = 0, // 0 means empty
    val initialValue: Int = 0, // >0 if clue given at start
    val notes: Set<Int> = emptySet(),
    val isError: Boolean = false,
    val isHinted: Boolean = false,
    val isRevealed: Boolean = false
) {
    val isFixed: Boolean get() = initialValue != 0
    val isEmpty: Boolean get() = value == 0
}

data class SudokuMove(
    val row: Int,
    val col: Int,
    val prevValue: Int,
    val newValue: Int,
    val prevNotes: Set<Int>,
    val newNotes: Set<Int>
)

enum class HintTechnique(val title: String) {
    NAKED_SINGLE("Naked Single"),
    HIDDEN_SINGLE_ROW("Row Hidden Single"),
    HIDDEN_SINGLE_COL("Column Hidden Single"),
    HIDDEN_SINGLE_BOX("Block Hidden Single"),
    LOGICAL_DEDUCTION("Logical Deduction")
}

data class HintInfo(
    val row: Int,
    val col: Int,
    val solutionValue: Int,
    val technique: HintTechnique,
    val explanation: String,
    val isRevealed: Boolean = false
)

data class DifficultyStats(
    val difficulty: Difficulty,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val bestTimeSeconds: Long = 0L,
    val totalTimeSeconds: Long = 0L
) {
    val winRate: Int get() = if (gamesPlayed > 0) ((gamesWon.toDouble() / gamesPlayed) * 100).toInt() else 0
    val averageTimeSeconds: Long get() = if (gamesWon > 0) totalTimeSeconds / gamesWon else 0L
}

data class OverallStats(
    val statsByDifficulty: Map<Difficulty, DifficultyStats> = emptyMap(),
    val currentDailyStreak: Int = 0,
    val bestDailyStreak: Int = 0,
    val totalDailyCompleted: Int = 0
)

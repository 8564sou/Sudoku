package com.example.data.repository

import com.example.data.local.dao.SudokuDao
import com.example.data.local.entity.DailyChallengeEntity
import com.example.data.local.entity.GameStatEntity
import com.example.data.local.entity.SavedGameEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.Difficulty
import com.example.data.model.DifficultyStats
import com.example.data.model.OverallStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

class SudokuRepository(private val sudokuDao: SudokuDao) {

    val allUsers: Flow<List<UserEntity>> = sudokuDao.getAllUsers()
    val deviceUser: Flow<UserEntity?> = sudokuDao.getDeviceUserFlow()

    suspend fun getDeviceUser(): UserEntity? = sudokuDao.getDeviceUser()

    companion object {
        fun hashPassword(password: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            return hash.joinToString("") { "%02x".format(it) }
        }
    }

    suspend fun getUserByUsername(username: String): UserEntity? {
        return sudokuDao.getUserByUsername(username.trim().lowercase())
    }

    suspend fun registerUser(
        username: String,
        displayName: String,
        password: String,
        avatarEmoji: String
    ): Result<UserEntity> {
        val cleanUsername = username.trim().lowercase()
        if (cleanUsername.length < 3) {
            return Result.failure(IllegalArgumentException("Username must be at least 3 characters"))
        }
        if (password.length < 4) {
            return Result.failure(IllegalArgumentException("Password must be at least 4 characters"))
        }

        val newUser = UserEntity(
            username = cleanUsername,
            displayName = displayName.ifBlank { cleanUsername.replaceFirstChar { it.uppercase() } },
            passwordHash = hashPassword(password),
            avatarEmoji = avatarEmoji,
            themePreference = "SYSTEM",
            createdAt = System.currentTimeMillis()
        )
        // 1 device user for this device
        sudokuDao.insertOrUpdateUser(newUser)
        return Result.success(newUser)
    }

    suspend fun loginUser(username: String, password: String): Result<UserEntity> {
        val cleanUsername = username.trim().lowercase()
        val user = sudokuDao.getUserByUsername(cleanUsername)
            ?: return Result.failure(IllegalArgumentException("Account '$cleanUsername' not found on this device."))

        val inputHash = hashPassword(password)
        if (user.passwordHash != inputHash) {
            return Result.failure(IllegalArgumentException("Incorrect password."))
        }
        return Result.success(user)
    }

    suspend fun updateUserTheme(username: String, theme: String) {
        sudokuDao.updateUserTheme(username.lowercase(), theme)
    }

    suspend fun getOrCreateDeviceUser(): UserEntity {
        var user = sudokuDao.getDeviceUser()
        if (user == null) {
            val defaultUser = UserEntity(
                username = "player",
                displayName = "Player",
                passwordHash = hashPassword("1234"),
                avatarEmoji = "🧩",
                themePreference = "SYSTEM",
                createdAt = System.currentTimeMillis()
            )
            sudokuDao.insertOrUpdateUser(defaultUser)
            user = defaultUser
        }
        return user
    }

    suspend fun ensureDefaultUsers() {
        getOrCreateDeviceUser()
    }

    fun getUserOverallStats(userId: String): Flow<OverallStats> {
        return combine(
            sudokuDao.getAllGameStats(userId),
            sudokuDao.getAllDailyChallenges(userId)
        ) { statsList, dailies ->
            val statsMap = mutableMapOf<Difficulty, DifficultyStats>()
            Difficulty.entries.forEach { diff ->
                val entity = statsList.find { it.difficulty.equals(diff.name, ignoreCase = true) }
                statsMap[diff] = DifficultyStats(
                    difficulty = diff,
                    gamesPlayed = entity?.gamesPlayed ?: 0,
                    gamesWon = entity?.gamesWon ?: 0,
                    bestTimeSeconds = entity?.bestTimeSeconds ?: 0L,
                    totalTimeSeconds = entity?.totalTimeSeconds ?: 0L
                )
            }

            val completedDates = dailies.filter { it.completed }
                .mapNotNull {
                    try {
                        LocalDate.parse(it.date, DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        null
                    }
                }
                .toSet()

            val streaks = calculateStreaks(completedDates)

            OverallStats(
                statsByDifficulty = statsMap,
                currentDailyStreak = streaks.first,
                bestDailyStreak = streaks.second,
                totalDailyCompleted = completedDates.size
            )
        }
    }

    fun getUserDailyChallenges(userId: String): Flow<List<DailyChallengeEntity>> =
        sudokuDao.getAllDailyChallenges(userId)

    fun getSavedGame(userId: String): Flow<SavedGameEntity?> =
        sudokuDao.getSavedGame(userId)

    suspend fun recordGameStarted(userId: String, difficulty: Difficulty) {
        val current = sudokuDao.getGameStat(userId, difficulty.name)
            ?: GameStatEntity(userId = userId, difficulty = difficulty.name)
        sudokuDao.insertOrUpdateGameStat(
            current.copy(gamesPlayed = current.gamesPlayed + 1)
        )
    }

    suspend fun recordGameWon(userId: String, difficulty: Difficulty, timeSeconds: Long) {
        val current = sudokuDao.getGameStat(userId, difficulty.name)
            ?: GameStatEntity(userId = userId, difficulty = difficulty.name)
        val newBest = if (current.bestTimeSeconds == 0L || timeSeconds < current.bestTimeSeconds) {
            timeSeconds
        } else {
            current.bestTimeSeconds
        }
        sudokuDao.insertOrUpdateGameStat(
            current.copy(
                gamesWon = current.gamesWon + 1,
                bestTimeSeconds = newBest,
                totalTimeSeconds = current.totalTimeSeconds + timeSeconds
            )
        )
    }

    suspend fun recordDailyCompleted(
        userId: String,
        date: String,
        timeSeconds: Long,
        hintsUsed: Int,
        difficulty: String
    ) {
        sudokuDao.insertOrUpdateDailyChallenge(
            DailyChallengeEntity(
                userId = userId,
                date = date,
                completed = true,
                timeSeconds = timeSeconds,
                hintsUsed = hintsUsed,
                difficulty = difficulty,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveGame(saved: SavedGameEntity) {
        sudokuDao.insertOrUpdateSavedGame(saved)
    }

    suspend fun clearSavedGame(userId: String) {
        sudokuDao.clearSavedGame(userId)
    }

    private fun calculateStreaks(completedDates: Set<LocalDate>): Pair<Int, Int> {
        if (completedDates.isEmpty()) return Pair(0, 0)

        val sortedDates = completedDates.sorted()
        var bestStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null

        for (date in sortedDates) {
            if (prevDate == null || date == prevDate.plusDays(1)) {
                tempStreak++
            } else if (date != prevDate) {
                tempStreak = 1
            }
            bestStreak = max(bestStreak, tempStreak)
            prevDate = date
        }

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        var currentStreak = 0
        var checkDate = if (completedDates.contains(today)) today else if (completedDates.contains(yesterday)) yesterday else null

        if (checkDate != null) {
            while (completedDates.contains(checkDate)) {
                currentStreak++
                checkDate = checkDate?.minusDays(1)
            }
        }

        return Pair(currentStreak, max(bestStreak, currentStreak))
    }
}

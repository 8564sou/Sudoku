package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.DailyChallengeEntity
import com.example.data.local.entity.GameStatEntity
import com.example.data.local.entity.SavedGameEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SudokuDao {
    // User Management (1 device user per device)
    @Query("SELECT * FROM users ORDER BY createdAt DESC LIMIT 1")
    fun getDeviceUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY createdAt DESC LIMIT 1")
    suspend fun getDeviceUser(): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity)

    @Query("UPDATE users SET themePreference = :theme WHERE username = :username")
    suspend fun updateUserTheme(username: String, theme: String)

    @Query("DELETE FROM users WHERE username = :username")
    suspend fun deleteUser(username: String)

    @Query("DELETE FROM users")
    suspend fun clearAllUsers()

    // Game stats scoped to user
    @Query("SELECT * FROM game_stats WHERE userId = :userId")
    fun getAllGameStats(userId: String): Flow<List<GameStatEntity>>

    @Query("SELECT * FROM game_stats WHERE userId = :userId AND difficulty = :difficulty LIMIT 1")
    suspend fun getGameStat(userId: String, difficulty: String): GameStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGameStat(stat: GameStatEntity)

    // Daily challenges scoped to user
    @Query("SELECT * FROM daily_challenges WHERE userId = :userId ORDER BY date DESC")
    fun getAllDailyChallenges(userId: String): Flow<List<DailyChallengeEntity>>

    @Query("SELECT * FROM daily_challenges WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getDailyChallenge(userId: String, date: String): DailyChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyChallenge(daily: DailyChallengeEntity)

    // Saved game scoped to user
    @Query("SELECT * FROM saved_game WHERE userId = :userId AND id = 1 LIMIT 1")
    fun getSavedGame(userId: String): Flow<SavedGameEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSavedGame(saved: SavedGameEntity)

    @Query("DELETE FROM saved_game WHERE userId = :userId AND id = 1")
    suspend fun clearSavedGame(userId: String)
}

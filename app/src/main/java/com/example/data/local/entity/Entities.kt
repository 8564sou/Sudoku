package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val username: String, // unique lowercase identifier
    val displayName: String,
    val passwordHash: String, // stored password hash or passcode
    val avatarEmoji: String = "🧩",
    val themePreference: String = "SYSTEM", // "LIGHT", "DARK", "SYSTEM"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "game_stats",
    primaryKeys = ["userId", "difficulty"]
)
data class GameStatEntity(
    val userId: String,
    val difficulty: String, // "EASY", "MEDIUM", "HARD", "EXPERT"
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val bestTimeSeconds: Long = 0L,
    val totalTimeSeconds: Long = 0L
)

@Entity(
    tableName = "daily_challenges",
    primaryKeys = ["userId", "date"]
)
data class DailyChallengeEntity(
    val userId: String,
    val date: String, // "YYYY-MM-DD"
    val completed: Boolean = false,
    val timeSeconds: Long = 0L,
    val difficulty: String = "MEDIUM",
    val hintsUsed: Int = 0,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "saved_game",
    primaryKeys = ["userId", "id"]
)
data class SavedGameEntity(
    val userId: String,
    val id: Int = 1,
    val difficulty: String,
    val initialBoard: String, // 81 chars digits
    val currentBoard: String, // 81 chars digits
    val solution: String,     // 81 chars digits
    val notesJson: String,    // notes serialization
    val elapsedSeconds: Long,
    val hintsUsed: Int,
    val isDaily: Boolean,
    val dailyDate: String?,
    val historyJson: String,  // undo stack JSON
    val redoJson: String      // redo stack JSON
)

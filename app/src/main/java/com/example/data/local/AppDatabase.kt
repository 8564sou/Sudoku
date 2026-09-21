package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.SudokuDao
import com.example.data.local.entity.DailyChallengeEntity
import com.example.data.local.entity.GameStatEntity
import com.example.data.local.entity.SavedGameEntity
import com.example.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        GameStatEntity::class,
        DailyChallengeEntity::class,
        SavedGameEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sudokuDao(): SudokuDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sudoku_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.paperapps.paperscores.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FollowedTeamEntity::class, FollowedTournamentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SoccerDatabase : RoomDatabase() {
    abstract fun soccerDao(): SoccerDao

    companion object {
        @Volatile
        private var INSTANCE: SoccerDatabase? = null

        fun getDatabase(context: Context): SoccerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SoccerDatabase::class.java,
                    "soccer_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

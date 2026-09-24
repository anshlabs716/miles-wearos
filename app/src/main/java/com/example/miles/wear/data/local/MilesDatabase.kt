package com.example.miles.wear.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.miles.wear.data.local.dao.DayStatsDao
import com.example.miles.wear.data.local.dao.QueueDao
import com.example.miles.wear.data.local.dao.SavedPinDao
import com.example.miles.wear.data.local.dao.WorkoutSessionDao
import com.example.miles.wear.data.local.entity.DayStatsEntity
import com.example.miles.wear.data.local.entity.QueueItemEntity
import com.example.miles.wear.data.local.entity.SavedPinEntity
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity

@Database(
    entities = [QueueItemEntity::class, WorkoutSessionEntity::class, SavedPinEntity::class, DayStatsEntity::class],
    version = 3,
    exportSchema = false
)
abstract class MilesDatabase : RoomDatabase() {
    abstract fun queueDao(): QueueDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun savedPinDao(): SavedPinDao
    abstract fun dayStatsDao(): DayStatsDao

    companion object {
        @Volatile
        private var INSTANCE: MilesDatabase? = null

        fun getInstance(context: Context): MilesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MilesDatabase::class.java,
                    "miles_wear_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example.miles.wear.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.miles.wear.data.local.dao.QueueDao
import com.example.miles.wear.data.local.dao.WorkoutSessionDao
import com.example.miles.wear.data.local.entity.QueueItemEntity
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity

@Database(
    entities = [QueueItemEntity::class, WorkoutSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MilesDatabase : RoomDatabase() {
    abstract fun queueDao(): QueueDao
    abstract fun workoutSessionDao(): WorkoutSessionDao

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

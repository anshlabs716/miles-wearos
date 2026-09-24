package com.example.miles.wear.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.miles.wear.data.local.dao.DayStatsDao
import com.example.miles.wear.data.local.dao.PetDao
import com.example.miles.wear.data.local.dao.QueueDao
import com.example.miles.wear.data.local.dao.SavedPinDao
import com.example.miles.wear.data.local.dao.WorkoutSessionDao
import com.example.miles.wear.data.local.entity.DayStatsEntity
import com.example.miles.wear.data.local.entity.PetEntity
import com.example.miles.wear.data.local.entity.QueueItemEntity
import com.example.miles.wear.data.local.entity.SavedPinEntity
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity

@Database(
    entities = [QueueItemEntity::class, WorkoutSessionEntity::class, SavedPinEntity::class, DayStatsEntity::class, PetEntity::class],
    version = 4,
    exportSchema = false
)
abstract class MilesDatabase : RoomDatabase() {
    abstract fun queueDao(): QueueDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun savedPinDao(): SavedPinDao
    abstract fun dayStatsDao(): DayStatsDao
    abstract fun petDao(): PetDao

    companion object {
        @Volatile
        private var INSTANCE: MilesDatabase? = null

        /** v3 -> v4 adds the fitness pet table. */
        val MIGRATION_3_4: androidx.room.migration.Migration = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pet` (" +
                        "`id` INTEGER NOT NULL, " +
                        "`petType` TEXT NOT NULL, " +
                        "`petName` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        fun getInstance(context: Context): MilesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MilesDatabase::class.java,
                    "miles_wear_database"
                ).addMigrations(MIGRATION_3_4).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

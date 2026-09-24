package com.example.miles.wear.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.miles.wear.data.local.dao.DayStatsDao
import com.example.miles.wear.data.local.dao.PetDao
import com.example.miles.wear.data.local.dao.QueueDao
import com.example.miles.wear.data.local.dao.SavedPinDao
import com.example.miles.wear.data.local.dao.SavedRouteDao
import com.example.miles.wear.data.local.dao.TrainingProgressDao
import com.example.miles.wear.data.local.dao.WorkoutSessionDao
import com.example.miles.wear.data.local.entity.DayStatsEntity
import com.example.miles.wear.data.local.entity.PetEntity
import com.example.miles.wear.data.local.entity.QueueItemEntity
import com.example.miles.wear.data.local.entity.SavedPinEntity
import com.example.miles.wear.data.local.entity.SavedRouteEntity
import com.example.miles.wear.data.local.entity.TrainingProgressEntity
import com.example.miles.wear.data.local.entity.WorkoutSessionEntity

@Database(
    entities = [QueueItemEntity::class, WorkoutSessionEntity::class, SavedPinEntity::class, DayStatsEntity::class, PetEntity::class, SavedRouteEntity::class, TrainingProgressEntity::class],
    version = 5,
    exportSchema = false
)
abstract class MilesDatabase : RoomDatabase() {
    abstract fun queueDao(): QueueDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun savedPinDao(): SavedPinDao
    abstract fun dayStatsDao(): DayStatsDao
    abstract fun petDao(): PetDao
    abstract fun savedRouteDao(): SavedRouteDao
    abstract fun trainingProgressDao(): TrainingProgressDao

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

        /** v4 -> v5 adds saved routes + training plan progress. */
        val MIGRATION_4_5: androidx.room.migration.Migration = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `saved_routes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`pointsJson` TEXT NOT NULL, " +
                        "`distanceMeters` REAL NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `training_progress` (" +
                        "`id` INTEGER NOT NULL, " +
                        "`planKey` TEXT NOT NULL, " +
                        "`startedEpochDay` INTEGER NOT NULL, " +
                        "`completedJson` TEXT NOT NULL, " +
                        "`finishedAtEpochDay` INTEGER NOT NULL, " +
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
                ).addMigrations(MIGRATION_3_4, MIGRATION_4_5).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

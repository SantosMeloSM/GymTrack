package com.gymtrack.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExerciseEntity::class, RoutineEntity::class, RoutineExerciseEntity::class, RoutineVersionEntity::class,
        SessionEntity::class, SessionExerciseEntity::class, SetEntity::class,
        BodyMeasurementEntity::class, ProgressPhotoEntity::class, GoalEntity::class, ReminderEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun sessionDao(): SessionDao
    abstract fun bodyDao(): BodyDao
    abstract fun goalDao(): GoalDao
    abstract fun reminderDao(): ReminderDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val NAME = "gymtrack.db"
        fun build(context: Context): GymDatabase =
            Room.databaseBuilder(context, GymDatabase::class.java, NAME)
                // WAL + synchronous writes: every set is persisted as soon as it is logged.
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}

/** v1 -> v2: exercise images and common mistakes. Purely additive: no data is touched. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN mistakes TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE exercises ADD COLUMN imageStart TEXT")
        db.execSQL("ALTER TABLE exercises ADD COLUMN imageEnd TEXT")
    }
}

package com.gymtrack.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ExerciseEntity::class, RoutineEntity::class, RoutineExerciseEntity::class, RoutineVersionEntity::class,
        SessionEntity::class, SessionExerciseEntity::class, SetEntity::class,
        BodyMeasurementEntity::class, ProgressPhotoEntity::class, GoalEntity::class, ReminderEntity::class,
    ],
    version = 1,
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
                .build()
    }
}

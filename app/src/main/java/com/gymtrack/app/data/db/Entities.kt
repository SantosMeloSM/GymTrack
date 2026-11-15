package com.gymtrack.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "exercises", indices = [Index("name"), Index(value = ["seedKey"], unique = true)])
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val primaryMuscle: String,
    /** Comma separated MuscleGroup names. */
    val secondaryMuscles: String = "",
    val equipment: String,
    val description: String = "",
    val tips: String = "",
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val defaultRestSec: Int = 90,
    val archived: Boolean = false,
    /** Stable key for seeded exercises; null for custom ones. */
    val seedKey: String? = null,
)

@Serializable
@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String = "",
    /** Grouping label such as "PPL" or "ABC". */
    val program: String = "",
    val position: Int = 0,
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
)

@Serializable
@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(entity = RoutineEntity::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("routineId"), Index("exerciseId")],
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val position: Int,
    val sets: Int = 3,
    val repsMin: Int = 8,
    val repsMax: Int = 12,
    val plannedWeight: Double? = null,
    val restSec: Int = 90,
    val warmupSets: Int = 0,
    val goal: String = "",
    val notes: String = "",
)

@Serializable
@Entity(
    tableName = "routine_versions",
    foreignKeys = [ForeignKey(entity = RoutineEntity::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("routineId")],
)
data class RoutineVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val createdAt: Long,
    val label: String,
    /** JSON snapshot of the routine exercises. */
    val snapshot: String,
)

object SessionStatus {
    const val ACTIVE = "ACTIVE"
    const val FINISHED = "FINISHED"
}

@Serializable
@Entity(tableName = "sessions", indices = [Index("startedAt"), Index("status")])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long? = null,
    val name: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val pausedAt: Long? = null,
    val pausedAccumMs: Long = 0,
    val status: String = SessionStatus.ACTIVE,
    val notes: String = "",
    val restEndsAt: Long? = null,
    val restTotalSec: Int = 0,
    val restExerciseName: String? = null,
) {
    fun elapsedMs(now: Long): Long {
        val end = endedAt ?: pausedAt ?: now
        return (end - startedAt - pausedAccumMs).coerceAtLeast(0)
    }
}

@Serializable
@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val position: Int,
    val restSec: Int = 90,
    val notes: String = "",
    val repsMin: Int? = null,
    val repsMax: Int? = null,
)

@Serializable
@Entity(
    tableName = "sets",
    foreignKeys = [ForeignKey(entity = SessionExerciseEntity::class, parentColumns = ["id"], childColumns = ["sessionExerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionExerciseId")],
)
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val position: Int,
    val weight: Double? = null,
    val reps: Int? = null,
    val isWarmup: Boolean = false,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val rpe: Double? = null,
    val rir: Int? = null,
)

@Serializable
@Entity(tableName = "body_measurements", indices = [Index("date")])
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val weight: Double? = null,
    val height: Double? = null,
    val waist: Double? = null,
    val chest: Double? = null,
    val armLeft: Double? = null,
    val armRight: Double? = null,
    val thighLeft: Double? = null,
    val thighRight: Double? = null,
    val hips: Double? = null,
    val calves: Double? = null,
    val shoulders: Double? = null,
    val neck: Double? = null,
    val forearm: Double? = null,
    /** Free "name=value" lines for any other measurement, in cm. */
    val other: String = "",
    val notes: String = "",
)

@Serializable
@Entity(tableName = "progress_photos", indices = [Index("date")])
data class ProgressPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    /** File name inside the app's private photos directory. */
    val fileName: String,
    val pose: String = "",
    val notes: String = "",
)

@Serializable
@Entity(
    tableName = "goals",
    foreignKeys = [ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("exerciseId")],
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val targetWeight: Double,
    val targetReps: Int,
    val createdAt: Long,
    val deadline: Long? = null,
    val achievedAt: Long? = null,
    val notes: String = "",
)

@Serializable
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ISO day of week 1 = Monday .. 7 = Sunday */
    val dayOfWeek: Int,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val label: String = "",
)

/** Flat row used for history/statistics queries. */
data class SetRow(
    val setId: Long,
    val sessionId: Long,
    val sessionName: String,
    val startedAt: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val primaryMuscle: String,
    val weight: Double?,
    val reps: Int?,
    val isWarmup: Boolean,
    val completed: Boolean,
    val rpe: Double?,
    val rir: Int?,
    @ColumnInfo(name = "setPosition") val setPosition: Int,
    val exercisePosition: Int,
)

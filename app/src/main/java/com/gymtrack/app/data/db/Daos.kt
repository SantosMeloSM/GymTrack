package com.gymtrack.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE archived = 0 ORDER BY isFavorite DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises")
    fun observeAllIncludingArchived(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observe(id: Long): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun get(id: Long): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT seedKey FROM exercises WHERE seedKey IS NOT NULL")
    suspend fun seedKeys(): List<String>

    @Query("SELECT * FROM exercises WHERE seedKey = :key LIMIT 1")
    suspend fun bySeedKey(key: String): ExerciseEntity?

    @Insert suspend fun insert(e: ExerciseEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(e: List<ExerciseEntity>)
    @Update suspend fun update(e: ExerciseEntity)

    @Query("UPDATE exercises SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("SELECT COUNT(*) FROM session_exercises WHERE exerciseId = :id")
    suspend fun usageCount(id: Long): Int

    @Query("SELECT COUNT(*) FROM routine_exercises WHERE exerciseId = :id")
    suspend fun routineUsageCount(id: Long): Int

    @Query("SELECT COUNT(*) FROM goals WHERE exerciseId = :id")
    suspend fun goalUsageCount(id: Long): Int

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun delete(id: Long)
}

data class RoutineSummary(
    val id: Long,
    val name: String,
    val notes: String,
    val program: String,
    val position: Int,
    val colorIndex: Int,
    val exerciseCount: Int,
    val totalSets: Int,
    val lastPerformed: Long?,
)

@Dao
interface RoutineDao {
    @Query(
        """SELECT r.id, r.name, r.notes, r.program, r.position, r.colorIndex,
           (SELECT COUNT(*) FROM routine_exercises re WHERE re.routineId = r.id) AS exerciseCount,
           (SELECT COALESCE(SUM(re.sets),0) FROM routine_exercises re WHERE re.routineId = r.id) AS totalSets,
           (SELECT MAX(s.startedAt) FROM sessions s WHERE s.routineId = r.id AND s.status = 'FINISHED') AS lastPerformed
           FROM routines r WHERE r.archived = 0 ORDER BY r.program COLLATE NOCASE, r.position, r.id"""
    )
    fun observeSummaries(): Flow<List<RoutineSummary>>

    @Query("SELECT * FROM routines") suspend fun getAll(): List<RoutineEntity>
    @Query("SELECT * FROM routines WHERE id = :id") fun observe(id: Long): Flow<RoutineEntity?>
    @Query("SELECT * FROM routines WHERE id = :id") suspend fun get(id: Long): RoutineEntity?
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM routines") suspend fun nextPosition(): Int
    @Insert suspend fun insert(r: RoutineEntity): Long
    @Update suspend fun update(r: RoutineEntity)
    @Query("DELETE FROM routines WHERE id = :id") suspend fun delete(id: Long)

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY position")
    fun observeExercises(routineId: Long): Flow<List<RoutineExerciseEntity>>
    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY position")
    suspend fun getExercises(routineId: Long): List<RoutineExerciseEntity>
    @Query("SELECT * FROM routine_exercises") suspend fun getAllExercises(): List<RoutineExerciseEntity>
    @Insert suspend fun insertExercise(e: RoutineExerciseEntity): Long
    @Insert suspend fun insertExercises(e: List<RoutineExerciseEntity>)
    @Update suspend fun updateExercise(e: RoutineExerciseEntity)
    @Update suspend fun updateExercises(e: List<RoutineExerciseEntity>)
    @Query("DELETE FROM routine_exercises WHERE id = :id") suspend fun deleteExercise(id: Long)
    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId") suspend fun deleteExercisesOf(routineId: Long)

    @Query("SELECT * FROM routine_versions WHERE routineId = :routineId ORDER BY createdAt DESC")
    fun observeVersions(routineId: Long): Flow<List<RoutineVersionEntity>>
    @Query("SELECT * FROM routine_versions") suspend fun getAllVersions(): List<RoutineVersionEntity>
    @Insert suspend fun insertVersion(v: RoutineVersionEntity): Long
    @Query("DELETE FROM routine_versions WHERE id = :id") suspend fun deleteVersion(id: Long)
}

data class SessionSummary(
    val id: Long,
    val name: String,
    val routineId: Long?,
    val startedAt: Long,
    val endedAt: Long?,
    val pausedAccumMs: Long,
    val exerciseCount: Int,
    val setCount: Int,
    val volume: Double,
)

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<SessionEntity?>
    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActive(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :id") fun observe(id: Long): Flow<SessionEntity?>
    @Query("SELECT * FROM sessions WHERE id = :id") suspend fun get(id: Long): SessionEntity?
    @Query("SELECT * FROM sessions") suspend fun getAll(): List<SessionEntity>

    @Query(
        """SELECT s.id, s.name, s.routineId, s.startedAt, s.endedAt, s.pausedAccumMs,
           (SELECT COUNT(*) FROM session_exercises se WHERE se.sessionId = s.id) AS exerciseCount,
           (SELECT COUNT(*) FROM sets st JOIN session_exercises se ON st.sessionExerciseId = se.id
                WHERE se.sessionId = s.id AND st.completed = 1 AND st.isWarmup = 0) AS setCount,
           (SELECT COALESCE(SUM(st.weight * st.reps),0) FROM sets st JOIN session_exercises se ON st.sessionExerciseId = se.id
                WHERE se.sessionId = s.id AND st.completed = 1 AND st.isWarmup = 0 AND st.weight IS NOT NULL AND st.reps IS NOT NULL) AS volume
           FROM sessions s WHERE s.status = 'FINISHED' ORDER BY s.startedAt DESC"""
    )
    fun observeFinishedSummaries(): Flow<List<SessionSummary>>

    @Query("SELECT startedAt FROM sessions WHERE status = 'FINISHED'")
    fun observeFinishedDates(): Flow<List<Long>>

    @Insert suspend fun insert(s: SessionEntity): Long
    @Update suspend fun update(s: SessionEntity)
    @Query("DELETE FROM sessions WHERE id = :id") suspend fun delete(id: Long)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY position")
    fun observeExercises(sessionId: Long): Flow<List<SessionExerciseEntity>>
    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY position")
    suspend fun getExercises(sessionId: Long): List<SessionExerciseEntity>
    @Query("SELECT * FROM session_exercises") suspend fun getAllExercises(): List<SessionExerciseEntity>
    @Query("SELECT * FROM session_exercises WHERE id = :id") suspend fun getExercise(id: Long): SessionExerciseEntity?
    @Insert suspend fun insertExercise(e: SessionExerciseEntity): Long
    @Update suspend fun updateExercise(e: SessionExerciseEntity)
    @Update suspend fun updateExercises(e: List<SessionExerciseEntity>)
    @Query("DELETE FROM session_exercises WHERE id = :id") suspend fun deleteExercise(id: Long)

    @Query("SELECT st.* FROM sets st JOIN session_exercises se ON st.sessionExerciseId = se.id WHERE se.sessionId = :sessionId ORDER BY st.position")
    fun observeSets(sessionId: Long): Flow<List<SetEntity>>
    @Query("SELECT st.* FROM sets st JOIN session_exercises se ON st.sessionExerciseId = se.id WHERE se.sessionId = :sessionId ORDER BY st.position")
    suspend fun getSets(sessionId: Long): List<SetEntity>
    @Query("SELECT * FROM sets WHERE sessionExerciseId = :seId ORDER BY position")
    suspend fun getSetsOf(seId: Long): List<SetEntity>
    @Query("SELECT * FROM sets") suspend fun getAllSets(): List<SetEntity>
    @Query("SELECT * FROM sets WHERE id = :id") suspend fun getSet(id: Long): SetEntity?
    @Insert suspend fun insertSet(s: SetEntity): Long
    @Insert suspend fun insertSets(s: List<SetEntity>)
    @Update suspend fun updateSet(s: SetEntity)
    @Update suspend fun updateSets(s: List<SetEntity>)
    @Query("DELETE FROM sets WHERE id = :id") suspend fun deleteSet(id: Long)
    @Query("UPDATE sets SET weight = :w WHERE id = :id") suspend fun setWeight(id: Long, w: Double?)
    @Query("UPDATE sets SET reps = :r WHERE id = :id") suspend fun setReps(id: Long, r: Int?)
    @Query("UPDATE sets SET rpe = :rpe, rir = :rir WHERE id = :id") suspend fun setEffort(id: Long, rpe: Double?, rir: Int?)
    @Query("UPDATE sets SET isWarmup = :w WHERE id = :id") suspend fun setWarmup(id: Long, w: Boolean)

    @Query("DELETE FROM sets WHERE completed = 0 AND sessionExerciseId IN (SELECT id FROM session_exercises WHERE sessionId = :sessionId)")
    suspend fun deleteIncompleteSets(sessionId: Long)
    @Query("DELETE FROM session_exercises WHERE sessionId = :sessionId AND id NOT IN (SELECT sessionExerciseId FROM sets)")
    suspend fun deleteEmptyExercises(sessionId: Long)

    @Query(
        """SELECT st.id AS setId, s.id AS sessionId, s.name AS sessionName, s.startedAt, e.id AS exerciseId, e.name AS exerciseName,
           e.primaryMuscle, st.weight, st.reps, st.isWarmup, st.completed, st.rpe, st.rir, st.position AS setPosition, se.position AS exercisePosition
           FROM sets st JOIN session_exercises se ON st.sessionExerciseId = se.id
           JOIN sessions s ON se.sessionId = s.id JOIN exercises e ON se.exerciseId = e.id
           WHERE s.status = 'FINISHED' AND st.completed = 1
           ORDER BY s.startedAt, se.position, st.position"""
    )
    fun observeAllCompletedRows(): Flow<List<SetRow>>

    @Query(
        """SELECT st.id AS setId, s.id AS sessionId, s.name AS sessionName, s.startedAt, e.id AS exerciseId, e.name AS exerciseName,
           e.primaryMuscle, st.weight, st.reps, st.isWarmup, st.completed, st.rpe, st.rir, st.position AS setPosition, se.position AS exercisePosition
           FROM sets st JOIN session_exercises se ON st.sessionExerciseId = se.id
           JOIN sessions s ON se.sessionId = s.id JOIN exercises e ON se.exerciseId = e.id
           WHERE s.status = 'FINISHED' AND st.completed = 1 AND e.id = :exerciseId
           ORDER BY s.startedAt, st.position"""
    )
    fun observeExerciseRows(exerciseId: Long): Flow<List<SetRow>>

    @Query(
        """SELECT st.id AS setId, s.id AS sessionId, s.name AS sessionName, s.startedAt, e.id AS exerciseId, e.name AS exerciseName,
           e.primaryMuscle, st.weight, st.reps, st.isWarmup, st.completed, st.rpe, st.rir, st.position AS setPosition, se.position AS exercisePosition
           FROM sets st JOIN session_exercises se ON st.sessionExerciseId = se.id
           JOIN sessions s ON se.sessionId = s.id JOIN exercises e ON se.exerciseId = e.id
           WHERE s.status = 'FINISHED' AND st.completed = 1 AND e.id = :exerciseId
           ORDER BY s.startedAt, st.position"""
    )
    suspend fun getExerciseRows(exerciseId: Long): List<SetRow>

    /** Last finished session containing the exercise (excluding [excludeSessionId]). */
    @Query(
        """SELECT se.* FROM session_exercises se JOIN sessions s ON se.sessionId = s.id
           WHERE se.exerciseId = :exerciseId AND s.status = 'FINISHED' AND s.id != :excludeSessionId
           AND EXISTS (SELECT 1 FROM sets st WHERE st.sessionExerciseId = se.id AND st.completed = 1)
           ORDER BY s.startedAt DESC LIMIT 1"""
    )
    suspend fun lastPerformance(exerciseId: Long, excludeSessionId: Long): SessionExerciseEntity?
}

@Dao
interface BodyDao {
    @Query("SELECT * FROM body_measurements ORDER BY date DESC") fun observeMeasurements(): Flow<List<BodyMeasurementEntity>>
    @Query("SELECT * FROM body_measurements") suspend fun getAllMeasurements(): List<BodyMeasurementEntity>
    @Query("SELECT * FROM body_measurements WHERE id = :id") suspend fun getMeasurement(id: Long): BodyMeasurementEntity?
    @Upsert suspend fun upsertMeasurement(m: BodyMeasurementEntity): Long
    @Query("DELETE FROM body_measurements WHERE id = :id") suspend fun deleteMeasurement(id: Long)

    @Query("SELECT * FROM progress_photos ORDER BY date DESC") fun observePhotos(): Flow<List<ProgressPhotoEntity>>
    @Query("SELECT * FROM progress_photos") suspend fun getAllPhotos(): List<ProgressPhotoEntity>
    @Insert suspend fun insertPhoto(p: ProgressPhotoEntity): Long
    @Delete suspend fun deletePhoto(p: ProgressPhotoEntity)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY achievedAt IS NOT NULL, createdAt DESC") fun observeAll(): Flow<List<GoalEntity>>
    @Query("SELECT * FROM goals") suspend fun getAll(): List<GoalEntity>
    @Upsert suspend fun upsert(g: GoalEntity): Long
    @Query("DELETE FROM goals WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY dayOfWeek, hour, minute") fun observeAll(): Flow<List<ReminderEntity>>
    @Query("SELECT * FROM reminders") suspend fun getAll(): List<ReminderEntity>
    @Query("SELECT * FROM reminders WHERE id = :id") suspend fun get(id: Long): ReminderEntity?
    @Upsert suspend fun upsert(r: ReminderEntity): Long
    @Query("DELETE FROM reminders WHERE id = :id") suspend fun delete(id: Long)
}

/** Bulk operations used by backup import. */
@Dao
interface BackupDao {
    @Query("DELETE FROM sets") suspend fun clearSets()
    @Query("DELETE FROM session_exercises") suspend fun clearSessionExercises()
    @Query("DELETE FROM sessions") suspend fun clearSessions()
    @Query("DELETE FROM routine_versions") suspend fun clearVersions()
    @Query("DELETE FROM routine_exercises") suspend fun clearRoutineExercises()
    @Query("DELETE FROM routines") suspend fun clearRoutines()
    @Query("DELETE FROM goals") suspend fun clearGoals()
    @Query("DELETE FROM exercises") suspend fun clearExercises()
    @Query("DELETE FROM body_measurements") suspend fun clearMeasurements()
    @Query("DELETE FROM progress_photos") suspend fun clearPhotos()
    @Query("DELETE FROM reminders") suspend fun clearReminders()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun exercises(l: List<ExerciseEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun routines(l: List<RoutineEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun routineExercises(l: List<RoutineExerciseEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun versions(l: List<RoutineVersionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun sessions(l: List<SessionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun sessionExercises(l: List<SessionExerciseEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun sets(l: List<SetEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun measurements(l: List<BodyMeasurementEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun photos(l: List<ProgressPhotoEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun goals(l: List<GoalEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun reminders(l: List<ReminderEntity>)
}

package com.gymtrack.app.data.repo

import androidx.room.withTransaction
import com.gymtrack.app.data.db.ExerciseEntity
import com.gymtrack.app.data.db.GymDatabase
import com.gymtrack.app.data.db.RoutineEntity
import com.gymtrack.app.data.db.RoutineExerciseEntity
import com.gymtrack.app.data.db.RoutineVersionEntity
import com.gymtrack.app.data.db.SessionEntity
import com.gymtrack.app.data.db.SessionExerciseEntity
import com.gymtrack.app.data.db.SessionStatus
import com.gymtrack.app.data.db.SetEntity
import com.gymtrack.app.data.seed.ExerciseSeed
import com.gymtrack.app.data.seed.ProgramTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class SessionExerciseDetail(
    val entry: SessionExerciseEntity,
    val exercise: ExerciseEntity,
    val sets: List<SetEntity>,
)

data class SessionDetail(val session: SessionEntity, val exercises: List<SessionExerciseDetail>)

@Serializable
private data class RoutineSnapshot(val name: String, val notes: String, val program: String, val exercises: List<RoutineExerciseEntity>)

class WorkoutRepository(private val db: GymDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    private val exerciseDao = db.exerciseDao()
    private val routineDao = db.routineDao()
    private val sessionDao = db.sessionDao()
    private val json = Json { ignoreUnknownKeys = true }

    // ------------------------------------------------------------------ seed
    /** Inserts library exercises that are missing (by stable key). Never touches user edits. */
    suspend fun seedIfNeeded() {
        val existing = exerciseDao.seedKeys().toSet()
        val missing = ExerciseSeed.all.filter { it.seedKey !in existing }
        if (missing.isNotEmpty()) exerciseDao.insertAll(missing)
    }

    // ------------------------------------------------------------- exercises
    fun observeExercises() = exerciseDao.observeAll()
    fun observeExercise(id: Long) = exerciseDao.observe(id)
    suspend fun getExercise(id: Long) = exerciseDao.get(id)
    suspend fun saveExercise(e: ExerciseEntity): Long = if (e.id == 0L) exerciseDao.insert(e) else { exerciseDao.update(e); e.id }
    suspend fun setFavorite(id: Long, fav: Boolean) = exerciseDao.setFavorite(id, fav)

    /** Custom exercises with history are archived (history kept); unused ones are deleted. */
    suspend fun deleteExercise(id: Long) {
        val e = exerciseDao.get(id) ?: return
        if (exerciseDao.usageCount(id) > 0 || !e.isCustom) exerciseDao.update(e.copy(archived = true)) else exerciseDao.delete(id)
    }

    // -------------------------------------------------------------- routines
    fun observeRoutines() = routineDao.observeSummaries()
    fun observeRoutine(id: Long) = routineDao.observe(id)
    fun observeRoutineExercises(id: Long) = routineDao.observeExercises(id)
    fun observeVersions(id: Long) = routineDao.observeVersions(id)

    suspend fun createRoutine(name: String, program: String = "", notes: String = ""): Long =
        routineDao.insert(RoutineEntity(name = name.trim().ifBlank { "Novo treino" }, program = program.trim(), notes = notes, position = routineDao.nextPosition(), createdAt = clock(), updatedAt = clock()))

    suspend fun updateRoutine(r: RoutineEntity) = routineDao.update(r.copy(updatedAt = clock()))

    suspend fun deleteRoutine(id: Long) = routineDao.delete(id)

    suspend fun duplicateRoutine(id: Long): Long = db.withTransaction {
        val r = routineDao.get(id) ?: error("Treino não encontrado")
        val newId = routineDao.insert(r.copy(id = 0, name = "${r.name} (cópia)", position = routineDao.nextPosition(), createdAt = clock(), updatedAt = clock()))
        routineDao.insertExercises(routineDao.getExercises(id).map { it.copy(id = 0, routineId = newId) })
        newId
    }

    suspend fun addExercisesToRoutine(routineId: Long, exerciseIds: List<Long>) = db.withTransaction {
        val current = routineDao.getExercises(routineId)
        var pos = (current.maxOfOrNull { it.position } ?: -1) + 1
        exerciseIds.forEach { exId ->
            val ex = exerciseDao.get(exId) ?: return@forEach
            routineDao.insertExercise(RoutineExerciseEntity(routineId = routineId, exerciseId = exId, position = pos++, restSec = ex.defaultRestSec))
        }
        touch(routineId)
    }

    suspend fun updateRoutineExercise(e: RoutineExerciseEntity) {
        routineDao.updateExercise(e.copy(sets = e.sets.coerceIn(1, 20), warmupSets = e.warmupSets.coerceIn(0, 10), repsMin = e.repsMin.coerceAtLeast(1), repsMax = e.repsMax.coerceAtLeast(e.repsMin.coerceAtLeast(1)), restSec = e.restSec.coerceIn(0, 900)))
        touch(e.routineId)
    }

    suspend fun removeRoutineExercise(e: RoutineExerciseEntity) = db.withTransaction {
        routineDao.deleteExercise(e.id)
        routineDao.updateExercises(routineDao.getExercises(e.routineId).mapIndexed { i, x -> x.copy(position = i) })
        touch(e.routineId)
    }

    suspend fun moveRoutineExercise(routineId: Long, from: Int, to: Int) = db.withTransaction {
        val list = routineDao.getExercises(routineId).toMutableList()
        if (from !in list.indices || to !in list.indices) return@withTransaction
        list.add(to, list.removeAt(from))
        routineDao.updateExercises(list.mapIndexed { i, x -> x.copy(position = i) })
        touch(routineId)
    }

    private suspend fun touch(routineId: Long) {
        routineDao.get(routineId)?.let { routineDao.update(it.copy(updatedAt = clock())) }
    }

    suspend fun saveVersion(routineId: Long, label: String): Long {
        val r = routineDao.get(routineId) ?: error("Treino não encontrado")
        val snap = RoutineSnapshot(r.name, r.notes, r.program, routineDao.getExercises(routineId))
        return routineDao.insertVersion(RoutineVersionEntity(routineId = routineId, createdAt = clock(), label = label.ifBlank { "Versão" }, snapshot = json.encodeToString(snap)))
    }

    /** Restores a version; the current state is saved as a version first so nothing is lost. */
    suspend fun restoreVersion(version: RoutineVersionEntity) = db.withTransaction {
        saveVersion(version.routineId, "Antes de restaurar \"${version.label}\"")
        val snap = json.decodeFromString<RoutineSnapshot>(version.snapshot)
        val r = routineDao.get(version.routineId) ?: return@withTransaction
        routineDao.update(r.copy(name = snap.name, notes = snap.notes, program = snap.program, updatedAt = clock()))
        routineDao.deleteExercisesOf(r.id)
        val validIds = exerciseDao.getAll().map { it.id }.toSet()
        routineDao.insertExercises(snap.exercises.filter { it.exerciseId in validIds }.mapIndexed { i, x -> x.copy(id = 0, routineId = r.id, position = i) })
    }

    suspend fun deleteVersion(id: Long) = routineDao.deleteVersion(id)

    /** Imports a template as normal editable routines. Returns the created routine ids. */
    suspend fun importTemplate(template: ProgramTemplate): List<Long> = db.withTransaction {
        seedIfNeeded()
        template.days.map { day ->
            val rid = createRoutine(day.name, template.name)
            val items = day.items.mapIndexedNotNull { i, item ->
                val ex = exerciseDao.bySeedKey(item.key) ?: return@mapIndexedNotNull null
                RoutineExerciseEntity(routineId = rid, exerciseId = ex.id, position = i, sets = item.sets, repsMin = item.repsMin, repsMax = item.repsMax, restSec = item.rest ?: ex.defaultRestSec)
            }
            routineDao.insertExercises(items)
            rid
        }
    }

    // -------------------------------------------------------------- sessions
    fun observeActiveSession() = sessionDao.observeActive()
    suspend fun getActiveSession() = sessionDao.getActive()
    fun observeSession(id: Long) = sessionDao.observe(id)
    fun observeFinishedSessions() = sessionDao.observeFinishedSummaries()
    fun observeFinishedDates() = sessionDao.observeFinishedDates()
    fun observeAllCompletedRows() = sessionDao.observeAllCompletedRows()
    fun observeExerciseRows(exerciseId: Long) = sessionDao.observeExerciseRows(exerciseId)
    suspend fun getExerciseRows(exerciseId: Long) = sessionDao.getExerciseRows(exerciseId)

    fun observeSessionDetail(sessionId: Long): Flow<SessionDetail?> =
        combine(sessionDao.observe(sessionId), sessionDao.observeExercises(sessionId), sessionDao.observeSets(sessionId), exerciseDao.observeAllIncludingArchived()) { s, exs, sets, allEx ->
            if (s == null) return@combine null
            val exMap = allEx.associateBy { it.id }
            val bySe = sets.groupBy { it.sessionExerciseId }
            SessionDetail(s, exs.mapNotNull { se -> exMap[se.exerciseId]?.let { SessionExerciseDetail(se, it, bySe[se.id].orEmpty().sortedBy { st -> st.position }) } })
        }

    suspend fun getSessionDetail(sessionId: Long): SessionDetail? {
        val s = sessionDao.get(sessionId) ?: return null
        val exMap = exerciseDao.getAll().associateBy { it.id }
        val sets = sessionDao.getSets(sessionId).groupBy { it.sessionExerciseId }
        return SessionDetail(s, sessionDao.getExercises(sessionId).mapNotNull { se -> exMap[se.exerciseId]?.let { SessionExerciseDetail(se, it, sets[se.id].orEmpty().sortedBy { st -> st.position }) } })
    }

    /** Previous finished sets of an exercise (for the "Anterior" column). */
    suspend fun previousSets(exerciseId: Long, excludeSessionId: Long): List<SetEntity> {
        val se = sessionDao.lastPerformance(exerciseId, excludeSessionId) ?: return emptyList()
        return sessionDao.getSetsOf(se.id).filter { it.completed }
    }

    /**
     * Starts a session (optionally from a routine). Sets are pre-filled with the values of the
     * last time each exercise was performed, falling back to the planned load.
     * If a session is already active, it is returned instead (only one active session).
     */
    suspend fun startSession(routineId: Long?): Long = db.withTransaction {
        sessionDao.getActive()?.let { return@withTransaction it.id }
        val routine = routineId?.let { routineDao.get(it) }
        val sessionId = sessionDao.insert(SessionEntity(routineId = routine?.id, name = routine?.name ?: "Treino livre", startedAt = clock()))
        routine?.let { r ->
            routineDao.getExercises(r.id).forEachIndexed { i, re ->
                val seId = sessionDao.insertExercise(SessionExerciseEntity(sessionId = sessionId, exerciseId = re.exerciseId, position = i, restSec = re.restSec, notes = re.notes, repsMin = re.repsMin, repsMax = re.repsMax))
                val prev = previousSets(re.exerciseId, sessionId)
                val prevWarm = prev.filter { it.isWarmup }
                val prevWork = prev.filter { !it.isWarmup }
                val sets = mutableListOf<SetEntity>()
                repeat(re.warmupSets) { w ->
                    val p = prevWarm.getOrNull(w)
                    sets += SetEntity(sessionExerciseId = seId, position = sets.size, isWarmup = true, weight = p?.weight, reps = p?.reps)
                }
                repeat(re.sets) { k ->
                    val p = prevWork.getOrNull(k) ?: prevWork.lastOrNull()
                    sets += SetEntity(sessionExerciseId = seId, position = sets.size, weight = p?.weight ?: re.plannedWeight, reps = p?.reps)
                }
                sessionDao.insertSets(sets)
            }
        }
        sessionId
    }

    suspend fun addExerciseToSession(sessionId: Long, exerciseId: Long, setCount: Int = 3): Long = db.withTransaction {
        val ex = exerciseDao.get(exerciseId) ?: error("Exercício não encontrado")
        val pos = (sessionDao.getExercises(sessionId).maxOfOrNull { it.position } ?: -1) + 1
        val seId = sessionDao.insertExercise(SessionExerciseEntity(sessionId = sessionId, exerciseId = exerciseId, position = pos, restSec = ex.defaultRestSec))
        val prevWork = previousSets(exerciseId, sessionId).filter { !it.isWarmup }
        sessionDao.insertSets((0 until setCount).map { k ->
            val p = prevWork.getOrNull(k) ?: prevWork.lastOrNull()
            SetEntity(sessionExerciseId = seId, position = k, weight = p?.weight, reps = p?.reps)
        })
        seId
    }

    /** Swaps the exercise of an entry. Completed sets are kept; pending sets are re-filled from the new exercise's history. */
    suspend fun replaceSessionExercise(seId: Long, newExerciseId: Long) = db.withTransaction {
        val se = sessionDao.getExercise(seId) ?: return@withTransaction
        val ex = exerciseDao.get(newExerciseId) ?: return@withTransaction
        sessionDao.updateExercise(se.copy(exerciseId = newExerciseId, restSec = ex.defaultRestSec))
        val prevWork = previousSets(newExerciseId, se.sessionId).filter { !it.isWarmup }
        var k = 0
        sessionDao.updateSets(sessionDao.getSetsOf(seId).filter { !it.completed }.map { s ->
            if (s.isWarmup) s.copy(weight = null, reps = null) else {
                val p = prevWork.getOrNull(k++) ?: prevWork.lastOrNull(); s.copy(weight = p?.weight, reps = p?.reps)
            }
        })
    }

    suspend fun removeSessionExercise(seId: Long) = db.withTransaction {
        val se = sessionDao.getExercise(seId) ?: return@withTransaction
        sessionDao.deleteExercise(seId)
        sessionDao.updateExercises(sessionDao.getExercises(se.sessionId).mapIndexed { i, x -> x.copy(position = i) })
    }

    suspend fun moveSessionExercise(sessionId: Long, from: Int, to: Int) = db.withTransaction {
        val list = sessionDao.getExercises(sessionId).toMutableList()
        if (from !in list.indices || to !in list.indices) return@withTransaction
        list.add(to, list.removeAt(from))
        sessionDao.updateExercises(list.mapIndexed { i, x -> x.copy(position = i) })
    }

    suspend fun updateSessionExercise(se: SessionExerciseEntity) = sessionDao.updateExercise(se)

    /** Adds a set copying the last set's values. Warm-up sets are inserted before working sets. */
    suspend fun addSet(seId: Long, warmup: Boolean): Long = db.withTransaction {
        val sets = sessionDao.getSetsOf(seId)
        val template = if (warmup) sets.lastOrNull { it.isWarmup } else sets.lastOrNull { !it.isWarmup }
        val newSet = SetEntity(sessionExerciseId = seId, position = 0, isWarmup = warmup, weight = template?.weight, reps = template?.reps)
        val ordered = sets.toMutableList()
        val insertAt = if (warmup) (ordered.indexOfLast { it.isWarmup } + 1) else ordered.size
        val id = sessionDao.insertSet(newSet.copy(position = insertAt))
        ordered.add(insertAt, newSet.copy(id = id))
        sessionDao.updateSets(ordered.mapIndexed { i, s -> s.copy(position = i) })
        id
    }

    suspend fun deleteSet(setId: Long) = db.withTransaction {
        val s = sessionDao.getSet(setId) ?: return@withTransaction
        sessionDao.deleteSet(setId)
        sessionDao.updateSets(sessionDao.getSetsOf(s.sessionExerciseId).mapIndexed { i, x -> x.copy(position = i) })
    }

    suspend fun updateSet(set: SetEntity) = sessionDao.updateSet(set)
    suspend fun setWeight(id: Long, w: Double?) = sessionDao.setWeight(id, w)
    suspend fun setReps(id: Long, r: Int?) = sessionDao.setReps(id, r)
    suspend fun setEffort(id: Long, rpe: Double?, rir: Int?) = sessionDao.setEffort(id, rpe, rir)

    /** Toggles warm-up flag and keeps warm-ups ordered before working sets. */
    suspend fun setWarmup(id: Long, warmup: Boolean) = db.withTransaction {
        val s = sessionDao.getSet(id) ?: return@withTransaction
        sessionDao.setWarmup(id, warmup)
        val ordered = sessionDao.getSetsOf(s.sessionExerciseId).sortedWith(compareBy<SetEntity> { !it.isWarmup }.thenBy { it.position })
        sessionDao.updateSets(ordered.mapIndexed { i, x -> x.copy(position = i) })
    }

    /** Copies values into a set (e.g. from the previous session). */
    suspend fun fillSet(id: Long, weight: Double?, reps: Int?) { sessionDao.setWeight(id, weight); sessionDao.setReps(id, reps) }

    /**
     * One-tap completion. Returns the updated set, or null when the set has no repetitions yet
     * (the UI then asks the user to fill them in).
     */
    suspend fun toggleSetCompleted(setId: Long): SetEntity? {
        val s = sessionDao.getSet(setId) ?: return null
        val updated = if (s.completed) s.copy(completed = false, completedAt = null) else {
            if ((s.reps ?: 0) <= 0) return null
            s.copy(completed = true, completedAt = clock(), weight = s.weight ?: 0.0)
        }
        sessionDao.updateSet(updated)
        return updated
    }

    suspend fun startRest(sessionId: Long, seconds: Int, exerciseName: String?) {
        val s = sessionDao.get(sessionId) ?: return
        sessionDao.update(s.copy(restEndsAt = clock() + seconds * 1000L, restTotalSec = seconds, restExerciseName = exerciseName))
    }

    suspend fun adjustRest(sessionId: Long, deltaSec: Int) {
        val s = sessionDao.get(sessionId) ?: return
        val end = s.restEndsAt ?: return
        val newEnd = maxOf(clock(), end + deltaSec * 1000L)
        sessionDao.update(s.copy(restEndsAt = newEnd, restTotalSec = (s.restTotalSec + deltaSec).coerceAtLeast(1)))
    }

    suspend fun clearRest(sessionId: Long) {
        val s = sessionDao.get(sessionId) ?: return
        sessionDao.update(s.copy(restEndsAt = null, restTotalSec = 0, restExerciseName = null))
    }

    suspend fun pauseSession(sessionId: Long) {
        val s = sessionDao.get(sessionId) ?: return
        if (s.pausedAt == null) sessionDao.update(s.copy(pausedAt = clock(), restEndsAt = null))
    }

    suspend fun resumeSession(sessionId: Long) {
        val s = sessionDao.get(sessionId) ?: return
        val p = s.pausedAt ?: return
        sessionDao.update(s.copy(pausedAt = null, pausedAccumMs = s.pausedAccumMs + (clock() - p)))
    }

    suspend fun renameSession(sessionId: Long, name: String, notes: String) {
        val s = sessionDao.get(sessionId) ?: return
        sessionDao.update(s.copy(name = name.ifBlank { s.name }, notes = notes))
    }

    suspend fun completedSetCount(sessionId: Long): Int = sessionDao.getSets(sessionId).count { it.completed }

    /** Finishes the session: pending (not completed) sets and empty exercises are removed. */
    suspend fun finishSession(sessionId: Long) = db.withTransaction {
        val s = sessionDao.get(sessionId) ?: return@withTransaction
        val now = clock()
        val pausedExtra = s.pausedAt?.let { now - it } ?: 0
        sessionDao.deleteIncompleteSets(sessionId)
        sessionDao.deleteEmptyExercises(sessionId)
        sessionDao.updateExercises(sessionDao.getExercises(sessionId).mapIndexed { i, x -> x.copy(position = i) })
        sessionDao.update(s.copy(status = SessionStatus.FINISHED, endedAt = now, pausedAt = null, pausedAccumMs = s.pausedAccumMs + pausedExtra, restEndsAt = null, restTotalSec = 0))
    }

    suspend fun deleteSession(sessionId: Long) = sessionDao.delete(sessionId)

    /** Copies the heaviest working weight of each exercise back into the routine's planned load. */
    suspend fun updateRoutineFromSession(sessionId: Long): Int = db.withTransaction {
        val detail = getSessionDetail(sessionId) ?: return@withTransaction 0
        val rid = detail.session.routineId ?: return@withTransaction 0
        val best = detail.exercises.associate { d -> d.exercise.id to d.sets.filter { it.completed && !it.isWarmup }.mapNotNull { it.weight }.maxOrNull() }
        var changed = 0
        routineDao.updateExercises(routineDao.getExercises(rid).map { re ->
            val w = best[re.exerciseId]
            if (w != null && w > 0 && w != re.plannedWeight) { changed++; re.copy(plannedWeight = w) } else re
        })
        touch(rid)
        changed
    }
}

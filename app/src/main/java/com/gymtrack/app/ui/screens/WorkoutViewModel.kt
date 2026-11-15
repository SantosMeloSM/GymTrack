package com.gymtrack.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.app.AppContainer
import com.gymtrack.app.data.db.ExerciseEntity
import com.gymtrack.app.data.db.SessionExerciseEntity
import com.gymtrack.app.data.db.SetEntity
import com.gymtrack.app.data.repo.SessionDetail
import com.gymtrack.app.notify.RestTimerScheduler
import com.gymtrack.domain.Equipment
import com.gymtrack.domain.MuscleGroup
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(private val c: AppContainer) : ViewModel() {
    private val repo = c.workouts

    /** null = loading, false = no active session */
    val hasActive: StateFlow<Boolean?> = repo.observeActiveSession().map { it != null }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val detail: StateFlow<SessionDetail?> = repo.observeActiveSession()
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else repo.observeSessionDetail(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val exercises: StateFlow<List<ExerciseEntity>> = repo.observeExercises().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Previous performance per exercise id (for the "Anterior" column). */
    private val _previous = MutableStateFlow<Map<Long, List<SetEntity>>>(emptyMap())
    val previous = _previous.asStateFlow()

    val messages = Channel<String>(Channel.BUFFERED)

    /** All writes run strictly in order, so a quick "type reps → tap ✓" never races. */
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        viewModelScope.launch { for (w in writes) runCatching { w() }.onFailure { messages.trySend(it.message ?: "Erro ao salvar") } }
        viewModelScope.launch {
            detail.collect { d ->
                if (d == null) return@collect
                val missing = d.exercises.map { it.exercise.id }.filter { it !in _previous.value }
                if (missing.isNotEmpty()) {
                    val add = missing.associateWith { repo.previousSets(it, d.session.id) }
                    _previous.value = _previous.value + add
                }
            }
        }
    }

    private fun enqueue(block: suspend () -> Unit) { writes.trySend(block) }
    private val sessionId get() = detail.value?.session?.id

    fun setWeight(id: Long, w: Double?) = enqueue { repo.setWeight(id, w) }
    fun setReps(id: Long, r: Int?) = enqueue { repo.setReps(id, r) }
    fun setEffort(id: Long, rpe: Double?, rir: Int?) = enqueue { repo.setEffort(id, rpe, rir) }
    fun setWarmup(id: Long, warmup: Boolean) = enqueue { repo.setWarmup(id, warmup) }
    fun fill(id: Long, w: Double?, r: Int?) = enqueue { repo.fillSet(id, w, r) }
    fun addSet(seId: Long, warmup: Boolean) = enqueue { repo.addSet(seId, warmup) }
    fun deleteSet(id: Long) = enqueue { repo.deleteSet(id) }

    fun toggle(set: SetEntity, entry: SessionExerciseEntity, exerciseName: String) = enqueue {
        val updated = repo.toggleSetCompleted(set.id)
        if (updated == null) { messages.trySend("Informe as repetições antes de concluir a série"); return@enqueue }
        val sid = entry.sessionId
        if (updated.completed) {
            val settings = c.settings.current()
            if (settings.autoStartRest && entry.restSec > 0) startRestInternal(sid, entry.restSec, exerciseName)
        }
    }

    private suspend fun startRestInternal(sid: Long, seconds: Int, name: String?) {
        repo.startRest(sid, seconds, name)
        repo.getActiveSession()?.restEndsAt?.let { RestTimerScheduler.schedule(c.appContext, it) }
    }

    fun startRest(seconds: Int, name: String?) { val sid = sessionId ?: return; enqueue { startRestInternal(sid, seconds, name) } }

    fun adjustRest(delta: Int) {
        val sid = sessionId ?: return
        enqueue {
            repo.adjustRest(sid, delta)
            repo.getActiveSession()?.restEndsAt?.let { RestTimerScheduler.schedule(c.appContext, it) }
        }
    }

    fun skipRest() { val sid = sessionId ?: return; enqueue { RestTimerScheduler.cancel(c.appContext); repo.clearRest(sid) } }

    fun pause() { val sid = sessionId ?: return; enqueue { RestTimerScheduler.cancel(c.appContext); repo.pauseSession(sid) } }
    fun resume() { val sid = sessionId ?: return; enqueue { repo.resumeSession(sid) } }

    fun addExercises(ids: List<Long>) { val sid = sessionId ?: return; enqueue { ids.forEach { repo.addExerciseToSession(sid, it) } } }
    fun replaceExercise(seId: Long, exerciseId: Long) = enqueue {
        repo.replaceSessionExercise(seId, exerciseId)
        sessionId?.let { sid -> _previous.value = _previous.value + (exerciseId to repo.previousSets(exerciseId, sid)) }
    }
    fun removeExercise(seId: Long) = enqueue { repo.removeSessionExercise(seId) }
    fun moveExercise(from: Int, to: Int) { val sid = sessionId ?: return; enqueue { repo.moveSessionExercise(sid, from, to) } }
    fun updateEntry(e: SessionExerciseEntity) = enqueue { repo.updateSessionExercise(e) }
    fun rename(name: String, notes: String) { val sid = sessionId ?: return; enqueue { repo.renameSession(sid, name, notes) } }

    fun createExercise(name: String, muscle: MuscleGroup, eq: Equipment, done: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repo.saveExercise(ExerciseEntity(name = name, primaryMuscle = muscle.name, equipment = eq.name, isCustom = true, defaultRestSec = c.settings.current().defaultRestSec))
            done(id)
        }
    }

    /** Finishes the session; [onDone] receives the session id, or null if nothing was logged. */
    fun finish(onDone: (Long?) -> Unit) {
        val sid = sessionId ?: return
        enqueue {
            RestTimerScheduler.cancel(c.appContext)
            if (repo.completedSetCount(sid) == 0) { onDone(null); return@enqueue }
            repo.finishSession(sid)
            if (c.settings.current().autoBackup) runCatching { c.backup.writeAutoBackup() }
            onDone(sid)
        }
    }

    fun discard(onDone: () -> Unit) {
        val sid = sessionId ?: return
        enqueue { RestTimerScheduler.cancel(c.appContext); repo.deleteSession(sid); onDone() }
    }
}

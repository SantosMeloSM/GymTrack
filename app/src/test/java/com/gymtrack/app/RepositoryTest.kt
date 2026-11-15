package com.gymtrack.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.app.data.db.BodyMeasurementEntity
import com.gymtrack.app.data.db.GymDatabase
import com.gymtrack.app.data.db.ReminderEntity
import com.gymtrack.app.data.db.SessionStatus
import com.gymtrack.app.data.repo.BackupManager
import com.gymtrack.app.data.repo.ReportExporter
import com.gymtrack.app.data.repo.WorkoutRepository
import com.gymtrack.app.data.seed.ExerciseSeed
import com.gymtrack.app.data.seed.ProgramTemplates
import com.gymtrack.app.notify.ReminderScheduler
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RepositoryTest {
    private lateinit var db: GymDatabase
    private lateinit var repo: WorkoutRepository
    private var now = 1_700_000_000_000L
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, GymDatabase::class.java).allowMainThreadQueries().build()
        repo = WorkoutRepository(db) { now }
    }

    @After fun tearDown() = db.close()

    private suspend fun exId(key: String) = db.exerciseDao().bySeedKey(key)!!.id

    @Test fun seedIsIdempotentAndComplete() = runBlocking {
        repo.seedIfNeeded(); repo.seedIfNeeded()
        assertEquals(ExerciseSeed.all.size, db.exerciseDao().count())
        assertTrue(ExerciseSeed.all.size >= 100)
        assertEquals(ExerciseSeed.all.size, ExerciseSeed.all.map { it.seedKey }.toSet().size)
        assertTrue(ExerciseSeed.all.all { it.description.isNotBlank() && it.tips.isNotBlank() })
    }

    @Test fun templatesReferenceExistingExercises() = runBlocking {
        val keys = ExerciseSeed.all.map { it.seedKey }.toSet()
        ProgramTemplates.all.forEach { t -> t.days.forEach { d -> d.items.forEach { assertTrue("${it.key} missing", it.key in keys) } } }
        val ids = repo.importTemplate(ProgramTemplates.all.first { it.id == "ppl" })
        assertEquals(3, ids.size)
        val push = db.routineDao().getExercises(ids[0])
        assertEquals(6, push.size)
        assertEquals("Push / Pull / Legs", db.routineDao().get(ids[0])!!.program)
    }

    @Test fun fullWorkoutFlowWithPrefillRecordsAndFinish() = runBlocking {
        repo.seedIfNeeded()
        val bench = exId("bench_press")
        val rid = repo.createRoutine("Treino A")
        repo.addExercisesToRoutine(rid, listOf(bench))
        val re = db.routineDao().getExercises(rid).single()
        repo.updateRoutineExercise(re.copy(sets = 3, warmupSets = 1, plannedWeight = 60.0))

        // Session 1: planned load is pre-filled, reps empty
        val s1 = repo.startSession(rid)
        assertEquals("starting again returns the active session", s1, repo.startSession(rid))
        var sets = db.sessionDao().getSets(s1)
        assertEquals(4, sets.size)
        assertTrue(sets.first().isWarmup)
        assertEquals(60.0, sets[1].weight!!, 0.0)
        assertNull("cannot complete a set without reps", repo.toggleSetCompleted(sets[1].id))
        repo.setReps(sets[1].id, 10)
        val done = repo.toggleSetCompleted(sets[1].id)!!
        assertTrue(done.completed); assertEquals(now, done.completedAt)
        repo.setReps(sets[2].id, 8); repo.toggleSetCompleted(sets[2].id)
        // extra set copies values of the last working set
        val extra = repo.addSet(db.sessionDao().getExercises(s1).single().id, warmup = false)
        assertEquals(60.0, db.sessionDao().getSet(extra)!!.weight!!, 0.0)

        // rest timer & pause bookkeeping
        repo.startRest(s1, 90, "Supino")
        assertEquals(now + 90_000, repo.getActiveSession()!!.restEndsAt)
        repo.adjustRest(s1, 15)
        assertEquals(now + 105_000, repo.getActiveSession()!!.restEndsAt)
        repo.pauseSession(s1); now += 60_000; repo.resumeSession(s1)
        assertEquals(60_000, repo.getActiveSession()!!.pausedAccumMs)

        now += 600_000
        repo.finishSession(s1)
        val fin = db.sessionDao().get(s1)!!
        assertEquals(SessionStatus.FINISHED, fin.status)
        assertNull(repo.getActiveSession())
        sets = db.sessionDao().getSets(s1)
        assertEquals("incomplete sets removed on finish", 2, sets.size)
        assertEquals(600_000, fin.elapsedMs(0))

        // Session 2: values pre-filled from session 1 (previous performance)
        now += 86_400_000
        val s2 = repo.startSession(rid)
        val s2sets = db.sessionDao().getSets(s2).filter { !it.isWarmup }
        assertEquals(listOf(10, 8, 8), s2sets.map { it.reps })
        val prev = repo.previousSets(bench, s2)
        assertEquals(2, prev.size)
        repo.setWeight(s2sets[0].id, 65.0); repo.toggleSetCompleted(s2sets[0].id)
        repo.finishSession(s2)

        val summaries = repo.observeFinishedSessions().first()
        assertEquals(2, summaries.size)
        assertEquals(65.0 * 10, summaries.first().volume, 0.0)
        assertEquals(60.0 * 10 + 60.0 * 8, summaries.last().volume, 0.0)

        assertEquals(1, repo.updateRoutineFromSession(s2))
        assertEquals(65.0, db.routineDao().getExercises(rid).single().plannedWeight!!, 0.0)
    }

    @Test fun replaceAndReorderInSession() = runBlocking {
        repo.seedIfNeeded()
        val s = repo.startSession(null)
        val a = repo.addExerciseToSession(s, exId("bench_press"))
        repo.addExerciseToSession(s, exId("lat_pulldown"))
        repo.moveSessionExercise(s, 1, 0)
        assertEquals(exId("lat_pulldown"), db.sessionDao().getExercises(s).first().exerciseId)
        repo.replaceSessionExercise(a, exId("incline_db_press"))
        assertEquals(exId("incline_db_press"), db.sessionDao().getExercise(a)!!.exerciseId)
        repo.removeSessionExercise(a)
        assertEquals(1, db.sessionDao().getExercises(s).size)
        val setId = db.sessionDao().getSets(s).first().id
        repo.setWarmup(setId, true)
        assertTrue(db.sessionDao().getSet(setId)!!.isWarmup)
        repo.finishSession(s) // no completed sets -> everything removed
        assertTrue(db.sessionDao().getExercises(s).isEmpty())
    }

    @Test fun routineDuplicateReorderAndVersions() = runBlocking {
        repo.seedIfNeeded()
        val rid = repo.createRoutine("Pull", "PPL")
        repo.addExercisesToRoutine(rid, listOf(exId("lat_pulldown"), exId("barbell_row"), exId("barbell_curl")))
        repo.moveRoutineExercise(rid, 2, 0)
        assertEquals(exId("barbell_curl"), db.routineDao().getExercises(rid).first().exerciseId)
        repo.saveVersion(rid, "v1")
        val first = db.routineDao().getExercises(rid).first()
        repo.removeRoutineExercise(first)
        assertEquals(2, db.routineDao().getExercises(rid).size)
        val v1 = repo.observeVersions(rid).first().single()
        repo.restoreVersion(v1)
        assertEquals(3, db.routineDao().getExercises(rid).size)
        assertEquals(2, repo.observeVersions(rid).first().size) // automatic pre-restore version
        val copy = repo.duplicateRoutine(rid)
        assertEquals("Pull (cópia)", db.routineDao().get(copy)!!.name)
        assertEquals(3, db.routineDao().getExercises(copy).size)
    }

    @Test fun backupRoundTripReplacesData() = runBlocking {
        repo.seedIfNeeded()
        val rid = repo.createRoutine("Legs")
        repo.addExercisesToRoutine(rid, listOf(exId("back_squat")))
        val s = repo.startSession(rid)
        val set = db.sessionDao().getSets(s).first()
        repo.fillSet(set.id, 100.0, 5); repo.toggleSetCompleted(set.id); repo.finishSession(s)
        db.bodyDao().upsertMeasurement(BodyMeasurementEntity(date = now, weight = 80.5, waist = 82.0))
        db.reminderDao().upsert(ReminderEntity(dayOfWeek = 1, hour = 7, minute = 30))

        val photos = File(context.filesDir, "test-photos").apply { mkdirs() }
        val backup = BackupManager(context, db) { photos }
        val out = ByteArrayOutputStream()
        backup.exportZip(out)

        // wipe & change things
        repo.deleteSession(s); repo.deleteRoutine(rid)
        assertTrue(repo.observeFinishedSessions().first().isEmpty())

        backup.importFrom(out.toByteArray().inputStream())
        val sessions = repo.observeFinishedSessions().first()
        assertEquals(1, sessions.size)
        assertEquals(500.0, sessions.single().volume, 0.0)
        assertEquals(80.5, db.bodyDao().getAllMeasurements().single().weight!!, 0.0)
        assertEquals(1, db.reminderDao().getAll().size)
        assertTrue("pre-import safety copy created", backup.listAutoBackups().any { it.file.name.startsWith("pre-import") })
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidBackupIsRejected() { runBlocking { BackupManager(context, db) { context.filesDir }.importFrom("{\"hello\":1}".byteInputStream()) } }

    @Test fun csvAndPdfExports() = runBlocking {
        repo.seedIfNeeded()
        val s = repo.startSession(null)
        val se = repo.addExerciseToSession(s, exId("bench_press"), 1)
        val set = db.sessionDao().getSetsOf(se).single()
        repo.fillSet(set.id, 82.5, 6); repo.toggleSetCompleted(set.id); repo.finishSession(s)
        val rows = repo.observeAllCompletedRows().first()
        val out = ByteArrayOutputStream()
        ReportExporter(context).writeCsv(rows, com.gymtrack.domain.OneRepMaxFormula.EPLEY, out)
        val csv = out.toString(Charsets.UTF_8.name())
        assertTrue(csv.contains("Supino reto com barra"))
        assertTrue(csv.contains(";82,5;6;495;"))
        val pdf = ByteArrayOutputStream()
        ReportExporter(context).writePdf(rows, repo.observeFinishedSessions().first(), emptyList(), com.gymtrack.domain.OneRepMaxFormula.EPLEY, 3, pdf)
        val pdfText = String(pdf.toByteArray(), Charsets.ISO_8859_1)
        assertTrue(pdfText.startsWith("%PDF-1.4"))
        assertTrue(pdfText.contains("Supino reto com barra"))
    }

    @Test fun reminderNextTrigger() {
        val wed = LocalDateTime.of(2026, 11, 18, 10, 0) // Wednesday
        assertEquals(LocalDateTime.of(2026, 11, 18, 18, 0), ReminderScheduler.nextTrigger(ReminderEntity(dayOfWeek = 3, hour = 18, minute = 0), wed))
        assertEquals(LocalDateTime.of(2026, 11, 25, 9, 0), ReminderScheduler.nextTrigger(ReminderEntity(dayOfWeek = 3, hour = 9, minute = 0), wed))
        assertEquals(LocalDateTime.of(2026, 11, 23, 7, 30), ReminderScheduler.nextTrigger(ReminderEntity(dayOfWeek = 1, hour = 7, minute = 30), wed))
    }

    @Test fun customExerciseDeleteArchivesWhenUsed() = runBlocking {
        val id = repo.saveExercise(com.gymtrack.app.data.db.ExerciseEntity(name = "Meu exercício", primaryMuscle = "CHEST", equipment = "MACHINE", isCustom = true))
        val unused = repo.saveExercise(com.gymtrack.app.data.db.ExerciseEntity(name = "Sem uso", primaryMuscle = "CHEST", equipment = "MACHINE", isCustom = true))
        val s = repo.startSession(null); repo.addExerciseToSession(s, id)
        repo.deleteExercise(id); repo.deleteExercise(unused)
        assertTrue(db.exerciseDao().get(id)!!.archived)
        assertNull(db.exerciseDao().get(unused))
        assertFalse(repo.observeExercises().first().any { it.id == id })
        assertNotNull(repo.getSessionDetail(s)!!.exercises.single())
    }
}

package com.gymtrack.app.data.repo

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.gymtrack.app.data.db.BodyMeasurementEntity
import com.gymtrack.app.data.db.ExerciseEntity
import com.gymtrack.app.data.db.GoalEntity
import com.gymtrack.app.data.db.GymDatabase
import com.gymtrack.app.data.db.ProgressPhotoEntity
import com.gymtrack.app.data.db.ReminderEntity
import com.gymtrack.app.data.db.RoutineEntity
import com.gymtrack.app.data.db.RoutineExerciseEntity
import com.gymtrack.app.data.db.RoutineVersionEntity
import com.gymtrack.app.data.db.SessionEntity
import com.gymtrack.app.data.db.SessionExerciseEntity
import com.gymtrack.app.data.db.SetEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupData(
    val format: String = FORMAT,
    val version: Int = 1,
    val createdAt: Long,
    val exercises: List<ExerciseEntity>,
    val routines: List<RoutineEntity>,
    val routineExercises: List<RoutineExerciseEntity>,
    val routineVersions: List<RoutineVersionEntity>,
    val sessions: List<SessionEntity>,
    val sessionExercises: List<SessionExerciseEntity>,
    val sets: List<SetEntity>,
    val measurements: List<BodyMeasurementEntity>,
    val photos: List<ProgressPhotoEntity>,
    val goals: List<GoalEntity>,
    val reminders: List<ReminderEntity>,
) {
    companion object { const val FORMAT = "gymtrack-backup" }
}

data class BackupInfo(val file: File, val createdAt: Long, val sizeBytes: Long)

class BackupManager(
    private val context: Context,
    private val db: GymDatabase,
    private val photosDir: () -> File,
    private val exerciseImagesDir: () -> File = { File(context.filesDir, "exercise_images").apply { mkdirs() } },
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val autoDir get() = File(context.filesDir, "backups").apply { mkdirs() }

    suspend fun snapshot(): BackupData {
        val e = db.exerciseDao(); val r = db.routineDao(); val s = db.sessionDao(); val b = db.bodyDao()
        return BackupData(
            createdAt = System.currentTimeMillis(),
            exercises = e.getAll(), routines = r.getAll(), routineExercises = r.getAllExercises(), routineVersions = r.getAllVersions(),
            sessions = s.getAll(), sessionExercises = s.getAllExercises(), sets = s.getAllSets(),
            measurements = b.getAllMeasurements(), photos = b.getAllPhotos(), goals = db.goalDao().getAll(), reminders = db.reminderDao().getAll(),
        )
    }

    fun encode(data: BackupData): String = json.encodeToString(data)

    fun decode(text: String): BackupData {
        val data = runCatching { json.decodeFromString<BackupData>(text) }.getOrElse { throw IllegalArgumentException("Arquivo de backup inválido ou corrompido.") }
        require(data.format == BackupData.FORMAT) { "Este arquivo não é um backup do GymTrack." }
        // Referential sanity checks before touching the database.
        val exIds = data.exercises.map { it.id }.toSet()
        val rIds = data.routines.map { it.id }.toSet()
        val sIds = data.sessions.map { it.id }.toSet()
        val seIds = data.sessionExercises.map { it.id }.toSet()
        require(data.routineExercises.all { it.routineId in rIds && it.exerciseId in exIds }) { "Backup inconsistente (fichas)." }
        require(data.sessionExercises.all { it.sessionId in sIds && it.exerciseId in exIds }) { "Backup inconsistente (sessões)." }
        require(data.sets.all { it.sessionExerciseId in seIds }) { "Backup inconsistente (séries)." }
        return data
    }

    /** Full export as a .zip with backup.json and the progress photos. */
    suspend fun exportZip(out: OutputStream) = withContext(Dispatchers.IO) {
        val data = snapshot()
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json")); zip.write(encode(data).toByteArray()); zip.closeEntry()
            data.photos.forEach { p ->
                val f = File(photosDir(), p.fileName)
                if (f.exists()) { zip.putNextEntry(ZipEntry("photos/${p.fileName}")); f.inputStream().use { it.copyTo(zip) }; zip.closeEntry() }
            }
            data.exercises.flatMap { listOfNotNull(it.imageStart, it.imageEnd) }.distinct().forEach { name ->
                val f = File(exerciseImagesDir(), name)
                if (f.exists()) { zip.putNextEntry(ZipEntry("exercise_images/$name")); f.inputStream().use { it.copyTo(zip) }; zip.closeEntry() }
            }
        }
    }

    suspend fun exportToUri(uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { exportZip(it) } ?: error("Não foi possível gravar o arquivo.")
    }

    /** Reads a .zip (or plain .json) backup, validates it and replaces all data in one transaction. */
    suspend fun importFrom(input: InputStream): BackupData = withContext(Dispatchers.IO) {
        val bytes = input.readBytes()
        var jsonText: String? = null
        val photos = mutableMapOf<String, ByteArray>()
        val exImages = mutableMapOf<String, ByteArray>()
        if (bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
            ZipInputStream(bytes.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory) {
                        val buf = ByteArrayOutputStream(); zip.copyTo(buf)
                        when {
                            name == "backup.json" -> jsonText = buf.toString(Charsets.UTF_8.name())
                            name.startsWith("photos/") -> {
                                val fileName = name.removePrefix("photos/")
                                // Guard against path traversal.
                                if (safeName(fileName)) photos[fileName] = buf.toByteArray()
                            }
                            name.startsWith("exercise_images/") -> {
                                val fileName = name.removePrefix("exercise_images/")
                                if (safeName(fileName)) exImages[fileName] = buf.toByteArray()
                            }
                        }
                    }
                    entry = zip.nextEntry
                }
            }
        } else jsonText = bytes.toString(Charsets.UTF_8)
        val data = decode(jsonText ?: throw IllegalArgumentException("backup.json não encontrado no arquivo."))
        // Safety copy of the current data before replacing it.
        writeAutoBackup(prefix = "pre-import")
        restore(data)
        photos.forEach { (name, content) -> File(photosDir(), name).writeBytes(content) }
        exImages.forEach { (name, content) -> File(exerciseImagesDir(), name).writeBytes(content) }
        data
    }

    private fun safeName(n: String) = n.isNotBlank() && !n.contains('/') && !n.contains('\\') && !n.contains("..")

    suspend fun importFromUri(uri: Uri): BackupData =
        context.contentResolver.openInputStream(uri)?.use { importFrom(it) } ?: error("Não foi possível abrir o arquivo.")

    suspend fun restore(data: BackupData) = db.withTransaction {
        val d = db.backupDao()
        d.clearSets(); d.clearSessionExercises(); d.clearSessions(); d.clearVersions(); d.clearRoutineExercises(); d.clearRoutines()
        d.clearGoals(); d.clearPhotos(); d.clearMeasurements(); d.clearReminders(); d.clearExercises()
        d.exercises(data.exercises); d.routines(data.routines); d.routineExercises(data.routineExercises); d.versions(data.routineVersions)
        d.sessions(data.sessions); d.sessionExercises(data.sessionExercises); d.sets(data.sets)
        d.measurements(data.measurements); d.photos(data.photos); d.goals(data.goals); d.reminders(data.reminders)
    }

    /** Local automatic backup (JSON only, photos stay in place). Keeps the newest [keep] files. */
    suspend fun writeAutoBackup(prefix: String = "auto", keep: Int = 7): File = withContext(Dispatchers.IO) {
        val f = File(autoDir, "$prefix-${System.currentTimeMillis()}.json")
        val tmp = File(autoDir, f.name + ".tmp")
        tmp.writeText(encode(snapshot()))
        tmp.renameTo(f)
        autoDir.listFiles { x -> x.name.startsWith("$prefix-") && x.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }?.drop(keep)?.forEach { it.delete() }
        f
    }

    fun listAutoBackups(): List<BackupInfo> =
        autoDir.listFiles { x -> x.name.endsWith(".json") }?.map { BackupInfo(it, it.lastModified(), it.length()) }?.sortedByDescending { it.createdAt }.orEmpty()

    suspend fun restoreAuto(file: File): BackupData = withContext(Dispatchers.IO) {
        val data = decode(file.readText())
        if (!file.name.startsWith("pre-import")) writeAutoBackup(prefix = "pre-import")
        restore(data); data
    }
}

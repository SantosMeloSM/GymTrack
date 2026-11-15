package com.gymtrack.app.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.gymtrack.app.data.db.BodyMeasurementEntity
import com.gymtrack.app.data.db.GoalEntity
import com.gymtrack.app.data.db.GymDatabase
import com.gymtrack.app.data.db.ProgressPhotoEntity
import com.gymtrack.app.data.db.ReminderEntity
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BodyRepository(private val context: Context, private val db: GymDatabase) {
    private val dao = db.bodyDao()
    val photosDir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    fun observeMeasurements() = dao.observeMeasurements()
    suspend fun getMeasurement(id: Long) = dao.getMeasurement(id)
    suspend fun saveMeasurement(m: BodyMeasurementEntity) = dao.upsertMeasurement(m)
    suspend fun deleteMeasurement(id: Long) = dao.deleteMeasurement(id)

    fun observePhotos() = dao.observePhotos()
    fun photoFile(p: ProgressPhotoEntity) = File(photosDir, p.fileName)

    /** Copies (and downsizes) a picked image into private storage. */
    suspend fun addPhoto(uri: Uri, date: Long, pose: String, notes: String): Long = withContext(Dispatchers.IO) {
        val name = "${UUID.randomUUID()}.jpg"
        val out = File(photosDir, name)
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: error("Não foi possível abrir a imagem")
        var sample = 1
        while (bounds.outWidth / sample > 2400 || bounds.outHeight / sample > 2400) sample *= 2
        val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            ?: error("Imagem inválida")
        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)
        val finalBmp = if (rotation != 0) Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rotation.toFloat()) }, true) else bmp
        out.outputStream().use { finalBmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        dao.insertPhoto(ProgressPhotoEntity(date = date, fileName = name, pose = pose, notes = notes))
    }

    suspend fun deletePhoto(p: ProgressPhotoEntity) {
        dao.deletePhoto(p)
        withContext(Dispatchers.IO) { photoFile(p).delete() }
    }
}

class GoalRepository(db: GymDatabase) {
    private val dao = db.goalDao()
    fun observeAll() = dao.observeAll()
    suspend fun save(g: GoalEntity) = dao.upsert(g)
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun getAll() = dao.getAll()
}

class ReminderRepository(db: GymDatabase) {
    private val dao = db.reminderDao()
    fun observeAll() = dao.observeAll()
    suspend fun getAll() = dao.getAll()
    suspend fun get(id: Long) = dao.get(id)
    suspend fun save(r: ReminderEntity) = dao.upsert(r)
    suspend fun delete(id: Long) = dao.delete(id)
}

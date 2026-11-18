package com.gymtrack.app.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.gymtrack.app.data.db.ExerciseEntity
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One entry of assets/exercises/manifest.json (bundled, offline). */
@Serializable
data class MediaEntry(
    val kind: String,
    val frames: Int,
    val captions: List<String>,
    val source: String,
    val sourceId: String? = null,
    val license: String,
    val swapped: Boolean = false,
    /** Photographer/author, required for attribution (e.g. Wikimedia Commons CC BY / CC BY-SA). */
    val author: String? = null,
    /** Link to the original file page, for attribution. */
    val sourceUrl: String? = null,
)

/** Resolved demonstration for one exercise. [frames] are Coil models (asset URI strings or Files). */
data class ExerciseDemo(
    val frames: List<Any>,
    val captions: List<String>,
    val isCustom: Boolean,
    val kind: String,
    val credit: String?,
) {
    val hasImages get() = frames.isNotEmpty()
    val animatable get() = frames.size >= 2
}

/**
 * Exercise images: bundled library (assets, available offline) and user images stored in the
 * app's private storage. User images always take precedence.
 */
class ExerciseMediaRepository(private val context: Context) {
    val imagesDir: File get() = File(context.filesDir, "exercise_images").apply { mkdirs() }

    private val json = Json { ignoreUnknownKeys = true }

    val manifest: Map<String, MediaEntry> by lazy {
        runCatching {
            context.assets.open("$ASSET_DIR/manifest.json").use { json.decodeFromString<Map<String, MediaEntry>>(it.readBytes().decodeToString()) }
        }.getOrDefault(emptyMap())
    }

    fun assetUri(key: String, frame: Int) = "file:///android_asset/$ASSET_DIR/$key/$frame.webp"

    fun demoFor(e: ExerciseEntity): ExerciseDemo {
        val custom = listOfNotNull(e.imageStart, e.imageEnd).map { File(imagesDir, it) }.filter { it.exists() }
        if (custom.isNotEmpty()) {
            val caps = if (custom.size == 2) listOf("Posição inicial", "Posição final") else listOf(if (e.imageStart != null) "Posição inicial" else "Posição final")
            return ExerciseDemo(custom, caps, isCustom = true, kind = "custom", credit = "Imagem adicionada por você")
        }
        val key = e.seedKey
        val m = key?.let { manifest[it] } ?: return ExerciseDemo(emptyList(), emptyList(), false, "none", null)
        val credit = when {
            m.kind == "photo" && m.author != null -> "Foto: ${m.author} · ${m.license} · ${m.source}"
            m.kind == "photo" -> "Foto: ${m.source} · ${m.license}"
            else -> m.source
        }
        return ExerciseDemo((0 until m.frames).map { assetUri(key, it) }, m.captions, isCustom = false, kind = m.kind, credit = credit)
    }

    /** Copies (downscaled, EXIF-rotated) an image into private storage and returns its file name. */
    suspend fun importImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: error("Não foi possível abrir a imagem")
        var sample = 1
        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
        val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: error("Imagem inválida")
        val rotation = runCatching { resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0 }.getOrDefault(0)
        val final = if (rotation != 0) Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rotation.toFloat()) }, true) else bmp
        val name = "${UUID.randomUUID()}.jpg"
        File(imagesDir, name).outputStream().use { final.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        name
    }

    fun delete(name: String?) { if (name != null) File(imagesDir, name).delete() }

    /** Temporary file the camera app writes into (shared through the FileProvider). */
    fun newCameraFile(): File = File(File(context.cacheDir, "camera").apply { mkdirs() }, "capture-${System.currentTimeMillis()}.jpg")

    companion object { const val ASSET_DIR = "exercises" }
}

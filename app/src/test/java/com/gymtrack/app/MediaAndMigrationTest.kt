package com.gymtrack.app

import android.content.Context
import android.graphics.BitmapFactory
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.app.data.db.ExerciseEntity
import com.gymtrack.app.data.db.GymDatabase
import com.gymtrack.app.data.db.MIGRATION_1_2
import com.gymtrack.app.data.repo.BackupManager
import com.gymtrack.app.data.repo.ExerciseMediaRepository
import com.gymtrack.app.data.repo.WorkoutRepository
import com.gymtrack.app.data.seed.ExerciseMistakes
import com.gymtrack.app.data.seed.ExerciseSeed
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MediaAuditTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun everyLibraryExerciseHasValidOfflineImages() {
        val media = ExerciseMediaRepository(context)
        val seeds = ExerciseSeed.all
        assertEquals("manifest covers exactly the library", seeds.map { it.seedKey!! }.toSet(), media.manifest.keys)
        val hashes = mutableMapOf<String, String>()
        val problems = mutableListOf<String>()
        seeds.forEach { e ->
            val key = e.seedKey!!
            val entry = media.manifest.getValue(key)
            assertTrue("$key needs >= 2 frames", entry.frames >= 2)
            assertEquals("$key captions", entry.frames, entry.captions.size)
            val demo = media.demoFor(e)
            assertEquals(entry.frames, demo.frames.size)
            repeat(entry.frames) { i ->
                val path = "exercises/$key/$i.webp"
                val bytes = runCatching { context.assets.open(path).use { it.readBytes() } }.getOrNull()
                if (bytes == null) { problems += "missing $path"; return@repeat }
                if (String(bytes, 0, 4) != "RIFF" || String(bytes, 8, 4) != "WEBP") problems += "not webp $path"
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp == null || bmp.width != 720 || bmp.height != 480) problems += "decode/size $path ${bmp?.width}x${bmp?.height}"
                val h = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                hashes.put(h, path)?.let { other -> problems += "duplicate image $path == $other" }
                if (bytes.size > 120_000) problems += "too large $path ${bytes.size}"
            }
            assertTrue("$key mistakes", ExerciseMistakes.byKey[key].orEmpty().isNotBlank())
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test fun customImagesOverrideAndExerciseWithoutImagesIsHandled() {
        val media = ExerciseMediaRepository(context)
        val f = File(media.imagesDir, "test.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val custom = ExerciseEntity(name = "X", primaryMuscle = "CHEST", equipment = "OTHER", isCustom = true, imageStart = f.name)
        val d = media.demoFor(custom)
        assertTrue(d.isCustom); assertEquals(1, d.frames.size)
        assertFalse(media.demoFor(custom.copy(imageStart = null)).hasImages)
        val seeded = ExerciseSeed.all.first().copy(imageEnd = f.name)
        assertTrue(media.demoFor(seeded).isCustom)
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationAndUpgradeTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** Builds a real version-1 database from the exported schema, fills it and migrates with the app's builder settings. */
    @Test fun migration1to2KeepsAllData() = runBlocking {
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val schema = Json.parseToJsonElement(File("schemas/com.gymtrack.app.data.db.GymDatabase/1.json").readText()).jsonObject["database"]!!.jsonObject
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    schema["entities"]!!.jsonArray.forEach { ent ->
                        val o = ent.jsonObject; val table = o["tableName"]!!.jsonPrimitive.content
                        db.execSQL(o["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                        o["indices"]?.jsonArray?.forEach { idx -> db.execSQL(idx.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table)) }
                    }
                    schema["setupQueries"]!!.jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
                }
                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            }).build(),
        )
        helper.writableDatabase.apply {
            execSQL("INSERT INTO exercises (id,name,primaryMuscle,secondaryMuscles,equipment,description,tips,isCustom,isFavorite,defaultRestSec,archived,seedKey) VALUES (1,'Agachamento goblet','QUADS','','DUMBBELL','Segure um halter junto ao peito e agache mantendo o tronco ereto.','',0,1,105,0,'goblet_squat')")
            execSQL("INSERT INTO exercises (id,name,primaryMuscle,secondaryMuscles,equipment,description,tips,isCustom,isFavorite,defaultRestSec,archived,seedKey) VALUES (2,'Meu exercício','CHEST','','OTHER','Minha descrição','',1,0,60,0,NULL)")
            execSQL("INSERT INTO exercises (id,name,primaryMuscle,secondaryMuscles,equipment,description,tips,isCustom,isFavorite,defaultRestSec,archived,seedKey) VALUES (3,'Rosca inversa','FOREARMS','','EZ_BAR','Minha versão editada','',0,0,75,0,'reverse_curl')")
            execSQL("INSERT INTO sessions (id,routineId,name,startedAt,endedAt,pausedAt,pausedAccumMs,status,notes,restEndsAt,restTotalSec,restExerciseName) VALUES (10,NULL,'Treino antigo',1000,2000,NULL,0,'FINISHED','',NULL,0,NULL)")
            execSQL("INSERT INTO session_exercises (id,sessionId,exerciseId,position,restSec,notes,repsMin,repsMax) VALUES (20,10,1,0,90,'',8,12)")
            execSQL("INSERT INTO sets (id,sessionExerciseId,position,weight,reps,isWarmup,completed,completedAt,rpe,rir) VALUES (30,20,0,32.0,10,0,1,1500,NULL,NULL)")
            version = 1
        }
        helper.close()

        val db = Room.databaseBuilder(context, GymDatabase::class.java, name).addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()
        val ex = db.exerciseDao().get(1)!!
        assertTrue("favorite kept", ex.isFavorite)
        assertEquals("", ex.mistakes); assertNull(ex.imageStart)
        val sets = db.sessionDao().getSets(10)
        assertEquals(32.0, sets.single().weight!!, 0.0)
        assertEquals(1, db.sessionDao().getAll().size)

        // Upgrading the seed fills new content without touching user edits.
        WorkoutRepository(db).seedIfNeeded()
        val goblet = db.exerciseDao().get(1)!!
        assertEquals("KETTLEBELL", goblet.equipment)
        assertTrue(goblet.description.contains("kettlebell"))
        assertTrue(goblet.mistakes.isNotBlank())
        assertTrue("favorite still kept", goblet.isFavorite)
        val reverse = db.exerciseDao().get(3)!!
        assertEquals("BARBELL", reverse.equipment)
        assertEquals("user edit preserved", "Minha versão editada", reverse.description)
        assertEquals("Minha descrição", db.exerciseDao().get(2)!!.description)
        assertEquals(ExerciseSeed.all.size + 1, db.exerciseDao().count())
        db.close()
    }

    @Test fun backupIncludesCustomExerciseImages() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, GymDatabase::class.java).allowMainThreadQueries().build()
        val imgDir = File(context.filesDir, "ex-img-test").apply { mkdirs() }
        File(imgDir, "a.jpg").writeBytes(byteArrayOf(9, 8, 7))
        val repo = WorkoutRepository(db)
        repo.saveExercise(ExerciseEntity(name = "Com foto", primaryMuscle = "CHEST", equipment = "OTHER", isCustom = true, imageStart = "a.jpg"))
        val backup = BackupManager(context, db, { File(context.filesDir, "p") .apply { mkdirs() } }, { imgDir })
        val out = ByteArrayOutputStream(); backup.exportZip(out)
        File(imgDir, "a.jpg").delete()
        backup.importFrom(out.toByteArray().inputStream())
        assertTrue(File(imgDir, "a.jpg").exists())
        assertNotNull(db.exerciseDao().getAll().single { it.name == "Com foto" }.imageStart)
        db.close()
    }
}

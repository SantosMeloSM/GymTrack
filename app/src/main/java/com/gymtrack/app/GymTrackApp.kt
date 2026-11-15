package com.gymtrack.app

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import com.gymtrack.app.data.db.GymDatabase
import com.gymtrack.app.data.repo.BackupManager
import com.gymtrack.app.data.repo.BodyRepository
import com.gymtrack.app.data.repo.ExerciseMediaRepository
import com.gymtrack.app.data.repo.GoalRepository
import com.gymtrack.app.data.repo.ReminderRepository
import com.gymtrack.app.data.repo.ReportExporter
import com.gymtrack.app.data.repo.SettingsRepository
import com.gymtrack.app.data.repo.WorkoutRepository
import com.gymtrack.app.notify.Channels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual dependency container (no reflection-based DI, small and fast). */
class AppContainer(context: Context, val db: GymDatabase = GymDatabase.build(context)) {
    val appContext: Context = context.applicationContext
    val settings = SettingsRepository(appContext)
    val workouts = WorkoutRepository(db)
    val body = BodyRepository(appContext, db)
    val goals = GoalRepository(db)
    val reminders = ReminderRepository(db)
    val media = ExerciseMediaRepository(appContext)
    val backup = BackupManager(appContext, db, { body.photosDir }, { media.imagesDir })
    val reports = ReportExporter(appContext)
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

class GymTrackApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Channels.create(this)
        container.appScope.launch { container.workouts.seedIfNeeded() }
    }

    /** Images are local (assets/private files): memory cache only, sized for smooth scrolling. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.15).build() }
        .diskCache(null)
        .crossfade(true)
        .respectCacheHeaders(false)
        .build()

    /** Used by tests to inject an in-memory database. */
    fun replaceContainer(c: AppContainer) { container = c }
}

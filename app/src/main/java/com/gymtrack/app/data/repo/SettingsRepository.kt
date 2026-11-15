package com.gymtrack.app.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gymtrack.domain.OneRepMaxFormula
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) { DARK("Escuro"), LIGHT("Claro"), SYSTEM("Seguir o sistema") }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val defaultRestSec: Int = 90,
    val autoStartRest: Boolean = true,
    val vibrate: Boolean = true,
    val restNotifications: Boolean = true,
    val weeklyTarget: Int = 3,
    val oneRmFormula: OneRepMaxFormula = OneRepMaxFormula.EPLEY,
    val showRpe: Boolean = false,
    val weightStep: Double = 2.5,
    val keepScreenOn: Boolean = true,
    val autoBackup: Boolean = true,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val theme = stringPreferencesKey("theme")
        val rest = intPreferencesKey("default_rest")
        val autoRest = booleanPreferencesKey("auto_rest")
        val vibrate = booleanPreferencesKey("vibrate")
        val restNotif = booleanPreferencesKey("rest_notif")
        val weekly = intPreferencesKey("weekly_target")
        val formula = stringPreferencesKey("orm_formula")
        val rpe = booleanPreferencesKey("show_rpe")
        val step = doublePreferencesKey("weight_step")
        val screenOn = booleanPreferencesKey("keep_screen_on")
        val autoBackup = booleanPreferencesKey("auto_backup")
        val onboarded = booleanPreferencesKey("onboarded")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == p[K.theme] } ?: ThemeMode.DARK,
            defaultRestSec = p[K.rest] ?: 90,
            autoStartRest = p[K.autoRest] ?: true,
            vibrate = p[K.vibrate] ?: true,
            restNotifications = p[K.restNotif] ?: true,
            weeklyTarget = p[K.weekly] ?: 3,
            oneRmFormula = OneRepMaxFormula.entries.firstOrNull { it.name == p[K.formula] } ?: OneRepMaxFormula.EPLEY,
            showRpe = p[K.rpe] ?: false,
            weightStep = p[K.step] ?: 2.5,
            keepScreenOn = p[K.screenOn] ?: true,
            autoBackup = p[K.autoBackup] ?: true,
        )
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[K.onboarded] ?: false }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setOnboarded() = context.dataStore.edit { it[K.onboarded] = true }
    suspend fun setTheme(v: ThemeMode) = context.dataStore.edit { it[K.theme] = v.name }
    suspend fun setDefaultRest(v: Int) = context.dataStore.edit { it[K.rest] = v.coerceIn(10, 900) }
    suspend fun setAutoStartRest(v: Boolean) = context.dataStore.edit { it[K.autoRest] = v }
    suspend fun setVibrate(v: Boolean) = context.dataStore.edit { it[K.vibrate] = v }
    suspend fun setRestNotifications(v: Boolean) = context.dataStore.edit { it[K.restNotif] = v }
    suspend fun setWeeklyTarget(v: Int) = context.dataStore.edit { it[K.weekly] = v.coerceIn(1, 7) }
    suspend fun setFormula(v: OneRepMaxFormula) = context.dataStore.edit { it[K.formula] = v.name }
    suspend fun setShowRpe(v: Boolean) = context.dataStore.edit { it[K.rpe] = v }
    suspend fun setWeightStep(v: Double) = context.dataStore.edit { it[K.step] = v }
    suspend fun setKeepScreenOn(v: Boolean) = context.dataStore.edit { it[K.screenOn] = v }
    suspend fun setAutoBackup(v: Boolean) = context.dataStore.edit { it[K.autoBackup] = v }
}

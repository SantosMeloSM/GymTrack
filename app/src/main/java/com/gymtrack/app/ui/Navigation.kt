package com.gymtrack.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymtrack.app.data.repo.AppSettings
import com.gymtrack.app.ui.screens.BackupScreen
import com.gymtrack.app.ui.screens.BodyScreen
import com.gymtrack.app.ui.screens.CompareScreen
import com.gymtrack.app.ui.screens.ExerciseDetailScreen
import com.gymtrack.app.ui.screens.ExerciseEditScreen
import com.gymtrack.app.ui.screens.GoalsScreen
import com.gymtrack.app.ui.screens.HistoryScreen
import com.gymtrack.app.ui.screens.HomeScreen
import com.gymtrack.app.ui.screens.LibraryScreen
import com.gymtrack.app.ui.screens.MoreScreen
import com.gymtrack.app.ui.screens.ProgressScreen
import com.gymtrack.app.ui.screens.RemindersScreen
import com.gymtrack.app.ui.screens.RoutineEditorScreen
import com.gymtrack.app.ui.screens.RoutinesScreen
import com.gymtrack.app.ui.screens.SessionDetailScreen
import com.gymtrack.app.ui.screens.SessionSummaryScreen
import com.gymtrack.app.ui.screens.SettingsScreen
import com.gymtrack.app.ui.screens.WorkoutScreen

object Routes {
    const val HOME = "home"
    const val ROUTINES = "routines"
    const val LIBRARY = "library"
    const val PROGRESS = "progress"
    const val MORE = "more"
    const val WORKOUT = "workout"
    const val HISTORY = "history"
    const val BODY = "body"
    const val GOALS = "goals"
    const val REMINDERS = "reminders"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
    fun routine(id: Long) = "routine/$id"
    fun exercise(id: Long) = "exercise/$id"
    fun exerciseEdit(id: Long) = "exerciseEdit/$id"
    fun session(id: Long) = "session/$id"
    fun summary(id: Long) = "summary/$id"
    fun compare(a: Long, b: Long) = "compare/$a/$b"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Início", Icons.Outlined.Home),
    Tab(Routes.ROUTINES, "Treinos", Icons.AutoMirrored.Outlined.ListAlt),
    Tab(Routes.LIBRARY, "Exercícios", Icons.Outlined.FitnessCenter),
    Tab(Routes.PROGRESS, "Progresso", Icons.Outlined.Insights),
    Tab(Routes.MORE, "Mais", Icons.Outlined.MoreHoriz),
)

@Composable
fun GymTrackRoot(settings: AppSettings, nav: NavHostController = rememberNavController()) {
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = tabs.any { it.route == route }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                tabs.forEach { t ->
                    NavigationBarItem(
                        modifier = Modifier.testTag("tab_${t.route}"),
                        selected = route == t.route,
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, null) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            NavHost(nav, startDestination = Routes.HOME, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                composable(Routes.HOME) { HomeScreen(nav) }
                composable(Routes.ROUTINES) { RoutinesScreen(nav) }
                composable(Routes.LIBRARY) { LibraryScreen(nav) }
                composable(Routes.PROGRESS) { ProgressScreen(nav) }
                composable(Routes.MORE) { MoreScreen(nav) }
                composable(Routes.WORKOUT) { WorkoutScreen(nav, settings) }
                composable(Routes.HISTORY) { HistoryScreen(nav) }
                composable(Routes.BODY) { BodyScreen(nav) }
                composable(Routes.GOALS) { GoalsScreen(nav) }
                composable(Routes.REMINDERS) { RemindersScreen(nav) }
                composable(Routes.SETTINGS) { SettingsScreen(nav) }
                composable(Routes.BACKUP) { BackupScreen(nav) }
                composable("routine/{id}", listOf(navArgument("id") { type = NavType.LongType })) { RoutineEditorScreen(nav, it.arguments!!.getLong("id")) }
                composable("exercise/{id}", listOf(navArgument("id") { type = NavType.LongType })) { ExerciseDetailScreen(nav, it.arguments!!.getLong("id")) }
                composable("exerciseEdit/{id}", listOf(navArgument("id") { type = NavType.LongType })) { ExerciseEditScreen(nav, it.arguments!!.getLong("id")) }
                composable("session/{id}", listOf(navArgument("id") { type = NavType.LongType })) { SessionDetailScreen(nav, it.arguments!!.getLong("id")) }
                composable("summary/{id}", listOf(navArgument("id") { type = NavType.LongType })) { SessionSummaryScreen(nav, it.arguments!!.getLong("id")) }
                composable("compare/{a}/{b}", listOf(navArgument("a") { type = NavType.LongType }, navArgument("b") { type = NavType.LongType })) {
                    CompareScreen(nav, it.arguments!!.getLong("a"), it.arguments!!.getLong("b"))
                }
            }
        }
    }
}

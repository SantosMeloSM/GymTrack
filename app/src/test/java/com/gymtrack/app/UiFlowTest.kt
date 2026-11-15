package com.gymtrack.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.app.data.db.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class UiFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<GymTrackApp>()

    @Before fun seed() = runBlocking { app.container.workouts.seedIfNeeded() }

    private fun waitTagUnmerged(tag: String) = rule.waitUntil(5_000) {
        rule.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun back() { rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() } }

    private fun waitText(text: String, timeout: Long = 5_000) =
        rule.waitUntilAtLeastOneExists(hasText(text, substring = true), timeout)

    @Test fun createRoutineLogSetsFinishAndPersist() {
        // Treinos tab -> new routine
        rule.onNodeWithTag("tab_routines").performClick()
        rule.onNodeWithTag("new_routine").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("routine_add_exercise"), 5_000)
        // Auto-named; rename inline in the editor
        rule.onNodeWithTag("edit_routine_name").performTextReplacement("Peito e tríceps")
        rule.waitUntil(5_000) { runBlocking { app.container.db.routineDao().getAll().singleOrNull()?.name } == "Peito e tríceps" }

        // Add an exercise via search
        rule.onNodeWithTag("routine_add_exercise").performClick()
        rule.onNodeWithTag("picker_search").performTextInput("supino reto com barra")
        rule.waitUntilAtLeastOneExists(hasTestTag("pick_Supino reto com barra"), 5_000)
        rule.onNodeWithTag("pick_Supino reto com barra").performClick()
        rule.onNodeWithTag("picker_confirm").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("routine_item_Supino reto com barra"), 5_000)
        rule.onNodeWithTag("sets_Supino reto com barra").assertIsDisplayed()

        // Start workout
        rule.onNodeWithTag("start_routine").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("check_Supino reto com barra_1"), 5_000)

        // Set 1: weight + reps, one tap to complete
        rule.onNodeWithTag("weight_Supino reto com barra_1").performTextInput("60")
        rule.onNodeWithTag("reps_Supino reto com barra_1").performTextInput("10")
        rule.onNodeWithTag("check_Supino reto com barra_1").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("rest_bar"), 5_000)
        rule.onNodeWithTag("skip_rest").performClick()

        // Set 2 completed with one tap after typing reps (weight pre-filled? none yet -> type)
        rule.onNodeWithTag("weight_Supino reto com barra_2").performTextInput("62,5")
        rule.onNodeWithTag("reps_Supino reto com barra_2").performTextInput("8")
        rule.onNodeWithTag("check_Supino reto com barra_2").performClick()

        // Data must already be persisted while the session is active (crash/close protection)
        rule.waitForIdle()
        val live = runBlocking { app.container.workouts.getActiveSession()!!.let { s -> app.container.db.sessionDao().getSets(s.id) } }
        assertEquals(listOf(60.0 to 10, 62.5 to 8), live.filter { it.completed }.map { it.weight to it.reps })
        assertEquals(1, live.count { !it.completed })

        // Pause/resume
        rule.onNodeWithTag("pause_button").performClick()
        waitText("Treino pausado")
        rule.onNodeWithText("Continuar").performClick()

        // Finish
        rule.onNodeWithTag("finish_button").performClick()
        rule.onAllNodesWithText("Concluir")[1].performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("summary_done"), 5_000)
        waitText("Peito e tríceps")

        val sessions = runBlocking { app.container.workouts.observeFinishedSessions().first() }
        assertEquals(1, sessions.size)
        assertEquals(2, sessions.single().setCount)
        assertEquals(60.0 * 10 + 62.5 * 8, sessions.single().volume, 0.001)
        val stored = runBlocking { app.container.db.sessionDao().get(sessions.single().id)!! }
        assertEquals(SessionStatus.FINISHED, stored.status)

        rule.onNodeWithTag("summary_done").performClick()
        // Back on home, last session listed
        rule.waitUntilAtLeastOneExists(hasText("Últimos treinos", substring = true, ignoreCase = true), 5_000)

        // Progress tab shows data; exercise tab lists the record
        rule.onNodeWithTag("tab_progress").performClick()
        waitText("Frequência semanal")
        rule.waitForIdle()
        // Robolectric occasionally drops synthetic touches right after a NavHost crossfade; invoke the click action directly.
        rule.onNodeWithTag("progress_tab_1").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onNodeWithTag("progress_tab_1").assertIsSelected()
        rule.onNodeWithText("Máx 62,5 kg × 8", substring = true).assertExists()
    }

    @Test fun importTemplateAndStartFromHome() {
        rule.onNodeWithTag("tab_routines").performClick()
        rule.onNodeWithTag("templates_button").performClick()
        rule.onNodeWithTag("template_ppl").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("routine_Push"), 5_000)
        rule.onNodeWithTag("tab_home").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("start_suggested"), 5_000)
        rule.onNodeWithTag("start_suggested").performClick()
        // The routine is pre-built: 6 exercises, first card visible
        rule.waitUntilAtLeastOneExists(hasTestTag("exercise_block_Supino reto com barra"), 5_000)
        val active = runBlocking { app.container.workouts.getActiveSession() }!!
        assertEquals("Push", active.name)
        assertEquals(6, runBlocking { app.container.db.sessionDao().getExercises(active.id).size })
        // completing without reps shows a hint instead of saving an empty set
        rule.onNodeWithTag("check_Supino reto com barra_1").performClick()
        waitText("Informe as repetições")
        // Leave workout: it stays active and can be resumed from Home
        back()
        rule.waitUntilAtLeastOneExists(hasTestTag("resume_card"), 5_000)
    }

    @Test fun everyScreenOpens() {
        val checks = listOf(
            "tab_library" to "Abdominal",
            "tab_progress" to "Sem dados ainda",
            "tab_more" to "Evolução corporal",
        )
        checks.forEach { (tag, text) -> rule.onNodeWithTag(tag).performClick(); waitText(text) }
        listOf("Evolução corporal" to "Nenhuma medição", "Metas de desempenho" to "Nenhuma meta", "Lembretes de treino" to "Nenhum lembrete", "Backup e exportação" to "Exportar backup", "Configurações" to "Tema").forEach { (row, expect) ->
            rule.onNodeWithText(row).performClick(); waitText(expect)
            back()
            waitText("Evolução corporal")
        }
        // Exercise detail from library
        rule.onNodeWithTag("tab_library").performClick()
        rule.onNodeWithTag("library_search").performTextInput("agachamento livre")
        waitText("Agachamento livre")
        rule.onAllNodesWithText("Agachamento livre")[0].performClick()
        waitText("Execução")
        // Body measurement saved and charted
        back()
        rule.onNodeWithTag("tab_more").performClick()
        rule.onNodeWithText("Evolução corporal").performClick()
        rule.onNodeWithTag("add_measurement").performClick()
        rule.onNodeWithTag("measure_WEIGHT").performTextInput("81,4")
        rule.onNodeWithTag("save_measurement").performClick()
        waitText("81,4 kg")
        assertTrue(runBlocking { app.container.db.bodyDao().getAllMeasurements().single().weight } == 81.4)
        // Theme switch persists
        back()
        rule.onNodeWithText("Configurações").performClick()
        rule.onNodeWithTag("theme_LIGHT").performClick()
        rule.waitUntil(5_000) { runBlocking { app.container.settings.current().themeMode.name } == "LIGHT" }
    }

    @Test fun imagesInLibraryDetailAndWorkout() {
        // A. Library: every visible card has a thumbnail
        rule.onNodeWithTag("tab_library").performClick()
        waitTagUnmerged("thumb_Abdominal máquina")
        rule.onNodeWithTag("library_search").performTextInput("rosca direta com barra")
        waitTagUnmerged("thumb_Rosca direta com barra")
        // B. Detail: large demo, play/expand controls, muscles and common mistakes
        rule.onNodeWithTag("lib_Rosca direta com barra").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("demo_Rosca direta com barra"), 5_000)
        rule.onNodeWithTag("demo_play").assertExists()
        rule.onNodeWithText("Músculos trabalhados").assertExists()
        rule.onNodeWithTag("demo_expand").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("demo_fullscreen"), 5_000)
        rule.onNodeWithTag("demo_close").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Erros comuns")).performScrollTo().assertExists()
        back()

        // C. Workout: thumbnail opens the demo sheet without losing logged values
        rule.onNodeWithTag("tab_routines").performClick()
        rule.onNodeWithTag("templates_button").performClick()
        rule.onNodeWithTag("template_ppl").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("start_Push"), 5_000)
        rule.onNodeWithTag("start_Push").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("check_Supino reto com barra_1"), 5_000)
        rule.onNodeWithTag("weight_Supino reto com barra_1").performTextInput("70")
        rule.onNodeWithTag("reps_Supino reto com barra_1").performTextInput("9")
        rule.onAllNodesWithTag("thumb_Supino reto com barra")[0].performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag("demo_sheet"), 5_000)
        rule.onNodeWithTag("demo_Supino reto com barra").assertExists()
        rule.onNodeWithTag("back_to_sets").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("weight_Supino reto com barra_1").assertTextEquals("70")
        rule.onNodeWithTag("reps_Supino reto com barra_1").assertTextEquals("9")
        rule.onNodeWithTag("check_Supino reto com barra_1").performClick()
        rule.waitForIdle()
        val done = runBlocking { app.container.workouts.getActiveSession()!!.let { s -> app.container.db.sessionDao().getSets(s.id).filter { it.completed } } }
        assertEquals(listOf(70.0 to 9), done.map { it.weight to it.reps })
    }
}

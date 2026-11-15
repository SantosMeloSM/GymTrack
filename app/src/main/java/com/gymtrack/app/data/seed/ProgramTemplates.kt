package com.gymtrack.app.data.seed

/** A planned exercise in a template, referenced by seed key. */
data class TemplateItem(val key: String, val sets: Int, val repsMin: Int, val repsMax: Int, val rest: Int? = null)
data class TemplateDay(val name: String, val items: List<TemplateItem>)
data class ProgramTemplate(val id: String, val name: String, val description: String, val days: List<TemplateDay>)

/**
 * Starting-point programs. Importing one creates normal, fully editable routines;
 * nothing here is treated as user data until the user imports it.
 */
object ProgramTemplates {
    private fun t(key: String, sets: Int, min: Int, max: Int, rest: Int? = null) = TemplateItem(key, sets, min, max, rest)

    val all = listOf(
        ProgramTemplate(
            "ppl", "Push / Pull / Legs", "3 treinos: empurrar, puxar e pernas. Pode ser feito 3 ou 6x por semana.",
            listOf(
                TemplateDay("Push", listOf(t("bench_press", 4, 6, 10), t("incline_db_press", 3, 8, 12), t("db_shoulder_press", 3, 8, 12), t("lateral_raise", 4, 12, 15), t("triceps_pushdown", 3, 10, 12), t("overhead_triceps_cable", 3, 10, 12))),
                TemplateDay("Pull", listOf(t("pull_up", 3, 6, 10), t("barbell_row", 4, 6, 10), t("seated_cable_row", 3, 10, 12), t("face_pull", 3, 12, 15), t("barbell_curl", 3, 8, 12), t("hammer_curl", 3, 10, 12))),
                TemplateDay("Legs", listOf(t("back_squat", 4, 6, 10), t("romanian_deadlift", 3, 8, 10), t("leg_press", 3, 10, 12), t("leg_extension", 3, 12, 15), t("lying_leg_curl", 3, 10, 12), t("standing_calf_raise", 4, 10, 15))),
            ),
        ),
        ProgramTemplate(
            "abc", "ABC clássico", "A: peito e tríceps · B: costas e bíceps · C: pernas e ombros.",
            listOf(
                TemplateDay("A — Peito e tríceps", listOf(t("bench_press", 4, 8, 10), t("incline_db_press", 3, 8, 12), t("db_fly", 3, 10, 12), t("cable_crossover", 3, 12, 15), t("triceps_pushdown", 3, 10, 12), t("skull_crusher", 3, 10, 12))),
                TemplateDay("B — Costas e bíceps", listOf(t("lat_pulldown", 4, 8, 12), t("seated_cable_row", 3, 8, 12), t("db_row", 3, 8, 12), t("straight_arm_pulldown", 3, 12, 15), t("barbell_curl", 3, 8, 12), t("hammer_curl", 3, 10, 12))),
                TemplateDay("C — Pernas e ombros", listOf(t("back_squat", 4, 6, 10), t("leg_press", 3, 10, 12), t("leg_extension", 3, 12, 15), t("lying_leg_curl", 3, 10, 12), t("overhead_press", 3, 6, 10), t("lateral_raise", 4, 12, 15), t("standing_calf_raise", 4, 10, 15))),
            ),
        ),
        ProgramTemplate(
            "abcd", "ABCD", "A: peito · B: costas · C: pernas · D: ombros e braços.",
            listOf(
                TemplateDay("A — Peito", listOf(t("bench_press", 4, 6, 10), t("incline_bench_press", 3, 8, 10), t("machine_chest_press", 3, 10, 12), t("pec_deck", 3, 12, 15), t("dips_chest", 3, 8, 12))),
                TemplateDay("B — Costas", listOf(t("deadlift", 3, 4, 6), t("lat_pulldown", 4, 8, 12), t("tbar_row", 3, 8, 10), t("machine_row", 3, 10, 12), t("shrug_db", 3, 10, 15))),
                TemplateDay("C — Pernas", listOf(t("back_squat", 4, 6, 10), t("hack_squat", 3, 8, 12), t("romanian_deadlift", 3, 8, 10), t("leg_extension", 3, 12, 15), t("seated_leg_curl", 3, 10, 12), t("seated_calf_raise", 4, 12, 15))),
                TemplateDay("D — Ombros e braços", listOf(t("db_shoulder_press", 4, 8, 12), t("lateral_raise", 4, 12, 15), t("reverse_pec_deck", 3, 12, 15), t("ez_curl", 3, 8, 12), t("incline_db_curl", 3, 10, 12), t("triceps_rope", 3, 10, 12), t("db_overhead_ext", 3, 10, 12))),
            ),
        ),
        ProgramTemplate(
            "upper_lower", "Upper / Lower", "Superiores e inferiores alternados, 4x por semana.",
            listOf(
                TemplateDay("Upper", listOf(t("bench_press", 4, 6, 8), t("barbell_row", 4, 6, 8), t("db_shoulder_press", 3, 8, 12), t("lat_pulldown", 3, 8, 12), t("db_curl", 2, 10, 12), t("triceps_pushdown", 2, 10, 12))),
                TemplateDay("Lower", listOf(t("back_squat", 4, 6, 8), t("romanian_deadlift", 3, 8, 10), t("bulgarian_split_squat", 3, 8, 12), t("lying_leg_curl", 3, 10, 12), t("standing_calf_raise", 4, 10, 15), t("hanging_leg_raise", 3, 10, 15))),
            ),
        ),
        ProgramTemplate(
            "full_body", "Full Body 3x", "Corpo inteiro, ideal para iniciantes ou rotina com pouco tempo.",
            listOf(
                TemplateDay("Full Body A", listOf(t("back_squat", 3, 6, 10), t("bench_press", 3, 6, 10), t("seated_cable_row", 3, 8, 12), t("lateral_raise", 2, 12, 15), t("plank", 3, 30, 60))),
                TemplateDay("Full Body B", listOf(t("romanian_deadlift", 3, 8, 10), t("overhead_press", 3, 6, 10), t("lat_pulldown", 3, 8, 12), t("leg_press", 3, 10, 12), t("cable_crunch", 3, 10, 15))),
            ),
        ),
    )
}

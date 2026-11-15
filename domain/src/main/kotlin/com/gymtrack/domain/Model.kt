package com.gymtrack.domain

enum class MuscleGroup(val label: String) {
    CHEST("Peito"),
    BACK("Costas"),
    SHOULDERS("Ombros"),
    BICEPS("Bíceps"),
    TRICEPS("Tríceps"),
    FOREARMS("Antebraços"),
    QUADS("Quadríceps"),
    HAMSTRINGS("Posteriores"),
    GLUTES("Glúteos"),
    CALVES("Panturrilhas"),
    ABS("Abdômen"),
    TRAPS("Trapézio"),
    LOWER_BACK("Lombar"),
    ADDUCTORS("Adutores"),
    ABDUCTORS("Abdutores"),
    FULL_BODY("Corpo inteiro"),
    CARDIO("Cardio");

    companion object {
        fun fromName(name: String?): MuscleGroup? = entries.firstOrNull { it.name == name }
    }
}

enum class Equipment(val label: String) {
    BARBELL("Barra"),
    DUMBBELL("Halteres"),
    MACHINE("Máquina"),
    CABLE("Polia / Cabo"),
    BODYWEIGHT("Peso corporal"),
    SMITH("Smith"),
    KETTLEBELL("Kettlebell"),
    BAND("Elástico"),
    EZ_BAR("Barra W"),
    OTHER("Outro");

    companion object {
        fun fromName(name: String?): Equipment = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

/** One logged set, independent of persistence. */
data class LoggedSet(
    val weight: Double,
    val reps: Int,
    val isWarmup: Boolean = false,
    val completed: Boolean = true,
    val rpe: Double? = null,
    val rir: Int? = null,
) {
    /** Counts towards volume/records: completed working set with reps > 0. */
    val isWorking: Boolean get() = completed && !isWarmup && reps > 0
    val volume: Double get() = if (isWorking) weight * reps else 0.0
}

/** Sets of one exercise performed on one date (epoch millis). */
data class ExercisePerformance(
    val sessionId: Long,
    val date: Long,
    val sets: List<LoggedSet>,
)

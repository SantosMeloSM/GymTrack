package com.gymtrack.domain

import kotlin.math.roundToInt

enum class OneRepMaxFormula(val label: String) { EPLEY("Epley"), BRZYCKI("Brzycki") }

/**
 * Estimated 1RM. These are statistical ESTIMATES (not tested maxima) and are only
 * reasonably accurate for low rep ranges, so sets above [MAX_REPS] return null.
 */
object OneRepMax {
    const val MAX_REPS = 12

    fun estimate(weight: Double, reps: Int, formula: OneRepMaxFormula = OneRepMaxFormula.EPLEY): Double? {
        if (weight <= 0.0 || reps < 1 || reps > MAX_REPS) return null
        if (reps == 1) return weight
        return when (formula) {
            OneRepMaxFormula.EPLEY -> weight * (1.0 + reps / 30.0)
            OneRepMaxFormula.BRZYCKI -> weight * 36.0 / (37.0 - reps)
        }
    }

    /** Best estimated 1RM across working sets, or null when no set qualifies. */
    fun best(sets: List<LoggedSet>, formula: OneRepMaxFormula = OneRepMaxFormula.EPLEY): Double? =
        sets.filter { it.isWorking }.mapNotNull { estimate(it.weight, it.reps, formula) }.maxOrNull()

    /** Percentage table of the estimated max (e.g. 90% ≈ load for ~3-4 reps). */
    fun percentages(oneRm: Double): List<Pair<Int, Double>> =
        listOf(100, 95, 90, 85, 80, 75, 70, 65, 60).map { it to (oneRm * it / 100.0) }

    fun round(value: Double): Double = (value * 10).roundToInt() / 10.0
}

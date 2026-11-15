package com.gymtrack.app.data.repo

import com.gymtrack.app.data.db.SetRow
import com.gymtrack.domain.ExercisePerformance
import com.gymtrack.domain.LoggedSet
import com.gymtrack.domain.MuscleGroup
import com.gymtrack.domain.OneRepMax
import com.gymtrack.domain.OneRepMaxFormula
import com.gymtrack.domain.PersonalRecords
import com.gymtrack.domain.Records
import com.gymtrack.domain.WorkoutStats

fun SetRow.toLogged() = LoggedSet(weight = weight ?: 0.0, reps = reps ?: 0, isWarmup = isWarmup, completed = completed, rpe = rpe, rir = rir)

/** One point per session for an exercise. */
data class ExerciseSessionPoint(
    val sessionId: Long,
    val date: Long,
    val maxWeight: Double,
    val bestReps: Int,
    val estimated1Rm: Double?,
    val volume: Double,
    val totalReps: Int,
    val sets: List<LoggedSet>,
)

data class ExerciseSummary(
    val exerciseId: Long,
    val name: String,
    val muscle: MuscleGroup?,
    val sessions: Int,
    val records: PersonalRecords,
    val lastDate: Long,
)

object Analytics {

    fun performances(rows: List<SetRow>): List<ExercisePerformance> =
        rows.groupBy { it.sessionId }.map { (sid, r) -> ExercisePerformance(sid, r.first().startedAt, r.map { it.toLogged() }) }.sortedBy { it.date }

    fun exercisePoints(rows: List<SetRow>, formula: OneRepMaxFormula): List<ExerciseSessionPoint> =
        performances(rows).mapNotNull { p ->
            val best = WorkoutStats.bestSet(p.sets) ?: return@mapNotNull null
            ExerciseSessionPoint(p.sessionId, p.date, best.weight, best.reps, OneRepMax.best(p.sets, formula), WorkoutStats.volume(p.sets), WorkoutStats.totalReps(p.sets), p.sets)
        }

    fun exerciseSummaries(rows: List<SetRow>, formula: OneRepMaxFormula): List<ExerciseSummary> =
        rows.groupBy { it.exerciseId }.map { (id, r) ->
            val perf = performances(r)
            ExerciseSummary(id, r.first().exerciseName, MuscleGroup.fromName(r.first().primaryMuscle), perf.size, Records.compute(perf, formula), r.maxOf { it.startedAt })
        }.sortedByDescending { it.lastDate }

    /** Working sets and volume per primary muscle for rows within [from, to). */
    fun muscleBreakdown(rows: List<SetRow>, from: Long, to: Long): List<Triple<MuscleGroup, Int, Double>> =
        rows.filter { it.startedAt in from until to && !it.isWarmup && (it.reps ?: 0) > 0 }
            .groupBy { MuscleGroup.fromName(it.primaryMuscle) ?: MuscleGroup.FULL_BODY }
            .map { (m, r) -> Triple(m, r.size, r.sumOf { (it.weight ?: 0.0) * (it.reps ?: 0) }) }
            .sortedByDescending { it.second }

    /** Volume per session ordered by date. */
    fun sessionVolumes(rows: List<SetRow>): List<Pair<Long, Double>> =
        rows.groupBy { it.sessionId }.map { (_, r) -> r.first().startedAt to WorkoutStats.volume(r.map { it.toLogged() }) }.sortedBy { it.first }
}

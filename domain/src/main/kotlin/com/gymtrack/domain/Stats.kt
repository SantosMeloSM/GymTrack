package com.gymtrack.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object WorkoutStats {

    fun volume(sets: List<LoggedSet>): Double = sets.sumOf { it.volume }

    fun workingSets(sets: List<LoggedSet>): Int = sets.count { it.isWorking }

    fun totalReps(sets: List<LoggedSet>): Int = sets.filter { it.isWorking }.sumOf { it.reps }

    fun maxWeight(sets: List<LoggedSet>): Double? = sets.filter { it.isWorking }.maxOfOrNull { it.weight }

    /** Heaviest set; ties broken by more reps. */
    fun bestSet(sets: List<LoggedSet>): LoggedSet? =
        sets.filter { it.isWorking }.maxWithOrNull(compareBy<LoggedSet> { it.weight }.thenBy { it.reps })
}

/** Personal records for one exercise across its full history. */
data class PersonalRecords(
    val maxWeight: Double?,
    val maxWeightReps: Int?,
    val maxWeightDate: Long?,
    val bestEstimated1Rm: Double?,
    val bestEstimated1RmDate: Long?,
    val maxReps: Int?,
    val maxRepsWeight: Double?,
    val maxSessionVolume: Double?,
    val maxSessionVolumeDate: Long?,
) {
    val isEmpty: Boolean get() = maxWeight == null && maxReps == null
}

/** Records broken by a session relative to all previous history. */
data class RecordBreak(val type: RecordType, val value: Double, val previous: Double?)

enum class RecordType(val label: String) {
    WEIGHT("Maior carga"),
    ESTIMATED_1RM("Maior 1RM estimado"),
    REPS("Mais repetições"),
    VOLUME("Maior volume na sessão"),
}

object Records {

    fun compute(history: List<ExercisePerformance>, formula: OneRepMaxFormula = OneRepMaxFormula.EPLEY): PersonalRecords {
        var maxW: Double? = null; var maxWReps: Int? = null; var maxWDate: Long? = null
        var best1: Double? = null; var best1Date: Long? = null
        var maxR: Int? = null; var maxRW: Double? = null
        var maxV: Double? = null; var maxVDate: Long? = null
        for (perf in history.sortedBy { it.date }) {
            for (s in perf.sets.filter { it.isWorking }) {
                if (maxW == null || s.weight > maxW || (s.weight == maxW && s.reps > (maxWReps ?: 0))) {
                    maxW = s.weight; maxWReps = s.reps; maxWDate = perf.date
                }
                if (maxR == null || s.reps > maxR) { maxR = s.reps; maxRW = s.weight }
                val e = OneRepMax.estimate(s.weight, s.reps, formula)
                if (e != null && (best1 == null || e > best1)) { best1 = e; best1Date = perf.date }
            }
            val v = WorkoutStats.volume(perf.sets)
            if (v > 0 && (maxV == null || v > maxV)) { maxV = v; maxVDate = perf.date }
        }
        return PersonalRecords(maxW, maxWReps, maxWDate, best1, best1Date, maxR, maxRW, maxV, maxVDate)
    }

    /** Which records [current] breaks compared to [previous] history (strictly greater). */
    fun broken(previous: List<ExercisePerformance>, current: List<LoggedSet>, formula: OneRepMaxFormula = OneRepMaxFormula.EPLEY): List<RecordBreak> {
        if (current.none { it.isWorking }) return emptyList()
        val prev = compute(previous, formula)
        val out = mutableListOf<RecordBreak>()
        val w = WorkoutStats.maxWeight(current)
        if (w != null && (prev.maxWeight == null || w > prev.maxWeight)) out += RecordBreak(RecordType.WEIGHT, w, prev.maxWeight)
        val e = OneRepMax.best(current, formula)
        if (e != null && (prev.bestEstimated1Rm == null || e > prev.bestEstimated1Rm + 1e-9)) out += RecordBreak(RecordType.ESTIMATED_1RM, e, prev.bestEstimated1Rm)
        val r = current.filter { it.isWorking }.maxOf { it.reps }
        if (prev.maxReps == null || r > prev.maxReps) out += RecordBreak(RecordType.REPS, r.toDouble(), prev.maxReps?.toDouble())
        val v = WorkoutStats.volume(current)
        if (prev.maxSessionVolume == null || v > prev.maxSessionVolume) out += RecordBreak(RecordType.VOLUME, v, prev.maxSessionVolume)
        // A first-ever session is not a "record" worth celebrating.
        return if (previous.isEmpty()) emptyList() else out
    }
}

/** Calendar/consistency metrics. All dates are epoch millis interpreted in [zone]. */
class Consistency(private val zone: ZoneId = ZoneId.systemDefault()) {

    fun toDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Sessions per week (Monday-based) for the last [weeks] weeks, oldest first, including the current week. */
    fun weeklyCounts(sessionDates: List<Long>, today: LocalDate, weeks: Int): List<Pair<LocalDate, Int>> {
        val current = weekStart(today)
        val byWeek = sessionDates.groupingBy { weekStart(toDate(it)) }.eachCount()
        return (weeks - 1 downTo 0).map { i ->
            val ws = current.minusWeeks(i.toLong())
            ws to (byWeek[ws] ?: 0)
        }
    }

    /**
     * Consecutive weeks reaching [target] sessions, counting back from the current week.
     * The current week only counts if it already reached the target; otherwise counting
     * starts from last week (the current week is still in progress).
     */
    fun weekStreak(sessionDates: List<Long>, today: LocalDate, target: Int): Int {
        if (target <= 0) return 0
        val byWeek = sessionDates.groupingBy { weekStart(toDate(it)) }.eachCount()
        var ws = weekStart(today)
        if ((byWeek[ws] ?: 0) < target) ws = ws.minusWeeks(1)
        var streak = 0
        while ((byWeek[ws] ?: 0) >= target) { streak++; ws = ws.minusWeeks(1) }
        return streak
    }

    /** Distinct training days in [days] days up to and including today. */
    fun activeDays(sessionDates: List<Long>, today: LocalDate, days: Int): Int {
        val from = today.minusDays((days - 1).toLong())
        return sessionDates.map { toDate(it) }.filter { !it.isBefore(from) && !it.isAfter(today) }.toSet().size
    }

    /** Percentage (0-100) of the last [weeks] completed weeks that reached the target. */
    fun adherence(sessionDates: List<Long>, today: LocalDate, target: Int, weeks: Int): Int {
        if (target <= 0 || weeks <= 0) return 0
        val counts = weeklyCounts(sessionDates, today.minusWeeks(1), weeks)
        val hit = counts.count { it.second >= target }
        return (hit * 100) / weeks
    }

    fun trainingDaysInMonth(sessionDates: List<Long>, year: Int, month: Int): Set<Int> =
        sessionDates.map { toDate(it) }.filter { it.year == year && it.monthValue == month }.map { it.dayOfMonth }.toSet()
}

object Format {
    fun weight(value: Double?): String {
        if (value == null) return "–"
        return if (value % 1.0 == 0.0) value.toLong().toString() else String.format(java.util.Locale.US, "%.1f", value).trimEnd('0').trimEnd('.').replace('.', ',')
    }

    fun volume(value: Double): String {
        val v = value.toLong()
        return if (v >= 10_000) String.format(java.util.Locale("pt", "BR"), "%,.1f t", value / 1000.0) else String.format(java.util.Locale("pt", "BR"), "%,d kg", v)
    }

    fun duration(millis: Long): String {
        val total = (millis.coerceAtLeast(0) / 1000)
        val h = total / 3600; val m = (total % 3600) / 60; val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    fun durationShort(millis: Long): String {
        val totalMin = millis.coerceAtLeast(0) / 60000
        val h = totalMin / 60; val m = totalMin % 60
        return if (h > 0) "${h}h ${m}min" else "${m}min"
    }

    /** Parses user input like "82,5" or "82.5"; returns null for invalid/negative. */
    fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 && it.isFinite() }
}

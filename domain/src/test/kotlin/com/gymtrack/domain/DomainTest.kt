package com.gymtrack.domain

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class OneRepMaxTest {
    @Test fun epley() {
        assertEquals(100.0 * (1 + 5 / 30.0), OneRepMax.estimate(100.0, 5)!!, 1e-9)
    }
    @Test fun brzycki() {
        assertEquals(100.0 * 36 / 32, OneRepMax.estimate(100.0, 5, OneRepMaxFormula.BRZYCKI)!!, 1e-9)
    }
    @Test fun singleRepIsWeight() { assertEquals(140.0, OneRepMax.estimate(140.0, 1)!!, 0.0) }
    @Test fun invalidInputsReturnNull() {
        assertNull(OneRepMax.estimate(0.0, 5))
        assertNull(OneRepMax.estimate(50.0, 0))
        assertNull(OneRepMax.estimate(50.0, 20))
    }
    @Test fun bestIgnoresWarmupsAndIncomplete() {
        val sets = listOf(
            LoggedSet(200.0, 3, isWarmup = true),
            LoggedSet(300.0, 3, completed = false),
            LoggedSet(100.0, 3),
        )
        assertEquals(110.0, OneRepMax.best(sets)!!, 1e-9)
    }
}

class StatsTest {
    @Test fun volumeOnlyWorkingSets() {
        val sets = listOf(LoggedSet(20.0, 10, isWarmup = true), LoggedSet(60.0, 10), LoggedSet(60.0, 8), LoggedSet(70.0, 5, completed = false))
        assertEquals(1080.0, WorkoutStats.volume(sets), 0.0)
        assertEquals(2, WorkoutStats.workingSets(sets))
        assertEquals(18, WorkoutStats.totalReps(sets))
        assertEquals(60.0, WorkoutStats.maxWeight(sets)!!, 0.0)
        assertEquals(10, WorkoutStats.bestSet(sets)!!.reps)
    }

    @Test fun recordsAndBreaks() {
        val history = listOf(
            ExercisePerformance(1, 1000, listOf(LoggedSet(80.0, 8), LoggedSet(80.0, 6))),
            ExercisePerformance(2, 2000, listOf(LoggedSet(85.0, 5))),
        )
        val pr = Records.compute(history)
        assertEquals(85.0, pr.maxWeight!!, 0.0)
        assertEquals(2000L, pr.maxWeightDate)
        assertEquals(8, pr.maxReps)
        assertEquals(1120.0, pr.maxSessionVolume!!, 0.0)
        assertEquals(80 * (1 + 8 / 30.0), pr.bestEstimated1Rm!!, 1e-9)

        val broken = Records.broken(history, listOf(LoggedSet(90.0, 3)))
        assertTrue(broken.any { it.type == RecordType.WEIGHT && it.value == 90.0 && it.previous == 85.0 })
        assertFalse(broken.any { it.type == RecordType.VOLUME })
        assertTrue(Records.broken(emptyList(), listOf(LoggedSet(90.0, 3))).isEmpty())
    }
}

class ConsistencyTest {
    private val c = Consistency(ZoneOffset.UTC)
    private fun d(s: String) = LocalDate.parse(s).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test fun weeklyCountsAndStreak() {
        // 2026-11-16 is a Monday
        val dates = listOf(d("2026-11-02"), d("2026-11-04"), d("2026-11-06"), d("2026-11-09"), d("2026-11-11"), d("2026-11-13"), d("2026-11-16"))
        val today = LocalDate.parse("2026-11-17")
        val counts = c.weeklyCounts(dates, today, 3)
        assertEquals(listOf(3, 3, 1), counts.map { it.second })
        assertEquals(LocalDate.parse("2026-11-16"), counts.last().first)
        assertEquals(2, c.weekStreak(dates, today, 3))
        assertEquals(3, c.weekStreak(dates, today, 1))
        assertEquals(7, c.activeDays(dates, today, 30))
        assertEquals(setOf(2, 4, 6, 9, 11, 13, 16), c.trainingDaysInMonth(dates, 2026, 11))
        assertEquals(50, c.adherence(dates, today, 3, 4))
    }
}

class FormatTest {
    @Test fun formats() {
        assertEquals("80", Format.weight(80.0))
        assertEquals("82,5", Format.weight(82.5))
        assertEquals(82.5, Format.parseDecimal("82,5")!!, 0.0)
        assertNull(Format.parseDecimal("-3"))
        assertNull(Format.parseDecimal("abc"))
        assertEquals("01:05", Format.duration(65_000))
        assertEquals("1:01:05", Format.duration(3_665_000))
    }
}

class SimplePdfTest {
    @Test fun producesValidStructure() {
        val pdf = SimplePdf()
        pdf.text(40f, 40f, 20f, "Relatório de progresso (teste)", bold = true)
        pdf.line(40f, 70f, 555f, 70f)
        pdf.newPage()
        pdf.text(40f, 40f, 10f, "Supino 82,5 kg × 6 — ação")
        val bytes = pdf.toByteArray()
        val text = String(bytes, Charsets.ISO_8859_1)
        assertTrue(text.startsWith("%PDF-1.4"))
        assertTrue(text.trimEnd().endsWith("%%EOF"))
        assertTrue(text.contains("/Count 2"))
        assertTrue("parentheses escaped", text.contains("\\(teste\\)"))
        assertTrue("accents as WinAnsi octal", text.contains("Relat\\363rio"))
        // xref offsets must point at "N 0 obj"
        val startXref = text.substringAfterLast("startxref\n").substringBefore("\n").trim().toInt()
        assertTrue(text.substring(startXref).startsWith("xref"))
        val entries = text.substring(startXref).lines().drop(3).takeWhile { it.endsWith(" n ") }
        entries.forEachIndexed { i, e -> assertTrue(text.substring(e.take(10).toInt()).startsWith("${i + 1} 0 obj")) }
    }
}

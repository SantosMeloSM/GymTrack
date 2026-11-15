package com.gymtrack.app.data.repo

import android.content.Context
import android.net.Uri
import com.gymtrack.app.data.db.BodyMeasurementEntity
import com.gymtrack.app.data.db.SessionSummary
import com.gymtrack.app.data.db.SetRow
import com.gymtrack.domain.Consistency
import com.gymtrack.domain.Format
import com.gymtrack.domain.OneRepMax
import com.gymtrack.domain.OneRepMaxFormula
import com.gymtrack.domain.SimplePdf
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportExporter(private val context: Context) {
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val dateTimeFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    private fun csvCell(v: String): String = if (v.any { it == ';' || it == '"' || it == '\n' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
    private fun num(v: Double?): String = v?.let { Format.weight(it) } ?: ""

    /** CSV (semicolon separated, Excel pt-BR friendly) with every completed set. */
    fun writeCsv(rows: List<SetRow>, formula: OneRepMaxFormula, out: OutputStream) {
        val w = out.bufferedWriter(Charsets.UTF_8)
        w.write("﻿") // BOM so spreadsheet apps detect UTF-8
        w.write("data;sessao;exercicio;grupo_muscular;serie;tipo;carga_kg;repeticoes;volume_kg;rpe;rir;1rm_estimado_kg\n")
        rows.forEach { r ->
            val e = if (!r.isWarmup) OneRepMax.estimate(r.weight ?: 0.0, r.reps ?: 0, formula) else null
            val vol = if (!r.isWarmup) (r.weight ?: 0.0) * (r.reps ?: 0) else 0.0
            w.write(listOf(
                dateTimeFmt.format(Date(r.startedAt)), csvCell(r.sessionName), csvCell(r.exerciseName),
                com.gymtrack.domain.MuscleGroup.fromName(r.primaryMuscle)?.label ?: r.primaryMuscle,
                (r.setPosition + 1).toString(), if (r.isWarmup) "aquecimento" else "trabalho",
                num(r.weight), (r.reps ?: 0).toString(), num(vol), num(r.rpe), r.rir?.toString() ?: "", num(e?.let { OneRepMax.round(it) }),
            ).joinToString(";"))
            w.write("\n")
        }
        w.flush()
    }

    suspend fun exportCsv(uri: Uri, rows: List<SetRow>, formula: OneRepMaxFormula) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { writeCsv(rows, formula, it) } ?: error("Não foi possível gravar o arquivo.")
    }

    /** Multi-page A4 progress report (pure-Kotlin PDF writer, works on every Android version). */
    fun writePdf(
        rows: List<SetRow>, sessions: List<SessionSummary>, measurements: List<BodyMeasurementEntity>,
        formula: OneRepMaxFormula, weeklyTarget: Int, out: OutputStream,
    ) {
        val pdf = SimplePdf()
        val pageW = pdf.pageWidth; val pageH = pdf.pageHeight; val margin = 40f
        var y = 0f
        fun newPage() {
            pdf.newPage(); y = margin
            pdf.text(margin, pageH - 30f, 8f, "GymTrack · Relatório de progresso", gray = 0.45f)
            pdf.text(pageW - margin - 40f, pageH - 30f, 8f, "Página ${pdf.pageCount}", gray = 0.45f)
        }
        fun ensure(space: Float) { if (y + space > pageH - 50f) newPage() }
        fun text(s: String, size: Float = 10.5f, bold: Boolean = false, gray: Float = 0.2f, adv: Float = size + 6f) { ensure(adv); pdf.text(margin, y, size, s, bold, gray); y += adv }
        fun row(cols: List<String>, xs: List<Float>, bold: Boolean = false) {
            ensure(16f)
            cols.forEachIndexed { i, c -> pdf.text(xs[i], y, 10f, c.take(if (i == 0) 34 else 22), bold, 0.2f) }
            y += 16f
        }
        fun rule() { ensure(8f); pdf.line(margin, y + 2f, pageW - margin, y + 2f); y += 8f }

        newPage()
        text("Relatório de progresso", size = 22f, bold = true, gray = 0f, adv = 30f)
        text("Gerado em ${dateTimeFmt.format(Date())}", size = 9f, gray = 0.45f, adv = 20f)

        val c = Consistency()
        val dates = sessions.map { it.startedAt }
        val today = LocalDate.now()
        text("Resumo geral", size = 14f, bold = true, adv = 22f)
        val totalVol = sessions.sumOf { it.volume }
        val totalSets = sessions.sumOf { it.setCount }
        val totalTime = sessions.sumOf { s -> ((s.endedAt ?: s.startedAt) - s.startedAt - s.pausedAccumMs).coerceAtLeast(0) }
        listOf(
            "Sessões registradas: ${sessions.size}",
            if (sessions.isNotEmpty()) "Período: ${dateFmt.format(Date(dates.min()))} a ${dateFmt.format(Date(dates.max()))}" else "Período: –",
            "Séries de trabalho concluídas: $totalSets",
            "Volume total (carga × repetições): ${Format.volume(totalVol)}",
            "Tempo total de treino: ${Format.durationShort(totalTime)}",
            "Dias treinados nos últimos 30 dias: ${c.activeDays(dates, today, 30)}",
            "Sequência atual de semanas com meta ($weeklyTarget/semana): ${c.weekStreak(dates, today, weeklyTarget)}",
            "Aderência à meta nas últimas 8 semanas: ${c.adherence(dates, today, weeklyTarget, 8)}%",
        ).forEach { text(it) }
        y += 10f

        text("Recordes pessoais por exercício", size = 14f, bold = true, adv = 22f)
        text("1RM estimado pela fórmula ${formula.label} (séries de até ${OneRepMax.MAX_REPS} repetições). É uma estimativa, não um teste de carga máxima.", size = 8.5f, gray = 0.45f, adv = 16f)
        val xs = listOf(margin, 250f, 340f, 420f, 500f)
        row(listOf("Exercício", "Maior carga", "1RM est.", "Máx. reps", "Sessões"), xs, bold = true)
        rule()
        Analytics.exerciseSummaries(rows, formula).sortedBy { it.name }.forEach { s ->
            row(listOf(
                s.name,
                s.records.maxWeight?.let { "${Format.weight(it)} kg × ${s.records.maxWeightReps}" } ?: "–",
                s.records.bestEstimated1Rm?.let { "${Format.weight(OneRepMax.round(it))} kg" } ?: "–",
                s.records.maxReps?.toString() ?: "–",
                s.sessions.toString(),
            ), xs)
        }
        y += 12f

        text("Últimas sessões", size = 14f, bold = true, adv = 22f)
        val xs2 = listOf(margin, 140f, 330f, 420f, 490f)
        sessions.take(30).forEach { s ->
            row(listOf(dateFmt.format(Date(s.startedAt)), s.name, "${s.setCount} séries", Format.volume(s.volume), Format.durationShort(((s.endedAt ?: s.startedAt) - s.startedAt - s.pausedAccumMs))), xs2)
        }

        if (measurements.isNotEmpty()) {
            y += 12f
            text("Medidas corporais", size = 14f, bold = true, adv = 22f)
            val xs3 = listOf(margin, 130f, 210f, 290f, 370f, 450f)
            row(listOf("Data", "Peso (kg)", "Cintura", "Peitoral", "Braço D", "Coxa D"), xs3, bold = true)
            rule()
            measurements.sortedByDescending { it.date }.take(25).forEach { m ->
                row(listOf(dateFmt.format(Date(m.date)), num(m.weight), num(m.waist), num(m.chest), num(m.armRight), num(m.thighRight)), xs3)
            }
        }
        pdf.writeTo(out)
    }

    suspend fun exportPdf(
        uri: Uri, rows: List<SetRow>, sessions: List<SessionSummary>, measurements: List<BodyMeasurementEntity>, formula: OneRepMaxFormula, weeklyTarget: Int,
    ) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { writePdf(rows, sessions, measurements, formula, weeklyTarget, it) } ?: error("Não foi possível gravar o arquivo.")
    }
}

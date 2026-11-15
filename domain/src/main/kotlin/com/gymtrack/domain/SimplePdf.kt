package com.gymtrack.domain

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.Locale

/**
 * Minimal, dependency-free PDF 1.4 writer (text + lines) using the standard Helvetica fonts
 * with WinAnsi encoding, so Portuguese accents render correctly. Coordinates are top-left
 * based (like Android's Canvas) and converted internally.
 */
class SimplePdf(val pageWidth: Float = 595f, val pageHeight: Float = 842f) {
    private val pages = mutableListOf<StringBuilder>()
    private val current get() = pages.last()
    private val cp1252: Charset = Charset.forName("windows-1252")

    val pageCount: Int get() = pages.size

    fun newPage() { pages += StringBuilder() }

    private fun n(v: Float) = String.format(Locale.US, "%.2f", v)

    private fun encode(s: String): ByteArray {
        val enc = cp1252.newEncoder().onUnmappableCharacter(CodingErrorAction.REPLACE).onMalformedInput(CodingErrorAction.REPLACE).replaceWith(byteArrayOf('?'.code.toByte()))
        val buf: ByteBuffer = enc.encode(CharBuffer.wrap(s.replace('≥', '>').replace('−', '-').replace('—', '-')))
        return ByteArray(buf.remaining()).also { buf.get(it) }
    }

    /** Escapes a string literal; non-ASCII bytes are written as octal escapes. */
    private fun literal(s: String): String = buildString {
        append('(')
        encode(s).forEach { b ->
            val c = b.toInt() and 0xFF
            when {
                c == '('.code || c == ')'.code || c == '\\'.code -> { append('\\'); append(c.toChar()) }
                c in 32..126 -> append(c.toChar())
                else -> append('\\').append(Integer.toOctalString(c).padStart(3, '0'))
            }
        }
        append(')')
    }

    /** Draws text with its top at [yTop]. [gray] 0 = black, 1 = white. */
    fun text(x: Float, yTop: Float, size: Float, s: String, bold: Boolean = false, gray: Float = 0f) {
        if (pages.isEmpty()) newPage()
        val baseline = pageHeight - yTop - size * 0.8f
        current.append("BT /${if (bold) "F2" else "F1"} ${n(size)} Tf ${n(gray)} g ${n(x)} ${n(baseline)} Td ${literal(s)} Tj ET\n")
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, gray: Float = 0.8f, width: Float = 0.8f) {
        if (pages.isEmpty()) newPage()
        current.append("${n(gray)} G ${n(width)} w ${n(x1)} ${n(pageHeight - y1)} m ${n(x2)} ${n(pageHeight - y2)} l S\n")
    }

    fun toByteArray(): ByteArray {
        if (pages.isEmpty()) newPage()
        val out = ByteArrayOutputStream()
        val offsets = mutableListOf<Int>()
        fun w(s: String) = out.write(s.toByteArray(Charsets.ISO_8859_1))
        fun obj(id: Int, body: String) { offsets += out.size(); w("$id 0 obj\n$body\nendobj\n") }
        w("%PDF-1.4\n%âãÏÓ\n")
        val firstPageObj = 5
        val kids = pages.indices.joinToString(" ") { "${firstPageObj + it * 2} 0 R" }
        obj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        obj(2, "<< /Type /Pages /Kids [$kids] /Count ${pages.size} >>")
        obj(3, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
        obj(4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>")
        pages.forEachIndexed { i, content ->
            val pageId = firstPageObj + i * 2
            val bytes = content.toString().toByteArray(Charsets.ISO_8859_1)
            obj(pageId, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${n(pageWidth)} ${n(pageHeight)}] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents ${pageId + 1} 0 R >>")
            offsets += out.size()
            w("${pageId + 1} 0 obj\n<< /Length ${bytes.size} >>\nstream\n"); out.write(bytes); w("\nendstream\nendobj\n")
        }
        val xref = out.size()
        w("xref\n0 ${offsets.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { w(String.format(Locale.US, "%010d 00000 n \n", it)) }
        w("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return out.toByteArray()
    }

    fun writeTo(out: OutputStream) = out.write(toByteArray())
}

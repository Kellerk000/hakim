package com.hakim.clinical.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** تصدير ملف مريض كـ PDF (A4) بالعربي. */
object PdfExporter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40f
    private const val FOOTER = 26f

    private fun typeface(context: Context, weight: String): Typeface =
        runCatching { Typeface.createFromAsset(context.assets, "fonts/IBMPlexSansArabic-$weight.ttf") }
            .getOrDefault(if (weight == "Bold") Typeface.DEFAULT_BOLD else Typeface.DEFAULT)

    fun write(context: Context, patient: Patient, out: OutputStream) {
        val doc = PdfDocument()
        val contentW = (PAGE_W - 2 * MARGIN).toInt()
        val regular = typeface(context, "Regular")
        val bold = typeface(context, "Bold")

        val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = 0xFF1F2A3D.toInt()
            typeface = regular
        }
        val title = TextPaint(body).apply { textSize = 22f; typeface = bold; color = 0xFF0E9E8A.toInt() }
        val h1 = TextPaint(body).apply { textSize = 15f; typeface = bold; color = 0xFF0F1E33.toInt() }
        val h2 = TextPaint(body).apply { textSize = 12.5f; typeface = bold; color = 0xFF0E9E8A.toInt() }
        val small = TextPaint(body).apply { textSize = 9.5f; color = 0xFF6B7A90.toInt() }
        val linePaint = Paint().apply { color = 0xFFE1E8F0.toInt(); strokeWidth = 1f }

        var pageNo = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f
        val bottomLimit = PAGE_H - MARGIN - FOOTER

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            page = p
            canvas = p.canvas
            y = MARGIN
            p.canvas.drawText("DrAli  -  $pageNo", MARGIN, PAGE_H - MARGIN / 2f, small)
        }

        fun block(text: String, paint: TextPaint, spaceAfter: Float = 6f) {
            if (text.isBlank()) return
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, contentW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                .setLineSpacing(2f, 1.1f)
                .build()
            var line = 0
            while (line < layout.lineCount) {
                val firstH = (layout.getLineBottom(line) - layout.getLineTop(line)).toFloat()
                if (canvas == null || y + firstH > bottomLimit) newPage()
                val startTop = layout.getLineTop(line)
                var end = line
                while (end < layout.lineCount && y + (layout.getLineBottom(end) - startTop) <= bottomLimit) end++
                if (end == line) end = line + 1
                val c = canvas ?: return
                val endBottom = layout.getLineBottom(end - 1)
                c.save()
                c.translate(MARGIN, y - startTop)
                c.clipRect(0f, startTop.toFloat(), contentW.toFloat(), endBottom.toFloat())
                layout.draw(c)
                c.restore()
                y += (endBottom - startTop).toFloat()
                line = end
            }
            y += spaceAfter
        }

        fun divider() {
            if (canvas == null || y + 12f > bottomLimit) newPage()
            canvas?.drawLine(MARGIN, y + 4f, PAGE_W - MARGIN, y + 4f, linePaint)
            y += 12f
        }

        fun field(label: String, value: String) {
            if (value.isBlank()) return
            block(label, h2, 1f)
            block(value.trim(), body, 7f)
        }

        val dateFmt = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
        val dateTimeFmt = SimpleDateFormat("d MMMM yyyy - HH:mm", Locale("ar"))

        newPage()
        block("DrAli · ملف مريض", title, 2f)
        block("تاريخ التصدير: ${dateTimeFmt.format(Date())}", small, 10f)
        divider()
        block(patient.displayName, h1, 2f)
        block(patient.subtitle, body, 2f)
        if (patient.phone.isNotBlank()) block("الهاتف: ${patient.phone}", body, 4f)
        divider()
        field("السوابق المرضية", patient.history)
        field("الأدوية الدائمة", patient.meds)
        field("التحسس", patient.allergies)
        field("العمليات السابقة", patient.surgeries)
        field("ملاحظات", patient.notes)

        val visits = patient.visits.sortedByDescending { it.date }
        divider()
        block("الزيارات (${visits.size})", h1, 6f)
        visits.forEach { v ->
            block(dateFmt.format(Date(v.date)), h2, 3f)
            field("الشكوى والقصة", v.complaint)
            field("الفحص السريري", v.exam)
            if (!v.vitals.isEmpty()) field("العلامات الحيوية", v.vitals.toArabic().removePrefix("العلامات الحيوية المسجلة: "))
            field("التحاليل والصور", v.labs)
            field("التشخيص", v.diagnosis)
            field("العلاج والخطة", v.plan)
            if (v.analysis.isNotBlank()) {
                val p = AnswerParser.parse(v.analysis)
                val summary = when (p.mode) {
                    Mode.MULTIPLE -> "عدة احتمالات: " + p.candidates.joinToString("، ")
                    else -> p.top ?: ""
                }
                field("ملخص تحليل DrAli", summary)
            }
            divider()
        }
        block("هذا الملف للاستعمال الطبي فقط. تحليلات DrAli مساعدة ولا تغني عن التقييم السريري.", small, 0f)

        page?.let { doc.finishPage(it) }
        doc.writeTo(out)
        doc.close()
    }
}

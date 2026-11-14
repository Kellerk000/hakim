package com.hakim.clinical.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** بناء نص الطلب للنموذج. */
object PromptBuilder {

    /** لازم يطابق SYSTEM_PROMPT بملف training/prepare_dataset.py حرفياً. */
    const val SYSTEM_PROMPT: String =
        "أنت \"DrAli\"، مساعد تشخيصي يدعم الطبيب ولا يحلّ محلّه. اقرأ الحالة (فصحى أو عامية سورية) واكتب جوابك بالعربية بنفس عناوين المثال التالي وبنفس ترتيبها، واملأ كل عنوان بمعلومات من الحالة التي أمامك.\n" +
        "مثال لحالة أخرى (لا تنسخ محتواه):\n" +
        "**نوع النتيجة:** تشخيص مرجّح\n" +
        "**التشخيص الأرجح:** التهاب الزائدة الدودية الحاد\n" +
        "**القرائن:** ألم بدأ حول السرة وانتقل لأسفل اليمين مع غثيان ونقص شهية.\n" +
        "**كيف تأكد التشخيص:** فحص بطن متكرر، تعداد دم، سونار أو طبقي حسب الحالة.\n" +
        "**التشخيصات التفريقية:** التهاب عقد مساريقية، التهاب بول، سبب نسائي حاد.\n" +
        "**الخطوة التالية:** تقييم جراحي عاجل.\n" +
        "**الاستعجال:** إسعافي فوراً.\n" +
        "**علامات الإنذار:** تيبس البطن، إغماء، قيء مستمر.\n" +
        "إذا لم تكن القرائن مميزة بوضوح فاكتب في نوع النتيجة \"عدة احتمالات\"، واكتب بدل التشخيص الأرجح **الاحتمالات الأقرب:** مرتبة من الأرجح، واكتب في الخطوة التالية لكل احتمال العلامة التي ترجّحه بهذا الشكل:\n" +
        "- العلامة ← يرجّح اسم التشخيص\n" +
        "لا تعطِ جرعات أدوية ولا خطوات جراحية، ونبّه فوراً إذا كانت الحالة إسعافية."

    private const val MAX_CASE_CHARS = 6000

    fun buildUser(caseText: String, vitals: Vitals, refs: List<RefHit>, includeRefs: Boolean): String {
        val sb = StringBuilder()
        sb.append(caseText.trim().take(MAX_CASE_CHARS))
        if (!vitals.isEmpty()) {
            sb.append("\n").append(vitals.toArabic())
        }
        appendRefs(sb, refs, includeRefs)
        return sb.toString()
    }

    /** طلب تحليل زيارة مع سياق المريض (السوابق والأدوية والزيارات القديمة). */
    fun buildVisit(patient: Patient, visit: Visit, refs: List<RefHit>, includeRefs: Boolean): String {
        val sb = StringBuilder()
        val info = listOfNotNull(
            patient.sex.takeIf { it.isNotBlank() },
            patient.age?.let { "العمر $it سنة" },
        ).joinToString("، ")
        sb.append("معلومات المريض: ").append(info.ifBlank { "غير محددة" }).append(".")
        if (patient.history.isNotBlank()) sb.append("\nالسوابق المرضية: ").append(patient.history.trim().take(600))
        if (patient.meds.isNotBlank()) sb.append("\nالأدوية الدائمة: ").append(patient.meds.trim().take(400))
        if (patient.allergies.isNotBlank()) sb.append("\nالتحسس: ").append(patient.allergies.trim().take(200))
        if (patient.surgeries.isNotBlank()) sb.append("\nالعمليات السابقة: ").append(patient.surgeries.trim().take(300))

        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val previous = patient.visits.filter { it.id != visit.id && it.date <= visit.date }
            .sortedByDescending { it.date }.take(3)
        if (previous.isNotEmpty()) {
            sb.append("\nزيارات سابقة:")
            previous.forEach { v ->
                sb.append("\n- ").append(fmt.format(Date(v.date))).append(": ")
                    .append(v.complaint.trim().replace('\n', ' ').take(160))
                if (v.diagnosis.isNotBlank()) sb.append("؛ التشخيص: ").append(v.diagnosis.trim().take(100))
            }
        }

        sb.append("\n\nالزيارة الحالية:")
        if (visit.complaint.isNotBlank()) sb.append("\nالشكوى والقصة: ").append(visit.complaint.trim().take(3000))
        if (visit.exam.isNotBlank()) sb.append("\nالفحص السريري: ").append(visit.exam.trim().take(1500))
        if (!visit.vitals.isEmpty()) sb.append("\n").append(visit.vitals.toArabic())
        if (visit.labs.isNotBlank()) sb.append("\nالتحاليل والصور: ").append(visit.labs.trim().take(1500))
        appendRefs(sb, refs, includeRefs)
        return sb.toString()
    }

    private fun appendRefs(sb: StringBuilder, refs: List<RefHit>, includeRefs: Boolean) {
        if (includeRefs && refs.isNotEmpty()) {
            sb.append("\n\n[حالات مرجعية من المكتبة المحلية للمقارنة فقط، وقد لا تنطبق على هذه الحالة]")
            refs.take(3).forEach { h ->
                sb.append("\n- ").append(h.entry.dx).append(": ")
                    .append(h.entry.complaint).append("، ").append(h.entry.detail).append(".")
            }
        }
    }
}

data class Vitals(
    val bp: String = "",
    val hr: String = "",
    val temp: String = "",
    val spo2: String = "",
    val rr: String = "",
    val glucose: String = "",
    val weight: String = "",
) {
    fun isEmpty(): Boolean = listOf(bp, hr, temp, spo2, rr, glucose, weight).all { it.isBlank() }

    /** نفس صيغة العلامات الحيوية بملف التدريب. */
    fun toArabic(): String {
        val parts = ArrayList<String>()
        if (bp.isNotBlank()) parts.add("الضغط ${bp.trim()} مم زئبق")
        if (hr.isNotBlank()) parts.add("النبض ${hr.trim()}/د")
        if (temp.isNotBlank()) parts.add("الحرارة ${temp.trim()}°م")
        if (spo2.isNotBlank()) parts.add("تشبع الأكسجين ${spo2.trim()}%")
        if (rr.isNotBlank()) parts.add("معدل التنفس ${rr.trim()}/د")
        if (glucose.isNotBlank()) parts.add("سكر الدم ${glucose.trim()} ملغ/دل")
        if (weight.isNotBlank()) parts.add("الوزن ${weight.trim()} كغ")
        return "العلامات الحيوية المسجلة: " + parts.joinToString("، ") + "."
    }
}

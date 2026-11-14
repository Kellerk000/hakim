package com.hakim.clinical.data

/** بناء نص الطلب للنموذج. */
object PromptBuilder {

    /** لازم يطابق SYSTEM_PROMPT بملف training/prepare_dataset.py حرفياً. */
    const val SYSTEM_PROMPT: String =
        "أنت \"حكيم\"، مساعد تشخيصي يدعم الطبيب ولا يحلّ محلّه. يكتب الطبيب الحالة بالفصحى أو بالعامية السورية.\n" +
        "حلّل الحالة وأجب بهذا التنسيق فقط:\n" +
        "**نوع النتيجة:** تشخيص مرجّح، أو عدة احتمالات\n" +
        "**التشخيص الأرجح:** (مع تشخيص مرجّح) أو **الاحتمالات الأقرب:** (مع عدة احتمالات، مرتبة)\n" +
        "**القرائن:**\n" +
        "**كيف تأكد التشخيص:**\n" +
        "**التشخيصات التفريقية:**\n" +
        "**الخطوة التالية:**\n" +
        "**الاستعجال:**\n" +
        "**علامات الإنذار:**\n" +
        "اختر \"تشخيص مرجّح\" فقط إذا كانت القرائن مميزة بوضوح. وإلا اختر \"عدة احتمالات\" واكتب في الخطوة التالية ما يجب سؤاله أو فحصه للتفريق بينها. لا تعطِ جرعات أدوية ولا خطوات جراحية، ونبّه فوراً إذا كانت الحالة إسعافية."

    private const val MAX_CASE_CHARS = 6000

    fun buildUser(caseText: String, vitals: Vitals, refs: List<RefHit>, includeRefs: Boolean): String {
        val sb = StringBuilder()
        sb.append(caseText.trim().take(MAX_CASE_CHARS))
        if (!vitals.isEmpty()) {
            sb.append("\n").append(vitals.toArabic())
        }
        if (includeRefs && refs.isNotEmpty()) {
            sb.append("\n\n[حالات مرجعية من المكتبة المحلية للمقارنة فقط، وقد لا تنطبق على هذه الحالة]")
            refs.take(3).forEach { h ->
                sb.append("\n- ").append(h.entry.dx).append(": ")
                    .append(h.entry.complaint).append("، ").append(h.entry.detail).append(".")
            }
        }
        return sb.toString()
    }
}

data class Vitals(
    val bp: String = "",
    val hr: String = "",
    val temp: String = "",
    val spo2: String = "",
    val rr: String = "",
) {
    fun isEmpty(): Boolean = listOf(bp, hr, temp, spo2, rr).all { it.isBlank() }

    /** نفس صيغة العلامات الحيوية بملف التدريب. */
    fun toArabic(): String {
        val parts = ArrayList<String>()
        if (bp.isNotBlank()) parts.add("الضغط ${bp.trim()} مم زئبق")
        if (hr.isNotBlank()) parts.add("النبض ${hr.trim()}/د")
        if (temp.isNotBlank()) parts.add("الحرارة ${temp.trim()}°م")
        if (spo2.isNotBlank()) parts.add("تشبع الأكسجين ${spo2.trim()}%")
        if (rr.isNotBlank()) parts.add("معدل التنفس ${rr.trim()}/د")
        return "العلامات الحيوية المسجلة: " + parts.joinToString("، ") + "."
    }
}

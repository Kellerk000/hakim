package com.hakim.clinical.data

import java.util.Calendar
import java.util.UUID

/** ملف مريض. العمر بينحفظ كسنة ولادة حتى يضل صحيح مع الوقت. */
data class Patient(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val fileNo: String = "",
    val birthYear: Int? = null,
    val sex: String = "",
    val phone: String = "",
    val history: String = "",
    val meds: String = "",
    val allergies: String = "",
    val surgeries: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val visits: List<Visit> = emptyList(),
) {
    val displayName: String
        get() = when {
            name.isNotBlank() -> name.trim()
            fileNo.isNotBlank() -> "إضبارة ${fileNo.trim()}"
            else -> "مريض بدون اسم"
        }

    val age: Int?
        get() = birthYear?.let { (currentYear() - it).coerceAtLeast(0) }

    val initials: String
        get() {
            val parts = displayName.split(' ').filter { it.isNotBlank() }
            val arabic = parts.firstOrNull()?.firstOrNull()?.let { it in '\u0600'..'\u06FF' } ?: false
            return when {
                parts.isEmpty() -> "؟"
                parts.size == 1 || arabic -> parts[0].take(1)
                else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
            }
        }

    val lastVisit: Visit? get() = visits.maxByOrNull { it.date }

    /** سطر مختصر: العمر · الجنس · الإضبارة */
    val subtitle: String
        get() = listOfNotNull(
            age?.let { "$it سنة" },
            sex.takeIf { it.isNotBlank() },
            fileNo.takeIf { it.isNotBlank() }?.let { "إضبارة $it" },
        ).joinToString(" · ")

    fun withAge(ageText: String): Patient {
        val a = normalizeDigits(ageText).trim().toIntOrNull()
        return copy(birthYear = a?.let { currentYear() - it })
    }
}

data class Visit(
    val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis(),
    val complaint: String = "",
    val exam: String = "",
    val vitals: Vitals = Vitals(),
    val labs: String = "",
    val diagnosis: String = "",
    val plan: String = "",
    val analysis: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val isEmpty: Boolean
        get() = complaint.isBlank() && exam.isBlank() && vitals.isEmpty() && labs.isBlank() &&
            diagnosis.isBlank() && plan.isBlank()
}

fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

/** يحوّل الأرقام العربية (١٢٣) والفاصلة العربية للأرقام العادية. */
fun normalizeDigits(s: String): String {
    val sb = StringBuilder(s.length)
    for (ch in s) {
        when (ch) {
            in '٠'..'٩' -> sb.append('0' + (ch - '٠'))
            in '۰'..'۹' -> sb.append('0' + (ch - '۰'))
            '٫' -> sb.append('.')
            '،' -> sb.append(',')
            else -> sb.append(ch)
        }
    }
    return sb.toString()
}

/** قراءة رقم من خانة علامة حيوية. */
fun vitalNumber(s: String): Float? =
    normalizeDigits(s).replace(',', '.').trim().takeWhile { it.isDigit() || it == '.' }.toFloatOrNull()

/** الضغط "120/80" → (120, 80) */
fun bloodPressure(s: String): Pair<Float, Float>? {
    val parts = normalizeDigits(s).split('/', '\\', '-', ' ').map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size < 2) return null
    val sys = parts[0].toFloatOrNull() ?: return null
    val dia = parts[1].toFloatOrNull() ?: return null
    return sys to dia
}

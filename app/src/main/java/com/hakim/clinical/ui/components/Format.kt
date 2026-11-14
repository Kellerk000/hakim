package com.hakim.clinical.ui.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val arabic = Locale("ar")

fun formatDate(ms: Long): String = SimpleDateFormat("d MMMM yyyy", arabic).format(Date(ms))

fun formatDateTime(ms: Long): String = SimpleDateFormat("d MMMM yyyy · HH:mm", arabic).format(Date(ms))

/** "اليوم"، "مبارح"، أو التاريخ. */
fun friendlyDate(ms: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = ms }
    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    val dayDiff = now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR)
    val time = SimpleDateFormat("HH:mm", arabic).format(Date(ms))
    return when {
        sameYear && dayDiff == 0 -> "اليوم · $time"
        sameYear && dayDiff == 1 -> "مبارح · $time"
        else -> formatDate(ms)
    }
}

fun isToday(ms: Long): Boolean {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = ms }
    return now.get(Calendar.YEAR) == then.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
}

fun gb(bytes: Long): String = String.format(Locale.US, "%.1f", bytes / 1_073_741_824.0)

fun mb(bytes: Long): String = (bytes / 1_048_576L).toString()

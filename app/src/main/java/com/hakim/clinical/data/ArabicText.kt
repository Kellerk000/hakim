package com.hakim.clinical.data

/** تطبيع النص العربي (فصحى وعامية سورية) للبحث. */
object ArabicText {

    private val diacritics = Regex("[ً-ْٰـ]")
    private val splitter = Regex("[^\\p{L}\\p{N}]+")

    private val prefixes = listOf("وال", "بال", "كال", "فال", "عال", "لل", "ال", "و")
    private val suffixes = listOf("ات", "ين", "ون", "ها", "هم", "ه", "و", "ي")

    private val stopWords = setOf(
        "في", "من", "علي", "عن", "مع", "او", "ثم", "حتي", "منذ", "عند", "بعد", "قبل", "الي", "لا", "ما",
        "هو", "هي", "كان", "يوجد", "توجد", "قد", "كل", "اي", "هذا", "هذه", "ذلك", "التي", "الذي",
        "عم", "شي", "كمان", "بس", "هيك", "يعني", "انو", "اللي", "عنده", "عندها", "فيه", "فيها",
        "مريض", "مريضه", "عمر", "عمره", "سنه", "سنوات", "يوم", "ايام", "ساعه", "ساعات", "اسبوع",
        "شكوي", "اعراض", "حاله", "قصه", "يشتكي", "بيشتكي", "اجا", "حضر", "منها", "الشكوي",
        "صار", "صرل", "صرلو", "صرله", "صرلها", "صرلي", "عندو", "سنين", "شهور", "شهر", "دقيقه",
        "طفل", "طفله", "رضيع", "مراهق", "شب", "صبيه", "ولد", "بنت", "ختيار", "ختياره",
    )

    // مرادفات العامية/الفصحى (بعد التطبيع)
    private val synonyms = mapOf(
        "وجع" to "الم", "اوجاع" to "الم", "يوجع" to "الم", "بيوجع" to "الم", "موجوع" to "الم",
        "استفراغ" to "قيء", "ترجيع" to "قيء", "طرش" to "قيء", "اقياء" to "قيء", "يستفرغ" to "قيء",
        "سخونه" to "حمي", "سخون" to "حمي", "حراره" to "حمي",
        "دوخه" to "دوار", "دوخ" to "دوار", "دايخ" to "دوار",
        "كحه" to "سعال", "قحه" to "سعال", "كح" to "سعال", "قح" to "سعال",
        "بلغم" to "قشع", "صفار" to "يرقان", "اصفرار" to "يرقان",
        "نمل" to "تنميل", "خدر" to "تنميل", "نفخه" to "انتفاخ", "منفوخ" to "انتفاخ",
        "سهال" to "اسهال", "دقات" to "نبض",
        "توجع" to "الم", "بتوجع" to "الم", "عميوجع" to "الم",
        "حكي" to "كلام", "تلبك" to "اضطراب", "ايد" to "ذراع", "اجر" to "ساق", "ضيقه" to "ضيق",
    )

    fun normalize(s: String): String = s
        .replace(diacritics, "")
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        .replace('ى', 'ي').replace('ة', 'ه').replace('ؤ', 'و').replace('ئ', 'ي')
        .lowercase()

    private fun stem(word: String): String {
        var w = word
        synonyms[w]?.let { return it }
        for (p in prefixes) {
            if (w.startsWith(p) && w.length - p.length >= 3) {
                w = w.substring(p.length)
                break
            }
        }
        synonyms[w]?.let { return it }
        for (s in suffixes) {
            if (w.endsWith(s) && w.length - s.length >= 3) {
                w = w.dropLast(s.length)
                break
            }
        }
        return synonyms[w] ?: w
    }

    fun tokens(text: String): List<String> =
        normalize(text).split(splitter)
            .asSequence()
            .filter { it.length >= 2 && it !in stopWords }
            .map { stem(it) }
            .filter { it.length >= 2 && it !in stopWords && !it.all(Char::isDigit) }
            .toList()
}

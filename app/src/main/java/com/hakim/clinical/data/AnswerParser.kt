package com.hakim.clinical.data

enum class Mode { STRONG, MULTIPLE, UNKNOWN }
enum class Urgency { EMERGENCY, URGENT, ROUTINE, UNKNOWN }

private enum class Section { TYPE, TOP, CANDIDATES, CLUES, CONFIRM, DIFFERENTIAL, NEXT, URGENCY, RED_FLAGS, OTHER }

data class Discriminator(val sign: String, val favors: String)

data class ParsedAnswer(
    val mode: Mode,
    val top: String?,
    val candidates: List<String>,
    val clues: String?,
    val confirm: List<String>,
    val differentials: List<String>,
    val nextText: String?,
    val nextBullets: List<String>,
    val discriminators: List<Discriminator>,
    val urgencyText: String?,
    val urgency: Urgency,
    val redFlags: String?,
    val footer: String?,
    val sectionsFound: Int,
) {
    /** العنوان المختصر للسجل. */
    val title: String
        get() = top ?: candidates.firstOrNull()?.let { "$it…" } ?: "تحليل حالة"
}

/**
 * يقرأ جواب النموذج المكتوب بعناوين **...:** ويحوّله لأقسام.
 * متسامح مع الأخطاء الصغيرة، ويشتغل حتى أثناء الكتابة (النص الناقص).
 */
object AnswerParser {

    // عنوان = نص عريض ينتهي بنقطتين أو علامة استفهام (داخل أو بعد النجمتين)
    private val header = Regex(
        """\*\*\s*([^*\n]{1,60}?)\s*[:：؟?]\s*\*\*\s*[:：]?|\*\*\s*([^*\n]{1,60}?)\s*\*\*\s*[:：]"""
    )
    private val listSplit = Regex("[،,\\n]+")
    private val testsSplit = Regex("[؛;\\n]+")
    private val bulletMark = Regex("""^\s*(?:[-•*▪●]|\d+[.)\-])\s*""")
    private val arrows = listOf("←", "⟵", "->", "=>", "→")
    private val favorWords = Regex("""^(?:بيرجّح|يرجّح|بيرجح|يرجح|بترجح|ترجح|لصالح)\s*""")

    fun parse(text: String): ParsedAnswer {
        val matches = header.findAll(text).toList()
        val raw = LinkedHashMap<Section, String>()
        var footer: String? = null

        matches.forEachIndexed { i, m ->
            val label = (m.groups[1]?.value ?: m.groups[2]?.value ?: "").trim()
            val start = m.range.last + 1
            val end = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
            var body = text.substring(start, end).trim().replace("**", "")
            val sec = classify(label)
            if (i == matches.lastIndex && sec == Section.RED_FLAGS) {
                val lines = body.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.size > 1) {
                    body = lines.first()
                    footer = lines.drop(1).joinToString("\n")
                }
            }
            if (sec != Section.OTHER && sec !in raw) raw[sec] = body.trim()
        }

        val typeText = raw[Section.TYPE]?.let { ArabicText.normalize(it) }
        val top = raw[Section.TOP]?.let { cleanItem(it) }?.takeIf { it.isNotBlank() }
        val candidates = raw[Section.CANDIDATES]?.let { splitList(it, listSplit) }.orEmpty()
        val mode = when {
            typeText != null && (typeText.contains("عده") || typeText.contains("احتمالات")) -> Mode.MULTIPLE
            typeText != null && (typeText.contains("مرجح") || typeText.contains("قوي")) -> Mode.STRONG
            top != null -> Mode.STRONG
            candidates.isNotEmpty() -> Mode.MULTIPLE
            else -> Mode.UNKNOWN
        }

        val nextRaw = raw[Section.NEXT]
        val nextLines = nextRaw?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        val bullets = ArrayList<String>()
        val discriminators = ArrayList<Discriminator>()
        val plain = ArrayList<String>()
        for (line in nextLines) {
            val isBullet = bulletMark.containsMatchIn(line)
            val content = line.replace(bulletMark, "").trim()
            val arrow = arrows.firstOrNull { content.contains(it) }
            when {
                arrow != null -> {
                    val sign = content.substringBefore(arrow).trim().trimEnd('،', ',')
                    val favors = content.substringAfter(arrow).trim().replace(favorWords, "").trim().trimEnd('.')
                    if (sign.isNotEmpty() && favors.isNotEmpty()) discriminators.add(Discriminator(sign, favors))
                    else bullets.add(content)
                }
                isBullet -> bullets.add(content.trimEnd('.'))
                else -> plain.add(content)
            }
        }

        val urgencyText = raw[Section.URGENCY]?.let { cleanItem(it) }

        return ParsedAnswer(
            mode = mode,
            top = top,
            candidates = candidates,
            clues = raw[Section.CLUES]?.let { cleanItem(it) },
            confirm = raw[Section.CONFIRM]?.let { splitList(it, testsSplit) }.orEmpty(),
            differentials = raw[Section.DIFFERENTIAL]?.let { splitList(it, listSplit) }.orEmpty(),
            nextText = plain.joinToString(" ").ifBlank { null },
            nextBullets = bullets,
            discriminators = discriminators,
            urgencyText = urgencyText,
            urgency = urgencyText?.let { urgencyOf(it) } ?: Urgency.UNKNOWN,
            redFlags = raw[Section.RED_FLAGS]?.let { cleanItem(it) },
            footer = footer,
            sectionsFound = raw.size,
        )
    }

    private fun classify(label: String): Section {
        val l = ArabicText.normalize(label)
        return when {
            l.contains("نوع") -> Section.TYPE
            l.contains("تفريق") || l.contains("احتمالات تانيه") || l.contains("احتمالات اخري") -> Section.DIFFERENTIAL
            l.contains("احتمالات") -> Section.CANDIDATES
            l.contains("تاكد") || l.contains("تاكيد") || l.contains("فحوص") || l.contains("تحاليل") -> Section.CONFIRM
            l.contains("قراين") || l.contains("قرائن") || l.contains("ليش") -> Section.CLUES
            l.contains("خطوه") || l.contains("تصرف") || l.contains("اعمل") || l.contains("التاليه") -> Section.NEXT
            l.contains("استعجال") || l.contains("مستعجل") -> Section.URGENCY
            l.contains("انذار") || l.contains("دير بالك") -> Section.RED_FLAGS
            l.contains("ارجح") || l.contains("مرجح") || l.contains("احتمال") || l.contains("تشخيص") -> Section.TOP
            else -> Section.OTHER
        }
    }

    fun urgencyOf(text: String): Urgency {
        val t = ArabicText.normalize(text)
        return when {
            t.contains("اسعاف") || t.contains("فورا") || t.contains("طوارئ") || t.contains("حالا") -> Urgency.EMERGENCY
            t.contains("عاجل") || t.contains("نفسه") || t.contains("قريب") || t.contains("اليوم") -> Urgency.URGENT
            t.contains("عياده") || t.contains("متابعه") || t.contains("روتين") -> Urgency.ROUTINE
            else -> Urgency.UNKNOWN
        }
    }

    private fun cleanItem(s: String): String =
        s.lines().joinToString(" ") { it.trim() }.trim().trimEnd('.', '،').trim()

    private fun splitList(s: String, splitter: Regex): List<String> =
        s.split(splitter)
            .map { it.replace(bulletMark, "").trim().trimEnd('.').trim() }
            .filter { it.isNotEmpty() }
}

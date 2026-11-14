package com.hakim.clinical.data

import android.content.Context
import org.json.JSONArray
import kotlin.math.ln
import kotlin.random.Random

data class RefEntry(
    val dx: String,
    val domain: String,
    val specialty: String,
    val acuity: String,
    val triage: String,
    val dd: List<String>,
    val complaint: String,
    val detail: String,
    val complaintS: String,
    val detailS: String,
    val tests: String,
    val action: String,
    val warnings: String,
)

data class RefHit(val entry: RefEntry, val score: Double, val relative: Float)

/**
 * المكتبة المرجعية (184 تشخيص من ملف الحالات) مع بحث BM25 بدون إنترنت.
 */
class ReferenceLibrary(val entries: List<RefEntry>) {

    private val k1 = 1.2
    private val b = 0.75
    private val termFreqs: List<Map<String, Int>>
    private val docLens: IntArray
    private val avgLen: Double
    private val docFreq: Map<String, Int>

    // كلمات الشكوى والعلامة المميزة لكل تشخيص (لازم تتطابق كلمتين منها على الأقل)
    private val coreTerms: List<Set<String>> = entries.map { e ->
        ArabicText.tokens("${e.complaint} ${e.detail} ${e.complaintS} ${e.detailS}").toSet()
    }

    init {
        val tfs = ArrayList<Map<String, Int>>(entries.size)
        val lens = IntArray(entries.size)
        val df = HashMap<String, Int>()
        entries.forEachIndexed { i, e ->
            val terms = ArrayList<String>()
            fun add(text: String, weight: Int) {
                val t = ArabicText.tokens(text)
                repeat(weight) { terms.addAll(t) }
            }
            add(e.complaint, 3)
            add(e.complaintS, 3)
            add(e.detail, 2)
            add(e.detailS, 2)
            add(e.dx, 2)
            add(e.dd.joinToString(" "), 1)
            add(e.specialty, 1)
            val tf = terms.groupingBy { it }.eachCount()
            tfs.add(tf)
            lens[i] = terms.size
            tf.keys.forEach { df[it] = (df[it] ?: 0) + 1 }
        }
        termFreqs = tfs
        docLens = lens
        avgLen = if (lens.isEmpty()) 1.0 else lens.average()
        docFreq = df
    }

    fun search(query: String, limit: Int = 3, minScore: Double = MIN_SCORE): List<RefHit> {
        val q = ArabicText.tokens(query).toSet()
        if (q.isEmpty() || entries.isEmpty()) return emptyList()
        val n = entries.size.toDouble()
        val scores = DoubleArray(entries.size)
        for (term in q) {
            val df = docFreq[term] ?: continue
            val idf = ln(1.0 + (n - df + 0.5) / (df + 0.5))
            for (i in entries.indices) {
                val tf = termFreqs[i][term] ?: continue
                val norm = tf + k1 * (1 - b + b * docLens[i] / avgLen)
                scores[i] += idf * tf * (k1 + 1) / norm
            }
        }
        val ranked = entries.indices
            .filter { i -> scores[i] >= minScore && coreTerms[i].count { it in q } >= MIN_CORE_MATCH }
            .sortedByDescending { scores[it] }
            .take(limit)
        if (ranked.isEmpty()) return emptyList()
        val top = scores[ranked.first()]
        return ranked.filter { scores[it] >= top * 0.45 }
            .map { RefHit(entries[it], scores[it], (scores[it] / top).toFloat()) }
    }

    /** حالة مثال بالعامية من المكتبة (لزر "مثال"). */
    fun randomExample(random: Random = Random.Default): String {
        if (entries.isEmpty()) return ""
        val e = entries[random.nextInt(entries.size)]
        val age = when {
            e.dx.contains("رضيع") -> "${random.nextInt(1, 3)} شهر"
            e.dx.contains("طفل") || e.dx.contains("هيرشسبرونغ") -> "${random.nextInt(2, 11)} سنين"
            e.dx.contains("مراهق") -> "${random.nextInt(13, 18)} سنة"
            else -> "${random.nextInt(22, 70)} سنة"
        }
        val female = listOf("رحم", "مبيض", "حمل", "الولادة", "الثدي", "حوض", "طمث").any { e.dx.contains(it) }
        val male = listOf("خصية", "بروستات", "بربخ", "صفن", "قيلة").any { e.dx.contains(it) }
        val who = when {
            female -> "مريضة عمرها $age"
            male -> "مريض عمره $age"
            random.nextBoolean() -> "مريضة عمرها $age"
            else -> "مريض عمره $age"
        }
        return "$who إجا عالعيادة. عم يشتكي من ${e.complaintS}. ومن القصة: ${e.detailS}."
    }

    companion object {
        const val MIN_SCORE = 6.0
        const val MIN_CORE_MATCH = 2

        fun load(context: Context): ReferenceLibrary {
            val json = context.assets.open("reference_library.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val arr = JSONArray(json)
            val list = ArrayList<RefEntry>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val ddArr = o.optJSONArray("dd")
                val dd = if (ddArr == null) emptyList() else List(ddArr.length()) { ddArr.getString(it) }
                list.add(
                    RefEntry(
                        dx = o.optString("dx"),
                        domain = o.optString("domain"),
                        specialty = o.optString("specialty"),
                        acuity = o.optString("acuity"),
                        triage = o.optString("triage"),
                        dd = dd,
                        complaint = o.optString("complaint"),
                        detail = o.optString("detail"),
                        complaintS = o.optString("complaint_s"),
                        detailS = o.optString("detail_s"),
                        tests = o.optString("tests"),
                        action = o.optString("action"),
                        warnings = o.optString("warnings"),
                    )
                )
            }
            return ReferenceLibrary(list)
        }
    }
}

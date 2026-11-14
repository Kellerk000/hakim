package com.hakim.clinical.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class AppSettings(
    val threads: Int = defaultThreads(),
    val nCtx: Int = 4096,
    val maxTokens: Int = 900,
    val temperature: Float = 0.3f,
    val useReferences: Boolean = false,
    val modelPath: String? = null,
    val modelName: String? = null,
    val modelSize: Long = 0L,
) {
    companion object {
        fun defaultThreads(): Int = (Runtime.getRuntime().availableProcessors() - 2).coerceIn(2, 6)
    }
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("hakim_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            threads = prefs.getInt("threads", d.threads),
            nCtx = prefs.getInt("n_ctx", d.nCtx),
            maxTokens = prefs.getInt("max_tokens", d.maxTokens),
            temperature = prefs.getFloat("temperature", d.temperature),
            useReferences = prefs.getBoolean("use_refs", d.useReferences),
            modelPath = prefs.getString("model_path", null)?.takeIf { File(it).exists() },
            modelName = prefs.getString("model_name", null),
            modelSize = prefs.getLong("model_size", 0L),
        )
    }

    fun save(s: AppSettings) {
        prefs.edit()
            .putInt("threads", s.threads)
            .putInt("n_ctx", s.nCtx)
            .putInt("max_tokens", s.maxTokens)
            .putFloat("temperature", s.temperature)
            .putBoolean("use_refs", s.useReferences)
            .putString("model_path", s.modelPath)
            .putString("model_name", s.modelName)
            .putLong("model_size", s.modelSize)
            .apply()
    }
}

data class HistoryItem(
    val id: Long,
    val time: Long,
    val caseText: String,
    val vitals: Vitals,
    val answer: String,
    val title: String,
    val mode: Mode,
    val urgency: Urgency,
)

/** سجل الحالات، محفوظ محلياً بملف JSON داخل التطبيق فقط. */
class HistoryStore(context: Context) {
    private val file = File(context.filesDir, "history.json")

    @Synchronized
    fun load(): List<HistoryItem> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText(Charsets.UTF_8))
            List(arr.length()) { i -> fromJson(arr.getJSONObject(i)) }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun save(items: List<HistoryItem>) {
        val arr = JSONArray()
        items.take(MAX_ITEMS).forEach { arr.put(toJson(it)) }
        val tmp = File(file.parentFile, "history.json.tmp")
        tmp.writeText(arr.toString(), Charsets.UTF_8)
        if (!tmp.renameTo(file)) {
            file.writeText(arr.toString(), Charsets.UTF_8)
            tmp.delete()
        }
    }

    private fun toJson(h: HistoryItem) = JSONObject().apply {
        put("id", h.id)
        put("time", h.time)
        put("case", h.caseText)
        put("answer", h.answer)
        put("title", h.title)
        put("mode", h.mode.name)
        put("urgency", h.urgency.name)
        put("v_bp", h.vitals.bp)
        put("v_hr", h.vitals.hr)
        put("v_temp", h.vitals.temp)
        put("v_spo2", h.vitals.spo2)
        put("v_rr", h.vitals.rr)
    }

    private fun fromJson(o: JSONObject) = HistoryItem(
        id = o.optLong("id"),
        time = o.optLong("time"),
        caseText = o.optString("case"),
        vitals = Vitals(
            bp = o.optString("v_bp"),
            hr = o.optString("v_hr"),
            temp = o.optString("v_temp"),
            spo2 = o.optString("v_spo2"),
            rr = o.optString("v_rr"),
        ),
        answer = o.optString("answer"),
        title = o.optString("title"),
        mode = runCatching { Mode.valueOf(o.optString("mode")) }.getOrDefault(Mode.UNKNOWN),
        urgency = runCatching { Urgency.valueOf(o.optString("urgency")) }.getOrDefault(Urgency.UNKNOWN),
    )

    companion object {
        private const val MAX_ITEMS = 300
    }
}

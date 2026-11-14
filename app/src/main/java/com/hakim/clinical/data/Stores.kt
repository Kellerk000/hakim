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
    /** 0 = حسب الجهاز، 1 = نهاري، 2 = ليلي */
    val themeMode: Int = 0,
    val lockEnabled: Boolean = false,
    val setupSkipped: Boolean = false,
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
            themeMode = prefs.getInt("theme_mode", d.themeMode),
            lockEnabled = prefs.getBoolean("lock_enabled", d.lockEnabled),
            setupSkipped = prefs.getBoolean("setup_skipped", d.setupSkipped),
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
            .putInt("theme_mode", s.themeMode)
            .putBoolean("lock_enabled", s.lockEnabled)
            .putBoolean("setup_skipped", s.setupSkipped)
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

/** سجل التحليلات السريعة، مشفّر على الجهاز. */
class HistoryStore(context: Context) {
    private val file = File(context.filesDir, "history.enc")
    private val legacy = File(context.filesDir, "history.json")

    @Synchronized
    fun load(): List<HistoryItem> {
        if (file.exists()) {
            return runCatching {
                historyFromJson(JSONArray(String(Crypto.decrypt(file.readBytes()), Charsets.UTF_8)))
            }.getOrDefault(emptyList())
        }
        if (legacy.exists()) {
            // نقل السجل القديم (غير المشفّر) للصيغة المشفّرة
            val items = runCatching { historyFromJson(JSONArray(legacy.readText(Charsets.UTF_8))) }.getOrDefault(emptyList())
            runCatching {
                save(items)
                legacy.delete()
            }
            return items
        }
        return emptyList()
    }

    @Synchronized
    fun save(items: List<HistoryItem>) {
        val bytes = Crypto.encrypt(historyToJson(items.take(MAX_ITEMS)).toString().toByteArray(Charsets.UTF_8))
        val tmp = File(file.parentFile, "history.enc.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(file)) {
            file.writeBytes(bytes)
            tmp.delete()
        }
    }

    companion object {
        private const val MAX_ITEMS = 300

        fun historyToJson(items: List<HistoryItem>): JSONArray = JSONArray().apply {
            items.forEach { h ->
                put(JSONObject().apply {
                    put("id", h.id)
                    put("time", h.time)
                    put("case", h.caseText)
                    put("answer", h.answer)
                    put("title", h.title)
                    put("mode", h.mode.name)
                    put("urgency", h.urgency.name)
                    put("vitals", PatientStore.vitalsToJson(h.vitals))
                })
            }
        }

        fun historyFromJson(arr: JSONArray): List<HistoryItem> = List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val vit = o.optJSONObject("vitals")?.let { PatientStore.vitalsFromJson(it) }
                ?: Vitals(
                    bp = o.optString("v_bp"),
                    hr = o.optString("v_hr"),
                    temp = o.optString("v_temp"),
                    spo2 = o.optString("v_spo2"),
                    rr = o.optString("v_rr"),
                )
            HistoryItem(
                id = o.optLong("id"),
                time = o.optLong("time"),
                caseText = o.optString("case"),
                vitals = vit,
                answer = o.optString("answer"),
                title = o.optString("title"),
                mode = runCatching { Mode.valueOf(o.optString("mode")) }.getOrDefault(Mode.UNKNOWN),
                urgency = runCatching { Urgency.valueOf(o.optString("urgency")) }.getOrDefault(Urgency.UNKNOWN),
            )
        }
    }
}

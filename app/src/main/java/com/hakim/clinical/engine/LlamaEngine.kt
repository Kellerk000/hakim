package com.hakim.clinical.engine

/**
 * جسر JNI لمحرك llama.cpp. كل الدوال هون "blocking"، لازم تنستدعى من خيط خلفي.
 */
object LlamaEngine {

    init {
        System.loadLibrary("hakim")
    }

    /** واجهة يستدعيها الكود الأصلي أثناء التوليد. */
    interface NativeCallback {
        fun onText(bytes: ByteArray)
        fun onProgress(done: Int, total: Int)
    }

    external fun nativeLoad(path: String): Long
    external fun nativeFree(handle: Long)
    external fun nativeModelDesc(handle: Long): String
    external fun nativeSystemInfo(): String
    external fun nativeStop()

    external fun nativeGenerate(
        handle: Long,
        system: ByteArray,
        user: ByteArray,
        nCtx: Int,
        nThreads: Int,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
        callback: NativeCallback,
    ): Int
}

data class GenConfig(
    val nCtx: Int = 4096,
    val nThreads: Int = 4,
    val maxTokens: Int = 900,
    val temperature: Float = 0.3f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
)

/** غلاف بسيط يحمّل النموذج مرة وحدة ويولّد منه. */
class InferenceEngine {

    @Volatile
    private var handle: Long = 0L
    private val lock = Any()

    @Volatile
    var loadedPath: String? = null
        private set

    val isLoaded: Boolean get() = handle != 0L

    /** يرجّع وصف النموذج، أو null إذا فشل التحميل. */
    fun load(path: String): String? = synchronized(lock) {
        if (handle != 0L && loadedPath == path) return LlamaEngine.nativeModelDesc(handle)
        release()
        val h = LlamaEngine.nativeLoad(path)
        if (h == 0L) return null
        handle = h
        loadedPath = path
        LlamaEngine.nativeModelDesc(h)
    }

    fun release() = synchronized(lock) {
        if (handle != 0L) {
            LlamaEngine.nativeFree(handle)
            handle = 0L
            loadedPath = null
        }
    }

    fun stop() = LlamaEngine.nativeStop()

    fun systemInfo(): String = runCatching { LlamaEngine.nativeSystemInfo() }.getOrDefault("")

    /**
     * يولّد الجواب بشكل متدفق. يرجّع عدد التوكنات أو كود خطأ سالب.
     * النصوص بتنبعت كبايتات UTF-8 حتى ما تخرب الحروف العربية.
     */
    fun generate(
        system: String,
        user: String,
        config: GenConfig,
        progressCb: (done: Int, total: Int) -> Unit,
        textCb: (String) -> Unit,
    ): Int = synchronized(lock) {
        val h = handle
        if (h == 0L) return -2
        LlamaEngine.nativeGenerate(
            h,
            system.toByteArray(Charsets.UTF_8),
            user.toByteArray(Charsets.UTF_8),
            config.nCtx,
            config.nThreads,
            config.maxTokens,
            config.temperature,
            config.topP,
            config.topK,
            object : LlamaEngine.NativeCallback {
                override fun onText(bytes: ByteArray) {
                    textCb(String(bytes, Charsets.UTF_8))
                }

                override fun onProgress(done: Int, total: Int) {
                    progressCb(done, total)
                }
            },
        )
    }

    companion object {
        fun errorMessage(code: Int): String = when (code) {
            -1 -> "ما قدر يقرأ النص. جرّب تعيد صياغة الحالة."
            -2 -> "النموذج مو جاهز أو ما في ذاكرة كافية. سكّر التطبيقات التانية وجرّب مرة تانية."
            -3 -> "الحالة طويلة كتير على إعدادات السياق. اختصرها أو كبّر حجم السياق من الإعدادات."
            -4 -> "صار خطأ أثناء المعالجة. جرّب مرة تانية."
            else -> "خطأ غير معروف ($code)."
        }
    }
}

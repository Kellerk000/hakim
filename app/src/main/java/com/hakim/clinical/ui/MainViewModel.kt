package com.hakim.clinical.ui

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hakim.clinical.data.AnswerParser
import com.hakim.clinical.data.AppSettings
import com.hakim.clinical.data.HistoryItem
import com.hakim.clinical.data.HistoryStore
import com.hakim.clinical.data.ParsedAnswer
import com.hakim.clinical.data.PromptBuilder
import com.hakim.clinical.data.RefHit
import com.hakim.clinical.data.ReferenceLibrary
import com.hakim.clinical.data.SettingsStore
import com.hakim.clinical.data.Vitals
import com.hakim.clinical.engine.GenConfig
import com.hakim.clinical.engine.InferenceEngine
import com.hakim.clinical.engine.ModelManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { Setup, Home, Result, History, Settings }
enum class Stage { Idle, Reading, Writing, Done, Stopped, Error }
enum class ModelStatus { None, Loading, Ready, Failed }

data class ImportState(
    val active: Boolean = false,
    val fraction: Float = 0f,
    val copied: Long = 0L,
    val total: Long = 0L,
    val error: String? = null,
)

data class UiState(
    val screen: Screen = Screen.Home,
    val settings: AppSettings = AppSettings(),
    val deviceRamBytes: Long = 0L,
    val modelStatus: ModelStatus = ModelStatus.None,
    val modelDesc: String = "",
    val systemInfo: String = "",
    val importState: ImportState = ImportState(),
    // إدخال الحالة
    val caseText: String = "",
    val vitals: Vitals = Vitals(),
    // النتيجة
    val stage: Stage = Stage.Idle,
    val promptProgress: Float = 0f,
    val output: String = "",
    val parsed: ParsedAnswer? = null,
    val references: List<RefHit> = emptyList(),
    val tokensPerSec: Float = 0f,
    val elapsedMs: Long = 0L,
    val error: String? = null,
    val resultCase: String = "",
    val resultVitals: Vitals = Vitals(),
    val viewingHistoryId: Long? = null,
    // السجل
    val history: List<HistoryItem> = emptyList(),
) {
    val isGenerating: Boolean get() = stage == Stage.Reading || stage == Stage.Writing
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsStore = SettingsStore(app)
    private val historyStore = HistoryStore(app)
    private val modelManager = ModelManager(app)
    private val engine = InferenceEngine()

    @Volatile
    private var library: ReferenceLibrary? = null

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var genJob: Job? = null
    private var importJob: Job? = null

    @Volatile
    private var stopRequested = false

    init {
        val s = settingsStore.load()
        val ram = runCatching {
            val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.totalMem
        }.getOrDefault(0L)
        _state.update {
            it.copy(
                settings = s,
                deviceRamBytes = ram,
                screen = if (s.modelPath == null) Screen.Setup else Screen.Home,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            library = runCatching { ReferenceLibrary.load(app) }.getOrNull()
            val h = historyStore.load()
            val info = engine.systemInfo()
            _state.update { it.copy(history = h, systemInfo = info) }
        }
        s.modelPath?.let { loadModel(it) }
    }

    // ---------------- النموذج ----------------

    private fun loadModel(path: String) {
        _state.update { it.copy(modelStatus = ModelStatus.Loading, modelDesc = "") }
        viewModelScope.launch(Dispatchers.IO) {
            val desc = runCatching { engine.load(path) }.getOrNull()
            _state.update {
                it.copy(
                    modelStatus = if (desc != null) ModelStatus.Ready else ModelStatus.Failed,
                    modelDesc = desc ?: "",
                )
            }
        }
    }

    fun retryLoad() {
        _state.value.settings.modelPath?.let { loadModel(it) }
    }

    fun importModel(uri: Uri) {
        if (importJob?.isActive == true) return
        _state.update { it.copy(importState = ImportState(active = true)) }
        importJob = viewModelScope.launch {
            try {
                val res = modelManager.copyIn(uri) { f, c, t ->
                    _state.update { it.copy(importState = it.importState.copy(fraction = f, copied = c, total = t)) }
                }
                withContext(Dispatchers.IO) { engine.release() }
                val ns = _state.value.settings.copy(
                    modelPath = res.file.absolutePath,
                    modelName = res.displayName,
                    modelSize = res.sizeBytes,
                )
                settingsStore.save(ns)
                _state.update {
                    it.copy(
                        settings = ns,
                        importState = ImportState(),
                        screen = if (it.screen == Screen.Setup) Screen.Home else it.screen,
                    )
                }
                loadModel(res.file.absolutePath)
            } catch (e: CancellationException) {
                _state.update { it.copy(importState = ImportState()) }
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(importState = ImportState(error = e.message ?: "فشل نسخ الملف.")) }
            }
        }
    }

    fun cancelImport() {
        importJob?.cancel()
    }

    // ---------------- التنقل ----------------

    fun navigate(screen: Screen) {
        _state.update { it.copy(screen = screen) }
    }

    /** يرجّع true إذا تعامل مع زر الرجوع. */
    fun back(): Boolean {
        val s = _state.value
        return when (s.screen) {
            Screen.Result, Screen.History, Screen.Settings -> {
                _state.update { it.copy(screen = if (it.settings.modelPath == null) Screen.Setup else Screen.Home) }
                true
            }
            else -> false
        }
    }

    // ---------------- إدخال الحالة ----------------

    fun updateCase(text: String) = _state.update { it.copy(caseText = text) }

    fun updateVitals(v: Vitals) = _state.update { it.copy(vitals = v) }

    fun clearCase() = _state.update { it.copy(caseText = "", vitals = Vitals()) }

    fun fillExample() {
        val ex = library?.randomExample() ?: return
        _state.update { it.copy(caseText = ex, vitals = Vitals()) }
    }

    fun editAgain() {
        _state.update {
            it.copy(caseText = it.resultCase, vitals = it.resultVitals, screen = Screen.Home)
        }
    }

    // ---------------- التحليل ----------------

    fun analyze() {
        val st = _state.value
        if (st.caseText.isBlank() || st.modelStatus != ModelStatus.Ready || genJob?.isActive == true) return
        stopRequested = false
        val refs = runCatching { library?.search(st.caseText).orEmpty() }.getOrDefault(emptyList())
        val user = PromptBuilder.buildUser(st.caseText, st.vitals, refs, st.settings.useReferences)
        val cfg = GenConfig(
            nCtx = st.settings.nCtx,
            nThreads = st.settings.threads,
            maxTokens = st.settings.maxTokens,
            temperature = st.settings.temperature,
        )
        _state.update {
            it.copy(
                screen = Screen.Result,
                stage = Stage.Reading,
                promptProgress = 0f,
                output = "",
                parsed = null,
                references = refs,
                tokensPerSec = 0f,
                elapsedMs = 0L,
                error = null,
                resultCase = st.caseText,
                resultVitals = st.vitals,
                viewingHistoryId = null,
            )
        }

        genJob = viewModelScope.launch(Dispatchers.IO) {
            val sb = StringBuilder()
            val t0 = SystemClock.elapsedRealtime()
            var tFirst = 0L
            var pieces = 0
            var lastUi = 0L

            fun publish(isFinal: Boolean) {
                val now = SystemClock.elapsedRealtime()
                val text = sb.toString()
                val genSecs = if (tFirst > 0) (now - tFirst) / 1000f else 0f
                _state.update {
                    it.copy(
                        output = text,
                        parsed = if (text.isNotBlank()) AnswerParser.parse(text) else null,
                        tokensPerSec = if (genSecs > 0.5f) pieces / genSecs else it.tokensPerSec,
                        elapsedMs = now - t0,
                        stage = if (isFinal) it.stage else Stage.Writing,
                    )
                }
            }

            val code = try {
                engine.generate(
                    system = PromptBuilder.SYSTEM_PROMPT,
                    user = user,
                    config = cfg,
                    progressCb = { done, total ->
                        _state.update { it.copy(promptProgress = if (total > 0) done.toFloat() / total else 0f) }
                    },
                    textCb = { piece ->
                        val now = SystemClock.elapsedRealtime()
                        if (tFirst == 0L) tFirst = now
                        sb.append(piece)
                        pieces++
                        if (now - lastUi > 70) {
                            lastUi = now
                            publish(isFinal = false)
                        }
                    },
                )
            } catch (e: Throwable) {
                -4
            }

            publish(isFinal = true)
            val stage = when {
                code < 0 -> Stage.Error
                stopRequested -> Stage.Stopped
                else -> Stage.Done
            }
            _state.update {
                it.copy(
                    stage = stage,
                    error = if (code < 0) InferenceEngine.errorMessage(code) else null,
                )
            }
            if (code >= 0 && sb.isNotBlank()) saveToHistory(st.caseText, st.vitals, sb.toString())
        }
    }

    fun stop() {
        stopRequested = true
        engine.stop()
    }

    // ---------------- السجل ----------------

    private fun saveToHistory(caseText: String, vitals: Vitals, answer: String) {
        val parsed = AnswerParser.parse(answer)
        val item = HistoryItem(
            id = System.currentTimeMillis(),
            time = System.currentTimeMillis(),
            caseText = caseText,
            vitals = vitals,
            answer = answer,
            title = parsed.title,
            mode = parsed.mode,
            urgency = parsed.urgency,
        )
        val list = listOf(item) + _state.value.history
        _state.update { it.copy(history = list, viewingHistoryId = item.id) }
        runCatching { historyStore.save(list) }
    }

    fun openHistory(item: HistoryItem) {
        if (_state.value.isGenerating) return
        val refs = runCatching { library?.search(item.caseText).orEmpty() }.getOrDefault(emptyList())
        _state.update {
            it.copy(
                screen = Screen.Result,
                stage = Stage.Done,
                output = item.answer,
                parsed = AnswerParser.parse(item.answer),
                references = refs,
                resultCase = item.caseText,
                resultVitals = item.vitals,
                viewingHistoryId = item.id,
                error = null,
                tokensPerSec = 0f,
                elapsedMs = 0L,
                promptProgress = 1f,
            )
        }
    }

    fun deleteHistory(id: Long) {
        val list = _state.value.history.filterNot { it.id == id }
        _state.update { it.copy(history = list) }
        viewModelScope.launch(Dispatchers.IO) { runCatching { historyStore.save(list) } }
    }

    fun clearHistory() {
        _state.update { it.copy(history = emptyList()) }
        viewModelScope.launch(Dispatchers.IO) { runCatching { historyStore.save(emptyList()) } }
    }

    // ---------------- الإعدادات ----------------

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val ns = transform(_state.value.settings)
        settingsStore.save(ns)
        _state.update { it.copy(settings = ns) }
    }

    override fun onCleared() {
        engine.stop()
        Thread { engine.release() }.start()
        super.onCleared()
    }
}

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
import com.hakim.clinical.data.Backup
import com.hakim.clinical.data.HistoryItem
import com.hakim.clinical.data.HistoryStore
import com.hakim.clinical.data.ParsedAnswer
import com.hakim.clinical.data.Patient
import com.hakim.clinical.data.PatientStore
import com.hakim.clinical.data.PdfExporter
import com.hakim.clinical.data.PromptBuilder
import com.hakim.clinical.data.RefHit
import com.hakim.clinical.data.ReferenceLibrary
import com.hakim.clinical.data.SettingsStore
import com.hakim.clinical.data.Visit
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class Screen { Setup, Patients, PatientEdit, PatientDetail, Visit, Quick, Result, History, Settings }
enum class Stage { Idle, Reading, Writing, Done, Stopped, Error }
enum class ModelStatus { None, Loading, Ready, Failed }
enum class AnalysisTarget { Quick, Visit }

data class ImportState(
    val active: Boolean = false,
    val fraction: Float = 0f,
    val copied: Long = 0L,
    val total: Long = 0L,
    val error: String? = null,
)

data class UiState(
    val screen: Screen = Screen.Patients,
    val settings: AppSettings = AppSettings(),
    val deviceRamBytes: Long = 0L,
    val modelStatus: ModelStatus = ModelStatus.None,
    val modelDesc: String = "",
    val systemInfo: String = "",
    val importState: ImportState = ImportState(),
    // تحليل سريع
    val caseText: String = "",
    val vitals: Vitals = Vitals(),
    // حالة التحليل (مشتركة بين السريع والزيارة)
    val stage: Stage = Stage.Idle,
    val promptProgress: Float = 0f,
    val output: String = "",
    val parsed: ParsedAnswer? = null,
    val references: List<RefHit> = emptyList(),
    val tokensPerSec: Float = 0f,
    val elapsedMs: Long = 0L,
    val error: String? = null,
    val analysisTarget: AnalysisTarget = AnalysisTarget.Quick,
    val analysisVisitId: String? = null,
    val resultCase: String = "",
    val resultVitals: Vitals = Vitals(),
    val resultFrom: Screen = Screen.Quick,
    val viewingHistoryId: Long? = null,
    val history: List<HistoryItem> = emptyList(),
    // المرضى
    val patients: List<Patient> = emptyList(),
    val patientQuery: String = "",
    val selectedPatientId: String? = null,
    val patientDraft: Patient? = null,
    val patientDraftIsNew: Boolean = false,
    val visitDraft: Visit? = null,
    val visitDraftIsNew: Boolean = false,
    val detailTab: Int = 0,
    // عام
    val locked: Boolean = false,
    val toast: String? = null,
    val storageError: String? = null,
    val busy: Boolean = false,
) {
    val isGenerating: Boolean get() = stage == Stage.Reading || stage == Stage.Writing
    val selectedPatient: Patient? get() = patients.firstOrNull { it.id == selectedPatientId }
    val modelReady: Boolean get() = modelStatus == ModelStatus.Ready
}

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val settingsStore = SettingsStore(app)
    private val historyStore = HistoryStore(app)
    private val patientStore = PatientStore(app)
    private val modelManager = ModelManager(app)
    private val engine = InferenceEngine()
    private val ioLock = Mutex()

    @Volatile
    private var library: ReferenceLibrary? = null

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var genJob: Job? = null
    private var importJob: Job? = null

    @Volatile
    private var stopRequested = false
    private var backgroundAt = 0L

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
                locked = s.lockEnabled,
                screen = if (s.modelPath == null && !s.setupSkipped) Screen.Setup else Screen.Patients,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            library = runCatching { ReferenceLibrary.load(app) }.getOrNull()
            val loaded = patientStore.load()
            val h = runCatching { historyStore.load() }.getOrDefault(emptyList())
            val info = engine.systemInfo()
            _state.update {
                it.copy(
                    patients = loaded.patients.sortedByDescending { p -> p.updatedAt },
                    storageError = loaded.error,
                    history = h,
                    systemInfo = info,
                )
            }
        }
        s.modelPath?.let { loadModel(it) }
    }

    // ================= عام =================

    fun toast(msg: String) = _state.update { it.copy(toast = msg) }

    fun consumeToast() = _state.update { it.copy(toast = null) }

    fun navigate(screen: Screen) = _state.update { it.copy(screen = screen) }

    fun skipSetup() {
        updateSettings { it.copy(setupSkipped = true) }
        navigate(Screen.Patients)
    }

    /** زر الرجوع. يرجّع false إذا لازم يطلع من التطبيق. */
    fun back(): Boolean {
        val s = _state.value
        val target = when (s.screen) {
            Screen.PatientEdit -> if (s.patientDraftIsNew) Screen.Patients else Screen.PatientDetail
            Screen.PatientDetail -> Screen.Patients
            Screen.Visit -> Screen.PatientDetail
            Screen.Result -> s.resultFrom
            Screen.Quick, Screen.History, Screen.Settings -> Screen.Patients
            Screen.Setup -> if (s.settings.modelPath != null || s.settings.setupSkipped) Screen.Patients else null
            Screen.Patients -> null
        } ?: return false
        _state.update { it.copy(screen = target) }
        return true
    }

    fun onBackground() {
        backgroundAt = SystemClock.elapsedRealtime()
    }

    fun onForeground() {
        val s = _state.value
        if (s.settings.lockEnabled && backgroundAt > 0 && SystemClock.elapsedRealtime() - backgroundAt > LOCK_AFTER_MS) {
            _state.update { it.copy(locked = true) }
        }
        backgroundAt = 0L
    }

    fun unlock() = _state.update { it.copy(locked = false) }

    // ================= النموذج =================

    private fun loadModel(path: String) {
        _state.update { it.copy(modelStatus = ModelStatus.Loading, modelDesc = "") }
        viewModelScope.launch(Dispatchers.IO) {
            val desc = runCatching { engine.load(path) }.getOrNull()
            _state.update {
                it.copy(modelStatus = if (desc != null) ModelStatus.Ready else ModelStatus.Failed, modelDesc = desc ?: "")
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
                        screen = if (it.screen == Screen.Setup) Screen.Patients else it.screen,
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

    // ================= التحليل السريع =================

    fun updateCase(text: String) = _state.update { it.copy(caseText = text) }

    fun updateVitals(v: Vitals) = _state.update { it.copy(vitals = v) }

    fun clearCase() = _state.update { it.copy(caseText = "", vitals = Vitals()) }

    fun fillExample() {
        val ex = library?.randomExample() ?: return
        _state.update { it.copy(caseText = ex, vitals = Vitals()) }
    }

    fun editAgain() = _state.update { it.copy(caseText = it.resultCase, vitals = it.resultVitals, screen = Screen.Quick) }

    fun newQuickCase() = _state.update { it.copy(caseText = "", vitals = Vitals(), screen = Screen.Quick) }

    fun analyzeQuick() {
        val st = _state.value
        if (st.caseText.isBlank() || !st.modelReady || genJob?.isActive == true) return
        val refs = searchRefs(st.caseText)
        val user = PromptBuilder.buildUser(st.caseText, st.vitals, refs, st.settings.useReferences)
        _state.update {
            it.copy(
                screen = Screen.Result,
                resultFrom = Screen.Quick,
                resultCase = st.caseText,
                resultVitals = st.vitals,
                viewingHistoryId = null,
            )
        }
        runAnalysis(user, refs, AnalysisTarget.Quick, null) { answer ->
            saveToHistory(st.caseText, st.vitals, answer)
        }
    }

    // ================= تحليل زيارة =================

    fun analyzeVisit() {
        val st = _state.value
        val patient = st.selectedPatient ?: return
        val visit = st.visitDraft ?: return
        if (!st.modelReady || genJob?.isActive == true) return
        if (visit.complaint.isBlank() && visit.exam.isBlank() && visit.labs.isBlank()) {
            toast("اكتب الشكوى أو الفحص أول شي.")
            return
        }
        val searchText = listOf(visit.complaint, visit.exam).joinToString(" ")
        val refs = searchRefs(searchText)
        val user = PromptBuilder.buildVisit(patient, visit, refs, st.settings.useReferences)
        val visitId = visit.id
        runAnalysis(user, refs, AnalysisTarget.Visit, visitId) { answer ->
            _state.update {
                val d = it.visitDraft
                if (d != null && d.id == visitId) it.copy(visitDraft = d.copy(analysis = answer)) else it
            }
        }
    }

    private fun searchRefs(text: String): List<RefHit> =
        runCatching { library?.search(text).orEmpty() }.getOrDefault(emptyList())

    private fun runAnalysis(
        user: String,
        refs: List<RefHit>,
        target: AnalysisTarget,
        visitId: String?,
        onDone: (String) -> Unit,
    ) {
        val st = _state.value
        stopRequested = false
        val cfg = GenConfig(
            nCtx = st.settings.nCtx,
            nThreads = st.settings.threads,
            maxTokens = st.settings.maxTokens,
            temperature = st.settings.temperature,
        )
        _state.update {
            it.copy(
                stage = Stage.Reading,
                promptProgress = 0f,
                output = "",
                parsed = null,
                references = refs,
                tokensPerSec = 0f,
                elapsedMs = 0L,
                error = null,
                analysisTarget = target,
                analysisVisitId = visitId,
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
                it.copy(stage = stage, error = if (code < 0) InferenceEngine.errorMessage(code) else null)
            }
            if (code >= 0 && sb.isNotBlank()) onDone(sb.toString())
        }
    }

    fun stop() {
        stopRequested = true
        engine.stop()
    }

    // ================= السجل =================

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
        _state.update { it.copy(history = listOf(item) + it.history, viewingHistoryId = item.id) }
        persistHistory()
    }

    fun openHistory(item: HistoryItem) {
        if (_state.value.isGenerating) return
        _state.update {
            it.copy(
                screen = Screen.Result,
                resultFrom = Screen.History,
                stage = Stage.Done,
                output = item.answer,
                parsed = AnswerParser.parse(item.answer),
                references = searchRefs(item.caseText),
                resultCase = item.caseText,
                resultVitals = item.vitals,
                viewingHistoryId = item.id,
                analysisTarget = AnalysisTarget.Quick,
                analysisVisitId = null,
                error = null,
                tokensPerSec = 0f,
                elapsedMs = 0L,
                promptProgress = 1f,
            )
        }
    }

    fun deleteHistory(id: Long) {
        _state.update { it.copy(history = it.history.filterNot { h -> h.id == id }) }
        persistHistory()
    }

    fun clearHistory() {
        _state.update { it.copy(history = emptyList()) }
        persistHistory()
    }

    private fun persistHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            ioLock.withLock { runCatching { historyStore.save(_state.value.history) } }
        }
    }

    // ================= المرضى =================

    private fun persistPatients() {
        viewModelScope.launch(Dispatchers.IO) {
            ioLock.withLock {
                runCatching { patientStore.save(_state.value.patients) }
                    .onFailure { toast("ما قدرت احفظ البيانات: ${it.message}") }
            }
        }
    }

    fun setPatientQuery(q: String) = _state.update { it.copy(patientQuery = q) }

    fun openPatient(id: String) = _state.update {
        it.copy(selectedPatientId = id, detailTab = 0, screen = Screen.PatientDetail)
    }

    fun setDetailTab(i: Int) = _state.update { it.copy(detailTab = i) }

    fun newPatient() = _state.update {
        it.copy(patientDraft = Patient(), patientDraftIsNew = true, screen = Screen.PatientEdit)
    }

    fun editPatient() {
        val p = _state.value.selectedPatient ?: return
        _state.update { it.copy(patientDraft = p, patientDraftIsNew = false, screen = Screen.PatientEdit) }
    }

    fun updatePatientDraft(transform: (Patient) -> Patient) = _state.update { s ->
        s.patientDraft?.let { s.copy(patientDraft = transform(it)) } ?: s
    }

    fun savePatient() {
        val d = _state.value.patientDraft ?: return
        if (d.name.isBlank() && d.fileNo.isBlank()) {
            toast("اكتب اسم المريض أو رقم الإضبارة.")
            return
        }
        val saved = d.copy(updatedAt = System.currentTimeMillis())
        _state.update { s ->
            val others = s.patients.filterNot { it.id == saved.id }
            s.copy(
                patients = listOf(saved) + others,
                selectedPatientId = saved.id,
                patientDraft = null,
                screen = Screen.PatientDetail,
                toast = if (s.patientDraftIsNew) "انضاف المريض ✓" else "انحفظت التعديلات ✓",
            )
        }
        persistPatients()
    }

    fun deletePatient() {
        val id = _state.value.selectedPatientId ?: return
        _state.update { s ->
            s.copy(
                patients = s.patients.filterNot { it.id == id },
                selectedPatientId = null,
                screen = Screen.Patients,
                toast = "انحذف ملف المريض",
            )
        }
        persistPatients()
    }

    // ================= الزيارات =================

    fun newVisit() {
        if (_state.value.selectedPatient == null) return
        _state.update { it.copy(visitDraft = Visit(), visitDraftIsNew = true, screen = Screen.Visit) }
    }

    fun openVisit(v: Visit) = _state.update { it.copy(visitDraft = v, visitDraftIsNew = false, screen = Screen.Visit) }

    fun updateVisitDraft(transform: (Visit) -> Visit) = _state.update { s ->
        s.visitDraft?.let { s.copy(visitDraft = transform(it)) } ?: s
    }

    fun saveVisit() {
        val st = _state.value
        val pid = st.selectedPatientId ?: return
        val v = st.visitDraft ?: return
        if (v.isEmpty && v.analysis.isBlank()) {
            toast("الزيارة فاضية.")
            return
        }
        val now = System.currentTimeMillis()
        val saved = v.copy(updatedAt = now)
        _state.update { s ->
            s.copy(
                patients = s.patients.map { p ->
                    if (p.id != pid) p
                    else p.copy(
                        visits = (p.visits.filterNot { it.id == saved.id } + saved).sortedByDescending { it.date },
                        updatedAt = now,
                    )
                }.sortedByDescending { it.updatedAt },
                visitDraft = null,
                detailTab = 0,
                screen = Screen.PatientDetail,
                toast = "انحفظت الزيارة ✓",
            )
        }
        persistPatients()
    }

    fun deleteVisit() {
        val st = _state.value
        val pid = st.selectedPatientId ?: return
        val vid = st.visitDraft?.id ?: return
        _state.update { s ->
            s.copy(
                patients = s.patients.map { p -> if (p.id != pid) p else p.copy(visits = p.visits.filterNot { it.id == vid }) },
                visitDraft = null,
                screen = Screen.PatientDetail,
                toast = "انحذفت الزيارة",
            )
        }
        persistPatients()
    }

    // ================= تصدير ونسخ احتياطي =================

    fun exportPdf(uri: Uri) {
        val p = _state.value.selectedPatient ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                app.contentResolver.openOutputStream(uri)?.use { PdfExporter.write(app, p, it) } ?: error("no stream")
            }.isSuccess
            toast(if (ok) "انحفظ ملف الـ PDF ✓" else "ما قدرت احفظ الـ PDF.")
        }
    }

    fun exportBackup(uri: Uri, password: String) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val s = _state.value
            val ok = runCatching {
                val bytes = Backup.export(s.patients, s.history, password.toCharArray())
                app.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("no stream")
            }.isSuccess
            _state.update {
                it.copy(busy = false, toast = if (ok) "انحفظت النسخة الاحتياطية ✓ لا تنسى كلمة السر." else "فشل حفظ النسخة الاحتياطية.")
            }
        }
    }

    fun importBackup(uri: Uri, password: String) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val bytes = app.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("no stream")
                Backup.restore(bytes, password.toCharArray())
            }
            val imported = result.getOrNull()
            if (imported == null) {
                _state.update { it.copy(busy = false, toast = "كلمة السر غلط أو الملف مو نسخة DrAli.") }
                return@launch
            }
            _state.update { s ->
                val ids = imported.patients.map { it.id }.toSet()
                val hIds = imported.history.map { it.id }.toSet()
                s.copy(
                    patients = (imported.patients + s.patients.filterNot { it.id in ids }).sortedByDescending { it.updatedAt },
                    history = (imported.history + s.history.filterNot { it.id in hIds }).sortedByDescending { it.time },
                    busy = false,
                    storageError = null,
                    toast = "انسترجع ${imported.patients.size} مريض و ${imported.history.size} تحليل ✓",
                )
            }
            persistPatients()
            persistHistory()
        }
    }

    // ================= الإعدادات =================

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

    companion object {
        private const val LOCK_AFTER_MS = 60_000L
    }
}

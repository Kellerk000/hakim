package com.hakim.clinical.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hakim.clinical.data.AnswerParser
import com.hakim.clinical.data.Mode
import com.hakim.clinical.data.ParsedAnswer
import com.hakim.clinical.data.Visit
import com.hakim.clinical.ui.AnalysisTarget
import com.hakim.clinical.ui.ModelStatus
import com.hakim.clinical.ui.Stage
import com.hakim.clinical.ui.UiState
import com.hakim.clinical.ui.components.ActionIcon
import com.hakim.clinical.ui.components.AnswerCards
import com.hakim.clinical.ui.components.AppCard
import com.hakim.clinical.ui.components.AppField
import com.hakim.clinical.ui.components.Gap
import com.hakim.clinical.ui.components.InfoBanner
import com.hakim.clinical.ui.components.Pill
import com.hakim.clinical.ui.components.PrimaryButton
import com.hakim.clinical.ui.components.ReadingCard
import com.hakim.clinical.ui.components.SecondaryButton
import com.hakim.clinical.ui.components.SectionTitle
import com.hakim.clinical.ui.components.TopBar
import com.hakim.clinical.ui.components.TypingDots
import com.hakim.clinical.ui.components.VitalsEditor
import com.hakim.clinical.ui.components.WritingHint
import com.hakim.clinical.ui.components.formatDate
import com.hakim.clinical.ui.theme.Hk
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisitScreen(
    state: UiState,
    onBack: () -> Unit,
    onChange: ((Visit) -> Visit) -> Unit,
    onAnalyze: () -> Unit,
    onStop: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val p = state.selectedPatient ?: return
    val v = state.visitDraft ?: return
    val c = Hk.c
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    val live = state.analysisTarget == AnalysisTarget.Visit && state.analysisVisitId == v.id &&
        (state.isGenerating || state.output.isNotBlank() || state.error != null)
    val shownParsed: ParsedAnswer? = when {
        live -> state.parsed
        v.analysis.isNotBlank() -> AnswerParser.parse(v.analysis)
        else -> null
    }
    val shownRaw = if (live) state.output else v.analysis

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 18.dp),
    ) {
        TopBar(if (state.visitDraftIsNew) "زيارة جديدة" else "الزيارة", onBack = onBack, subtitle = p.displayName) {
            if (!state.visitDraftIsNew) ActionIcon(Icons.Rounded.DeleteOutline, "حذف", { confirmDelete = true }, tint = c.danger)
            ActionIcon(Icons.Rounded.Check, "حفظ", onSave, tint = c.primary)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (p.allergies.isNotBlank()) {
                InfoBanner("انتبه: المريض عنده تحسس من ${p.allergies.trim()}", c.danger, Icons.Rounded.WarningAmber)
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        val cal = Calendar.getInstance().apply { timeInMillis = v.date }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val nc = Calendar.getInstance().apply {
                                    timeInMillis = v.date
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                onChange { it.copy(date = nc.timeInMillis) }
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Pill(formatDate(v.date), c.primary, icon = Icons.Rounded.CalendarToday)
                Text("  تغيير التاريخ", style = MaterialTheme.typography.labelMedium)
            }

            AppCard(Modifier.fillMaxWidth()) {
                SectionTitle("الشكوى والقصة", Icons.Rounded.Description, c.primary)
                Gap(10.dp)
                AppField(
                    v.complaint, { t -> onChange { it.copy(complaint = t) } }, "اكتبها متل ما بتحكيها",
                    placeholder = "الشكوى ومدتها، الأعراض المرافقة، شو جرّب من أدوية…", minLines = 5,
                )
                Gap(10.dp)
                AppField(
                    v.exam, { t -> onChange { it.copy(exam = t) } }, "الفحص السريري",
                    placeholder = "الحالة العامة، القلب، الصدر، البطن…", minLines = 3,
                )
            }

            AppCard(Modifier.fillMaxWidth()) {
                SectionTitle("العلامات الحيوية", Icons.Rounded.Favorite, c.pink)
                Gap(12.dp)
                VitalsEditor(v.vitals, { nv -> onChange { it.copy(vitals = nv) } })
            }

            AppCard(Modifier.fillMaxWidth()) {
                SectionTitle("التحاليل والصور", Icons.Rounded.Timeline, c.info)
                Gap(10.dp)
                AppField(v.labs, { t -> onChange { it.copy(labs = t) } }, "النتائج", placeholder = "CBC، سكر، شوارد، صورة صدر…", minLines = 2)
            }

            // ---------- التحليل ----------
            if (state.modelStatus != ModelStatus.Ready) {
                InfoBanner(
                    when (state.modelStatus) {
                        ModelStatus.Loading -> "عم يتحمّل النموذج… استنى ثواني."
                        ModelStatus.Failed -> "ما قدر يحمّل النموذج. جرّب ملف تاني من الإعدادات."
                        else -> "لتفعيل التحليل الذكي، اختار ملف النموذج من الإعدادات."
                    },
                    c.warning,
                    Icons.Rounded.ErrorOutline,
                ) {
                    if (state.modelStatus != ModelStatus.Loading) {
                        SecondaryButton("الإعدادات", onClick = onOpenSettings, icon = Icons.Rounded.FolderOpen)
                    }
                }
            } else if (live && state.isGenerating) {
                SecondaryButton("إيقاف التحليل", onClick = onStop, icon = Icons.Rounded.Stop, color = c.danger, modifier = Modifier.fillMaxWidth())
            } else {
                PrimaryButton(
                    if (v.analysis.isNotBlank()) "أعد التحليل بـ DrAli" else "حلّل بـ DrAli",
                    onClick = onAnalyze,
                    icon = Icons.Rounded.AutoAwesome,
                    enabled = !state.isGenerating,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (live && state.stage == Stage.Reading) ReadingCard(state.promptProgress)
            val hasSections = shownParsed != null && shownParsed.sectionsFound > 0
            if (live && state.stage == Stage.Writing && !hasSections) WritingHint()
            if (shownParsed != null) AnswerCards(shownParsed, live && state.isGenerating, shownRaw)
            if (live && state.isGenerating && hasSections) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
                    TypingDots()
                    Text("  عم يكمّل الكتابة…", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (live && state.error != null) InfoBanner(state.error, c.danger, Icons.Rounded.ErrorOutline)

            // ---------- القرار ----------
            AppCard(Modifier.fillMaxWidth(), accent = c.primary) {
                SectionTitle("قرارك", Icons.Rounded.LocalHospital, c.primary)
                val suggestions = when {
                    shownParsed == null -> emptyList()
                    shownParsed.mode == Mode.MULTIPLE -> shownParsed.candidates.take(3)
                    else -> listOfNotNull(shownParsed.top)
                }.filter { it.isNotBlank() && it != v.diagnosis }
                if (suggestions.isNotEmpty() && !(live && state.isGenerating)) {
                    Gap(10.dp)
                    Text("اعتمد من اقتراحات DrAli:", style = MaterialTheme.typography.labelMedium)
                    Gap(6.dp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggestions.forEach { s ->
                            Pill(
                                "+ $s",
                                c.secondary,
                                modifier = Modifier.clickable { onChange { it.copy(diagnosis = s) } },
                            )
                        }
                    }
                }
                Gap(12.dp)
                AppField(v.diagnosis, { t -> onChange { it.copy(diagnosis = t) } }, "التشخيص")
                Gap(10.dp)
                AppField(v.plan, { t -> onChange { it.copy(plan = t) } }, "العلاج والخطة", placeholder = "الأدوية، التحاليل المطلوبة، موعد المراجعة…", minLines = 3)
            }

            PrimaryButton("حفظ الزيارة", onClick = onSave, icon = Icons.Rounded.Check, modifier = Modifier.fillMaxWidth())
            Text(
                "DrAli أداة مساعدة، والقرار السريري إلك.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Gap(40.dp)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف الزيارة؟") },
            text = { Text("ما فيك ترجعها بعد الحذف.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("حذف", color = c.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("إلغاء") } },
            containerColor = c.surface,
        )
    }
}

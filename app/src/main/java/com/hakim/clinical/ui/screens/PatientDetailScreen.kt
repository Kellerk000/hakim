package com.hakim.clinical.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Opacity
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hakim.clinical.data.AnswerParser
import com.hakim.clinical.data.Mode
import com.hakim.clinical.data.Patient
import com.hakim.clinical.data.Urgency
import com.hakim.clinical.data.Visit
import com.hakim.clinical.data.bloodPressure
import com.hakim.clinical.data.vitalNumber
import com.hakim.clinical.ui.UiState
import com.hakim.clinical.ui.components.ActionIcon
import com.hakim.clinical.ui.components.AppCard
import com.hakim.clinical.ui.components.Avatar
import com.hakim.clinical.ui.components.ChartSeries
import com.hakim.clinical.ui.components.EmptyState
import com.hakim.clinical.ui.components.Gap
import com.hakim.clinical.ui.components.Pill
import com.hakim.clinical.ui.components.PrimaryButton
import com.hakim.clinical.ui.components.SecondaryButton
import com.hakim.clinical.ui.components.SectionTitle
import com.hakim.clinical.ui.components.Segmented
import com.hakim.clinical.ui.components.TopBar
import com.hakim.clinical.ui.components.VitalChart
import com.hakim.clinical.ui.components.formatDate
import com.hakim.clinical.ui.components.friendlyDate
import com.hakim.clinical.ui.theme.Hk

@Composable
fun PatientDetailScreen(
    state: UiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onNewVisit: () -> Unit,
    onOpenVisit: (Visit) -> Unit,
    onTab: (Int) -> Unit,
    onExportPdf: (Uri) -> Unit,
) {
    val p = state.selectedPatient ?: return
    val c = Hk.c
    var confirmDelete by remember { mutableStateOf(false) }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) onExportPdf(uri)
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp),
    ) {
        TopBar("ملف المريض", onBack = onBack) {
            ActionIcon(Icons.Rounded.Edit, "تعديل", onEdit)
            ActionIcon(Icons.Rounded.DeleteOutline, "حذف", { confirmDelete = true }, tint = c.danger)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProfileHeader(p)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("زيارة جديدة", onClick = onNewVisit, icon = Icons.Rounded.Add, modifier = Modifier.weight(1.4f))
                SecondaryButton(
                    "PDF",
                    onClick = { pdfLauncher.launch("DrAli-${p.displayName.replace(' ', '-')}.pdf") },
                    icon = Icons.Rounded.PictureAsPdf,
                    modifier = Modifier.weight(1f).height(56.dp),
                )
            }
            Segmented(
                options = listOf(0, 1, 2),
                selected = state.detailTab,
                label = { listOf("الزيارات (${p.visits.size})", "المؤشرات", "المعلومات")[it] },
                onSelect = onTab,
            )
            when (state.detailTab) {
                0 -> VisitsTab(p, onOpenVisit, onNewVisit)
                1 -> ChartsTab(p)
                else -> InfoTab(p)
            }
            Gap(40.dp)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف ملف ${p.displayName}؟") },
            text = { Text("رح تنحذف كل معلومات المريض وزياراته، وما فيك ترجعها إلا من نسخة احتياطية.") },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileHeader(p: Patient) {
    val c = Hk.c
    AppCard(Modifier.fillMaxWidth(), padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.id, p.initials, size = 62.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(p.displayName, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (p.subtitle.isNotBlank()) Text(p.subtitle, style = MaterialTheme.typography.bodySmall)
                if (p.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(Icons.Rounded.Phone, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(14.dp))
                        Text("  ${p.phone}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        val chips = p.history.split('،', ',', '\n').map { it.trim() }.filter { it.isNotEmpty() }.take(4)
        if (p.allergies.isNotBlank() || chips.isNotEmpty()) {
            Gap(12.dp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (p.allergies.isNotBlank()) AllergyPill(p.allergies.trim().take(40))
                chips.forEach { Pill(it, c.info) }
            }
        }
    }
}

@Composable
private fun VisitsTab(p: Patient, onOpen: (Visit) -> Unit, onNew: () -> Unit) {
    val visits = p.visits.sortedByDescending { it.date }
    if (visits.isEmpty()) {
        EmptyState(
            Icons.Rounded.Description,
            "ما في زيارات لسا",
            "سجّل أول زيارة: الشكوى، الفحص، العلامات الحيوية، وخلّي DrAli يحللها.",
        ) {
            PrimaryButton("أول زيارة", onClick = onNew, icon = Icons.Rounded.Add)
        }
        return
    }
    Column {
        visits.forEachIndexed { i, v ->
            VisitTimelineRow(v, isLast = i == visits.lastIndex, onOpen = onOpen)
        }
    }
}

@Composable
private fun VisitTimelineRow(v: Visit, isLast: Boolean, onOpen: (Visit) -> Unit) {
    val c = Hk.c
    val parsed = if (v.analysis.isNotBlank()) AnswerParser.parse(v.analysis) else null
    val dotColor = when (parsed?.urgency) {
        Urgency.EMERGENCY -> c.danger
        Urgency.URGENT -> c.warning
        Urgency.ROUTINE -> c.success
        else -> c.primary
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(22.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Gap(20.dp)
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(c.border)
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 8.dp, bottom = 12.dp)) {
            AppCard(Modifier.fillMaxWidth(), padding = 14.dp, onClick = { onOpen(v) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(friendlyDate(v.date), style = MaterialTheme.typography.labelLarge.copy(color = c.primary), modifier = Modifier.weight(1f))
                    if (parsed != null) {
                        Pill(if (parsed.mode == Mode.MULTIPLE) "احتمالات" else "محلّلة", c.secondary)
                    }
                }
                if (v.complaint.isNotBlank()) {
                    Gap(6.dp)
                    Text(v.complaint.trim(), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (v.diagnosis.isNotBlank()) {
                    Gap(8.dp)
                    Pill(v.diagnosis.trim(), c.primary, icon = Icons.Rounded.LocalHospital)
                }
                val vit = vitalsSummary(v)
                if (vit.isNotBlank()) {
                    Gap(8.dp)
                    Text(vit, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun vitalsSummary(v: Visit): String = listOfNotNull(
    v.vitals.bp.takeIf { it.isNotBlank() }?.let { "ضغط $it" },
    v.vitals.hr.takeIf { it.isNotBlank() }?.let { "نبض $it" },
    v.vitals.temp.takeIf { it.isNotBlank() }?.let { "حرارة $it" },
    v.vitals.spo2.takeIf { it.isNotBlank() }?.let { "أكسجة $it٪" },
    v.vitals.glucose.takeIf { it.isNotBlank() }?.let { "سكر $it" },
    v.vitals.weight.takeIf { it.isNotBlank() }?.let { "وزن $it" },
).joinToString(" · ")

private class Spec(val title: String, val unit: String, val icon: ImageVector, val series: List<ChartSeries>)

@Composable
private fun ChartsTab(p: Patient) {
    val c = Hk.c
    val visits = p.visits.filter { !it.vitals.isEmpty() }.sortedBy { it.date }
    if (visits.isEmpty()) {
        EmptyState(Icons.Rounded.Favorite, "ما في علامات حيوية مسجلة", "سجّل الضغط والنبض والسكر بالزيارات، وهون بتطلع المنحنيات.")
        return
    }
    val dates = visits.map { it.date }
    val bp = visits.map { bloodPressure(it.vitals.bp) }

    val specs = listOf(
        Spec(
            "الضغط الشرياني", "مم زئبق", Icons.Rounded.Speed,
            listOf(
                ChartSeries("انقباضي", c.danger, bp.map { it?.first }),
                ChartSeries("انبساطي", c.info, bp.map { it?.second }),
            ),
        ),
        Spec("النبض", "نبضة/د", Icons.Rounded.Favorite, listOf(ChartSeries("النبض", c.pink, visits.map { vitalNumber(it.vitals.hr) }))),
        Spec("سكر الدم", "ملغ/دل", Icons.Rounded.Opacity, listOf(ChartSeries("السكر", c.warning, visits.map { vitalNumber(it.vitals.glucose) }))),
        Spec("الحرارة", "°م", Icons.Rounded.Thermostat, listOf(ChartSeries("الحرارة", c.secondary, visits.map { vitalNumber(it.vitals.temp) }))),
        Spec("الأكسجة", "٪", Icons.Rounded.Timeline, listOf(ChartSeries("الأكسجة", c.primary, visits.map { vitalNumber(it.vitals.spo2) }))),
        Spec("الوزن", "كغ", Icons.Rounded.FitnessCenter, listOf(ChartSeries("الوزن", c.success, visits.map { vitalNumber(it.vitals.weight) }))),
    ).filter { s -> s.series.any { ser -> ser.values.any { it != null } } }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        specs.forEach { s -> VitalChart(s.title, s.unit, s.icon, dates, s.series) }
    }
}

@Composable
private fun InfoTab(p: Patient) {
    val c = Hk.c
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard("السوابق المرضية", p.history, c.info, Icons.Rounded.LocalHospital)
        InfoCard("الأدوية الدائمة", p.meds, c.primary, Icons.Rounded.LocalPharmacy)
        InfoCard("التحسس", p.allergies, c.danger, Icons.Rounded.Favorite)
        InfoCard("العمليات السابقة", p.surgeries, c.secondary, Icons.Rounded.Description)
        InfoCard("ملاحظات", p.notes, c.textDim, Icons.Rounded.Edit)
        Text(
            "تاريخ فتح الملف: ${formatDate(p.createdAt)}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

@Composable
private fun InfoCard(title: String, body: String, color: androidx.compose.ui.graphics.Color, icon: ImageVector) {
    AppCard(Modifier.fillMaxWidth(), padding = 16.dp) {
        SectionTitle(title, icon, color)
        Gap(8.dp)
        Text(
            body.ifBlank { "ما في شي مسجّل" },
            style = if (body.isBlank()) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        )
    }
}

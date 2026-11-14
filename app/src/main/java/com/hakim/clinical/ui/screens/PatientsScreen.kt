package com.hakim.clinical.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hakim.clinical.data.ArabicText
import com.hakim.clinical.data.Patient
import com.hakim.clinical.ui.ModelStatus
import com.hakim.clinical.ui.UiState
import com.hakim.clinical.ui.components.AppCard
import com.hakim.clinical.ui.components.AppField
import com.hakim.clinical.ui.components.Avatar
import com.hakim.clinical.ui.components.BrandText
import com.hakim.clinical.ui.components.EmptyState
import com.hakim.clinical.ui.components.Gap
import com.hakim.clinical.ui.components.InfoBanner
import com.hakim.clinical.ui.components.Pill
import com.hakim.clinical.ui.components.PrimaryButton
import com.hakim.clinical.ui.components.StatCard
import com.hakim.clinical.ui.components.friendlyDate
import com.hakim.clinical.ui.components.isToday
import com.hakim.clinical.ui.theme.Hk

@Composable
fun PatientsScreen(
    state: UiState,
    onQuery: (String) -> Unit,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
) {
    val c = Hk.c
    val patients = state.patients
    val filtered = remember(patients, state.patientQuery) {
        val q = ArabicText.normalize(state.patientQuery.trim())
        if (q.isEmpty()) patients
        else patients.filter { p ->
            ArabicText.normalize("${p.name} ${p.fileNo} ${p.phone} ${p.lastVisit?.diagnosis ?: ""}").contains(q)
        }
    }
    val todayVisits = remember(patients) { patients.sumOf { p -> p.visits.count { isToday(it.date) } } }
    val totalVisits = remember(patients) { patients.sumOf { it.visits.size } }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 190.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    BrandText("DrAli", fontSize = 32.sp)
                    Text("عيادتك الذكية، بدون إنترنت", style = MaterialTheme.typography.bodySmall)
                }
                ModelChip(state.modelStatus)
            }
        }
        state.storageError?.let { err ->
            item { InfoBanner(err, c.danger, Icons.Rounded.ErrorOutline) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("${patients.size}", "مريض", Icons.Rounded.People, c.primary, Modifier.weight(1f))
                StatCard("$todayVisits", "زيارات اليوم", Icons.Rounded.CalendarToday, c.info, Modifier.weight(1f))
                StatCard("$totalVisits", "كل الزيارات", Icons.Rounded.DateRange, c.secondary, Modifier.weight(1f))
            }
        }
        if (patients.isNotEmpty()) {
            item {
                AppField(
                    value = state.patientQuery,
                    onValueChange = onQuery,
                    label = "بحث",
                    placeholder = "اسم، رقم إضبارة، هاتف أو تشخيص",
                    singleLine = true,
                    leadingIcon = Icons.Rounded.Search,
                )
            }
            item {
                Text(
                    if (state.patientQuery.isBlank()) "المرضى" else "النتائج (${filtered.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
            }
        }

        if (patients.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.People,
                    "ما في مرضى لسا",
                    "افتح ملف لكل مريض: معلوماته، سوابقه، زياراته، ومنحنيات علاماته الحيوية.",
                ) {
                    PrimaryButton("أضف أول مريض", onClick = onNew, icon = Icons.Rounded.PersonAdd)
                }
            }
        } else if (filtered.isEmpty()) {
            item { EmptyState(Icons.Rounded.Search, "ما في نتائج", "جرّب كلمة تانية.") }
        } else {
            items(filtered, key = { it.id }) { p -> PatientRow(p, onOpen) }
        }
    }
}

@Composable
private fun PatientRow(p: Patient, onOpen: (String) -> Unit) {
    val c = Hk.c
    val last = p.lastVisit
    AppCard(Modifier.fillMaxWidth(), padding = 14.dp, onClick = { onOpen(p.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.id, p.initials)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(p.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (p.subtitle.isNotBlank()) Text(p.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                if (last != null) {
                    Gap(6.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(friendlyDate(last.date), style = MaterialTheme.typography.labelMedium)
                        if (last.diagnosis.isNotBlank()) {
                            Text("  ·  ", style = MaterialTheme.typography.labelMedium)
                            Text(
                                last.diagnosis,
                                style = MaterialTheme.typography.labelMedium.copy(color = c.primary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }
                }
            }
            if (p.allergies.isNotBlank()) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = "تحسس", tint = c.danger, modifier = Modifier.size(18.dp))
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textFaint)
        }
    }
}

@Composable
fun ModelChip(status: ModelStatus) {
    val c = Hk.c
    val (label, color) = when (status) {
        ModelStatus.Ready -> "الذكاء جاهز" to c.success
        ModelStatus.Loading -> "عم يتحمّل" to c.warning
        ModelStatus.Failed -> "فشل التحميل" to c.danger
        ModelStatus.None -> "بدون نموذج" to c.textFaint
    }
    Row(
        Modifier
            .clip(CircleShape)
            .background(c.tint(color))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text("  $label", style = MaterialTheme.typography.labelMedium.copy(color = color))
    }
}

@Composable
fun AllergyPill(text: String) {
    Pill("تحسس: $text", Hk.c.danger, icon = Icons.Rounded.WarningAmber)
}

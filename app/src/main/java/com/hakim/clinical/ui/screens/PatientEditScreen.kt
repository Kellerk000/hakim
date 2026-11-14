package com.hakim.clinical.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AssignmentInd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hakim.clinical.data.Patient
import com.hakim.clinical.ui.components.ActionIcon
import com.hakim.clinical.ui.components.AppCard
import com.hakim.clinical.ui.components.AppField
import com.hakim.clinical.ui.components.Gap
import com.hakim.clinical.ui.components.PrimaryButton
import com.hakim.clinical.ui.components.SectionTitle
import com.hakim.clinical.ui.components.Segmented
import com.hakim.clinical.ui.components.TopBar
import com.hakim.clinical.ui.theme.Hk

@Composable
fun PatientEditScreen(
    draft: Patient,
    isNew: Boolean,
    onChange: ((Patient) -> Patient) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Hk.c
    // العمر بينكتب كنص وبيتحوّل لسنة ولادة
    var ageText by remember(draft.id) { mutableStateOf(draft.age?.toString() ?: "") }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 18.dp),
    ) {
        TopBar(if (isNew) "مريض جديد" else "تعديل المريض", onBack = onBack) {
            ActionIcon(Icons.Rounded.Check, "حفظ", onSave, tint = c.primary)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppCard(Modifier.fillMaxWidth()) {
                SectionTitle("المعلومات الأساسية", Icons.Rounded.Person, c.primary)
                Gap(12.dp)
                AppField(draft.name, { v -> onChange { it.copy(name = v) } }, "الاسم الكامل", singleLine = true)
                Gap(10.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppField(
                        draft.fileNo, { v -> onChange { it.copy(fileNo = v) } }, "رقم الإضبارة",
                        Modifier.weight(1f), singleLine = true, leadingIcon = Icons.Rounded.AssignmentInd,
                    )
                    AppField(
                        ageText,
                        { v ->
                            ageText = v.take(3)
                            onChange { it.withAge(v.take(3)) }
                        },
                        "العمر (سنوات)", Modifier.weight(1f), singleLine = true, keyboardType = KeyboardType.Number,
                    )
                }
                Gap(12.dp)
                Text("الجنس", style = MaterialTheme.typography.labelMedium)
                Gap(6.dp)
                Segmented(
                    options = listOf("ذكر", "أنثى"),
                    selected = draft.sex,
                    label = { it },
                ) { s -> onChange { it.copy(sex = s) } }
                Gap(10.dp)
                AppField(
                    draft.phone, { v -> onChange { it.copy(phone = v) } }, "رقم الهاتف",
                    singleLine = true, keyboardType = KeyboardType.Phone, leadingIcon = Icons.Rounded.Phone,
                )
            }

            AppCard(Modifier.fillMaxWidth()) {
                SectionTitle("القصة الطبية", Icons.Rounded.LocalHospital, c.info)
                Gap(12.dp)
                AppField(draft.history, { v -> onChange { it.copy(history = v) } }, "السوابق المرضية", placeholder = "سكري، ضغط، ربو…", minLines = 2)
                Gap(10.dp)
                AppField(draft.meds, { v -> onChange { it.copy(meds = v) } }, "الأدوية الدائمة", placeholder = "اسم الدوا والجرعة…", minLines = 2)
                Gap(10.dp)
                AppField(draft.allergies, { v -> onChange { it.copy(allergies = v) } }, "التحسس", placeholder = "بنسلين، أسبرين…", accent = c.danger)
                Gap(10.dp)
                AppField(draft.surgeries, { v -> onChange { it.copy(surgeries = v) } }, "العمليات السابقة", minLines = 2)
                Gap(10.dp)
                AppField(draft.notes, { v -> onChange { it.copy(notes = v) } }, "ملاحظات", minLines = 2)
            }

            PrimaryButton(if (isNew) "إضافة المريض" else "حفظ التعديلات", onClick = onSave, icon = Icons.Rounded.Check, modifier = Modifier.fillMaxWidth())
            Gap(30.dp)
        }
    }
}

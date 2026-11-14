package com.hakim.clinical.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hakim.clinical.data.Vitals
import com.hakim.clinical.ui.ModelStatus
import com.hakim.clinical.ui.UiState
import com.hakim.clinical.ui.components.EcgLine
import com.hakim.clinical.ui.components.Gap
import com.hakim.clinical.ui.components.GhostButton
import com.hakim.clinical.ui.components.GlassCard
import com.hakim.clinical.ui.components.GradientButton
import com.hakim.clinical.ui.components.GradientText
import com.hakim.clinical.ui.components.SectionTitle
import com.hakim.clinical.ui.theme.Hk

@Composable
fun HomeScreen(
    state: UiState,
    onCaseChange: (String) -> Unit,
    onVitalsChange: (Vitals) -> Unit,
    onExample: () -> Unit,
    onClear: () -> Unit,
    onAnalyze: () -> Unit,
    onRetryLoad: () -> Unit,
    onPickModel: () -> Unit,
) {
    val ready = state.modelStatus == ModelStatus.Ready
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 18.dp),
    ) {
        Gap(14.dp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                GradientText("حكيم", fontSize = 36.sp)
                Text("التحليل السريري الذكي — بدون إنترنت", style = MaterialTheme.typography.bodySmall)
            }
            ModelChip(state.modelStatus)
        }
        EcgLine(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(vertical = 6.dp),
            color = if (ready) Hk.Cyan else Hk.TextFaint,
        )

        ModelNotice(state.modelStatus, onRetryLoad, onPickModel)

        GlassCard(Modifier.fillMaxWidth(), padding = 14.dp) {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(
                    "القصة المرضية",
                    Icons.Rounded.Description,
                    Hk.Cyan,
                    modifier = Modifier.weight(1f),
                    trailing = if (state.caseText.isNotEmpty()) "${state.caseText.length} حرف" else null,
                )
            }
            TextField(
                value = state.caseText,
                onValueChange = onCaseChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 8,
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = {
                    Text(
                        "اكتب الحالة متل ما بتحكيها، بالفصحى أو بالعامية:\nالعمر والجنس، الشكوى ومدتها، الأعراض المرافقة، السوابق والأدوية، الفحص السريري، والتحاليل إذا في…",
                        color = Hk.TextFaint,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Hk.Cyan,
                    focusedTextColor = Hk.Text,
                    unfocusedTextColor = Hk.Text,
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("حالة مثال", onClick = onExample, icon = Icons.Rounded.AutoAwesome, color = Hk.Sky)
                if (state.caseText.isNotEmpty()) {
                    GhostButton("مسح", onClick = onClear, icon = Icons.Rounded.Clear, color = Hk.TextDim)
                }
            }
        }
        Gap(14.dp)

        VitalsCard(state.vitals, onVitalsChange)
        Gap(20.dp)

        GradientButton(
            text = "حلّل الحالة",
            onClick = onAnalyze,
            icon = Icons.Rounded.AutoAwesome,
            enabled = ready && state.caseText.isNotBlank() && !state.isGenerating,
            modifier = Modifier.fillMaxWidth(),
        )
        Gap(14.dp)
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = Hk.Warning, modifier = Modifier.size(18.dp))
            Text(
                "  كل ما كتبت تفاصيل أكتر (السوابق، الفحص، التحاليل) كل ما كان التحليل أدق.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Gap(130.dp)
    }
}

@Composable
private fun ModelChip(status: ModelStatus) {
    val (label, color) = when (status) {
        ModelStatus.Ready -> "جاهز" to Hk.Success
        ModelStatus.Loading -> "عم يتحمّل" to Hk.Warning
        ModelStatus.Failed -> "فشل التحميل" to Hk.Danger
        ModelStatus.None -> "ما في نموذج" to Hk.TextFaint
    }
    Row(
        Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status == ModelStatus.Loading) {
            CircularProgressIndicator(color = color, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
        } else {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text("  النموذج $label", color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModelNotice(status: ModelStatus, onRetry: () -> Unit, onPick: () -> Unit) {
    when (status) {
        ModelStatus.Loading -> {
            GlassCard(Modifier.fillMaxWidth(), accent = Hk.Warning, padding = 14.dp) {
                Text(
                    "عم يجهّز النموذج بالذاكرة… أول مرة ممكن ياخد كم ثانية.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Gap(14.dp)
        }
        ModelStatus.Failed -> {
            GlassCard(Modifier.fillMaxWidth(), accent = Hk.Danger, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Hk.Danger)
                    Text(
                        "  ما قدر يحمّل النموذج. ممكن الملف ناقص، أو بنية النموذج مو مدعومة، أو الذاكرة ما بتكفي.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Gap(12.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton("إعادة المحاولة", onClick = onRetry, icon = Icons.Rounded.Refresh)
                    GhostButton("ملف تاني", onClick = onPick, icon = Icons.Rounded.FolderOpen)
                }
            }
            Gap(14.dp)
        }
        else -> Unit
    }
}

@Composable
private fun VitalsCard(v: Vitals, onChange: (Vitals) -> Unit) {
    var open by rememberSaveable { mutableStateOf(!v.isEmpty()) }
    GlassCard(Modifier.fillMaxWidth(), padding = 14.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { open = !open }
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(
                "العلامات الحيوية",
                Icons.Rounded.Favorite,
                Hk.Pink,
                modifier = Modifier.weight(1f),
                trailing = if (v.isEmpty()) "اختياري" else null,
            )
            Icon(
                if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = Hk.TextDim,
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VitalField("الضغط", "120/80", v.bp, KeyboardType.Text, Modifier.weight(1f)) { onChange(v.copy(bp = it)) }
                    VitalField("النبض /د", "80", v.hr, KeyboardType.Number, Modifier.weight(1f)) { onChange(v.copy(hr = it)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VitalField("الحرارة °م", "37", v.temp, KeyboardType.Decimal, Modifier.weight(1f)) { onChange(v.copy(temp = it)) }
                    VitalField("الأكسجة ٪", "98", v.spo2, KeyboardType.Number, Modifier.weight(1f)) { onChange(v.copy(spo2 = it)) }
                    VitalField("التنفس /د", "16", v.rr, KeyboardType.Number, Modifier.weight(1f)) { onChange(v.copy(rr = it)) }
                }
            }
        }
    }
}

@Composable
private fun VitalField(
    label: String,
    hint: String,
    value: String,
    type: KeyboardType,
    modifier: Modifier,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(9)) },
        modifier = modifier,
        singleLine = true,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(hint, color = Hk.TextFaint) },
        keyboardOptions = KeyboardOptions(keyboardType = type),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Hk.Pink,
            unfocusedBorderColor = Hk.Stroke,
            focusedLabelColor = Hk.Pink,
            unfocusedLabelColor = Hk.TextDim,
            cursorColor = Hk.Pink,
            focusedTextColor = Hk.Text,
            unfocusedTextColor = Hk.Text,
            focusedContainerColor = Color.White.copy(alpha = 0.04f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
        ),
    )
}

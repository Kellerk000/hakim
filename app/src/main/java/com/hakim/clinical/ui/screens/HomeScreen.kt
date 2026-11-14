package com.hakim.clinical.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hakim.clinical.data.Vitals
import com.hakim.clinical.ui.ModelStatus
import com.hakim.clinical.ui.UiState
import com.hakim.clinical.ui.components.AppCard
import com.hakim.clinical.ui.components.AppField
import com.hakim.clinical.ui.components.EcgLine
import com.hakim.clinical.ui.components.Gap
import com.hakim.clinical.ui.components.InfoBanner
import com.hakim.clinical.ui.components.PrimaryButton
import com.hakim.clinical.ui.components.SecondaryButton
import com.hakim.clinical.ui.components.SectionTitle
import com.hakim.clinical.ui.components.VitalsEditor
import com.hakim.clinical.ui.theme.Hk

/** تحليل سريع لحالة بدون ما تفتح ملف مريض. */
@Composable
fun QuickScreen(
    state: UiState,
    onCaseChange: (String) -> Unit,
    onVitalsChange: (Vitals) -> Unit,
    onExample: () -> Unit,
    onClear: () -> Unit,
    onAnalyze: () -> Unit,
    onRetryLoad: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = Hk.c
    val ready = state.modelStatus == ModelStatus.Ready
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 18.dp),
    ) {
        Gap(12.dp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("تحليل سريع", style = MaterialTheme.typography.headlineSmall)
                Text("حالة بدون ملف مريض، بالفصحى أو بالعامية", style = MaterialTheme.typography.bodySmall)
            }
            ModelChip(state.modelStatus)
        }
        EcgLine(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(vertical = 6.dp),
            color = if (ready) c.primary else c.textFaint,
        )

        when (state.modelStatus) {
            ModelStatus.Loading -> {
                InfoBanner("عم يجهّز النموذج بالذاكرة… أول مرة بياخد كم ثانية.", c.warning, Icons.Rounded.ErrorOutline)
                Gap(14.dp)
            }
            ModelStatus.Failed -> {
                InfoBanner(
                    "ما قدر يحمّل النموذج. ممكن الملف ناقص، أو مو مدعوم، أو الذاكرة ما بتكفي.",
                    c.danger,
                    Icons.Rounded.ErrorOutline,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("إعادة المحاولة", onClick = onRetryLoad, icon = Icons.Rounded.Refresh)
                        SecondaryButton("الإعدادات", onClick = onOpenSettings, icon = Icons.Rounded.FolderOpen)
                    }
                }
                Gap(14.dp)
            }
            ModelStatus.None -> {
                InfoBanner("لتفعيل التحليل الذكي، اختار ملف النموذج من الإعدادات.", c.info, Icons.Rounded.ErrorOutline) {
                    SecondaryButton("الإعدادات", onClick = onOpenSettings, icon = Icons.Rounded.FolderOpen)
                }
                Gap(14.dp)
            }
            ModelStatus.Ready -> Unit
        }

        AppCard(Modifier.fillMaxWidth(), padding = 14.dp) {
            SectionTitle(
                "القصة المرضية",
                Icons.Rounded.Description,
                c.primary,
                trailing = if (state.caseText.isNotEmpty()) "${state.caseText.length} حرف" else null,
            )
            Gap(10.dp)
            AppField(
                state.caseText,
                onCaseChange,
                "الحالة",
                placeholder = "العمر والجنس، الشكوى ومدتها، الأعراض المرافقة، السوابق، الفحص، التحاليل…",
                minLines = 7,
            )
            Gap(10.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("حالة مثال", onClick = onExample, icon = Icons.Rounded.AutoAwesome, color = c.info)
                if (state.caseText.isNotEmpty()) SecondaryButton("مسح", onClick = onClear, icon = Icons.Rounded.Clear)
            }
        }
        Gap(14.dp)

        VitalsCard(state.vitals, onVitalsChange)
        Gap(18.dp)

        PrimaryButton(
            "حلّل الحالة",
            onClick = onAnalyze,
            icon = Icons.Rounded.AutoAwesome,
            enabled = ready && state.caseText.isNotBlank() && !state.isGenerating,
            modifier = Modifier.fillMaxWidth(),
        )
        Gap(12.dp)
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = c.warning, modifier = Modifier.size(18.dp))
            Text(
                "  كل ما كتبت تفاصيل أكتر (السوابق، الفحص، التحاليل) كل ما كان التحليل أدق.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Gap(150.dp)
    }
}

@Composable
private fun VitalsCard(v: Vitals, onChange: (Vitals) -> Unit) {
    val c = Hk.c
    var open by rememberSaveable { mutableStateOf(!v.isEmpty()) }
    AppCard(Modifier.fillMaxWidth(), padding = 14.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { open = !open },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(
                "العلامات الحيوية",
                Icons.Rounded.Favorite,
                c.pink,
                modifier = Modifier.weight(1f),
                trailing = if (v.isEmpty()) "اختياري" else null,
            )
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = c.textDim)
        }
        AnimatedVisibility(visible = open, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(Modifier.padding(top = 12.dp)) {
                VitalsEditor(v, onChange, showExtra = true)
            }
        }
    }
}

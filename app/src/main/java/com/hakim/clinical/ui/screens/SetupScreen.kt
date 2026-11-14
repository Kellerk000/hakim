package com.hakim.clinical.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hakim.clinical.ui.ImportState
import com.hakim.clinical.ui.UiState
import com.hakim.clinical.ui.components.AppCard
import com.hakim.clinical.ui.components.BrandText
import com.hakim.clinical.ui.components.Gap
import com.hakim.clinical.ui.components.GradientProgress
import com.hakim.clinical.ui.components.InfoBanner
import com.hakim.clinical.ui.components.Pill
import com.hakim.clinical.ui.components.PrimaryButton
import com.hakim.clinical.ui.components.PulseOrb
import com.hakim.clinical.ui.components.SecondaryButton
import com.hakim.clinical.ui.components.gb
import com.hakim.clinical.ui.components.mb
import com.hakim.clinical.ui.theme.Hk

@Composable
fun SetupScreen(state: UiState, onPickModel: () -> Unit, onCancelImport: () -> Unit, onSkip: () -> Unit) {
    val c = Hk.c
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Gap(12.dp)
        PulseOrb(Icons.Rounded.Psychology, size = 140.dp)
        Gap(8.dp)
        BrandText("DrAli", fontSize = 44.sp, textAlign = TextAlign.Center)
        Text(
            "مساعدك الطبي الذكي: ملفات المرضى، والتحليل السريري، بدون إنترنت",
            style = MaterialTheme.typography.bodyLarge.copy(color = c.textDim),
            textAlign = TextAlign.Center,
        )
        Gap(24.dp)

        AppCard(Modifier.fillMaxWidth()) {
            StepRow(1, "نزّل ملف النموذج", "MedGemma 1.5 4B بصيغة GGUF. ننصح بـ Q6_K (حوالي ٣٫٥ غيغا) أو Q4_K_M الأخف.")
            Gap(14.dp)
            StepRow(2, "اختاره من جهازك", "بينتسخ مرة وحدة جوّا التطبيق، وبعدها فيك تمسح النسخة الأصلية.")
            Gap(14.dp)
            StepRow(3, "ابدأ مع مرضاك", "افتح ملف لكل مريض، سجّل زياراته، وخلّي DrAli يساعدك بالتشخيص.")
        }
        Gap(16.dp)

        if (state.deviceRamBytes > 0) {
            Pill("ذاكرة جهازك: ${gb(state.deviceRamBytes)} غيغا", c.primary, icon = Icons.Rounded.Memory)
            Gap(16.dp)
        }

        ImportBlock(state.importState, onPickModel, onCancelImport)
        Gap(6.dp)
        TextButton(onClick = onSkip) {
            Text("تخطّي هلق، بدي ابدأ بملفات المرضى", style = MaterialTheme.typography.labelLarge.copy(color = c.textDim))
        }

        Gap(14.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(16.dp))
            Text(
                "  ما في إنترنت أبداً، وبيانات المرضى مشفّرة على الجهاز.",
                style = MaterialTheme.typography.bodySmall.copy(color = c.textFaint),
            )
        }
        Gap(24.dp)
    }
}

@Composable
fun ImportBlock(st: ImportState, onPickModel: () -> Unit, onCancelImport: () -> Unit) {
    val c = Hk.c
    if (st.active) {
        AppCard(Modifier.fillMaxWidth(), accent = c.primary) {
            Text("عم ينسخ النموذج…", style = MaterialTheme.typography.titleMedium)
            Gap(12.dp)
            GradientProgress(if (st.fraction >= 0f) st.fraction else 0f)
            Gap(10.dp)
            val pct = if (st.fraction >= 0f) "${(st.fraction * 100).toInt()}٪" else ""
            val sizes = if (st.total > 0) "${mb(st.copied)} / ${mb(st.total)} ميغا" else "${mb(st.copied)} ميغا"
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(pct, style = MaterialTheme.typography.titleSmall.copy(color = c.primary))
                Text(sizes, style = MaterialTheme.typography.bodySmall)
            }
            Gap(12.dp)
            SecondaryButton("إلغاء", onClick = onCancelImport, icon = Icons.Rounded.Close, modifier = Modifier.fillMaxWidth())
        }
    } else {
        PrimaryButton("اختيار ملف النموذج", onClick = onPickModel, icon = Icons.Rounded.FolderOpen, modifier = Modifier.fillMaxWidth())
    }
    if (st.error != null) {
        Gap(12.dp)
        InfoBanner(st.error, c.danger, Icons.Rounded.ErrorOutline)
    }
}

@Composable
private fun StepRow(n: Int, title: String, body: String) {
    val c = Hk.c
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(c.brand),
            contentAlignment = Alignment.Center,
        ) {
            Text("$n", style = MaterialTheme.typography.labelLarge.copy(color = c.onPrimary, fontWeight = FontWeight.Bold))
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

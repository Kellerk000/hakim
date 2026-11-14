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
import com.hakim.clinical.ui.components.Gap
import com.hakim.clinical.ui.components.GhostButton
import com.hakim.clinical.ui.components.GlassCard
import com.hakim.clinical.ui.components.GradientButton
import com.hakim.clinical.ui.components.GradientProgress
import com.hakim.clinical.ui.components.GradientText
import com.hakim.clinical.ui.components.Pill
import com.hakim.clinical.ui.components.PulseOrb
import com.hakim.clinical.ui.theme.Hk
import java.util.Locale

@Composable
fun SetupScreen(state: UiState, onPickModel: () -> Unit, onCancelImport: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Gap(18.dp)
        PulseOrb(Icons.Rounded.Psychology, size = 150.dp)
        Gap(10.dp)
        GradientText("حكيم", fontSize = 48.sp, textAlign = TextAlign.Center)
        Text(
            "مساعدك التشخيصي الذكي، بيشتغل على جهازك بدون إنترنت",
            style = MaterialTheme.typography.bodyLarge,
            color = Hk.TextDim,
            textAlign = TextAlign.Center,
        )
        Gap(26.dp)

        GlassCard(Modifier.fillMaxWidth()) {
            StepRow(1, "نزّل ملف النموذج", "مثلاً MedGemma 1.5 4B بصيغة GGUF ‏(Q4_K_M)، حوالي ٢٫٥ غيغا. أو النموذج اللي دربته على حالاتك.")
            Gap(14.dp)
            StepRow(2, "اختاره من جهازك", "رح ينتسخ مرة وحدة جوّا التطبيق، وبعدها ما عاد تحتاجه.")
            Gap(14.dp)
            StepRow(3, "اكتب الحالة وحلّلها", "بالفصحى أو بالعامية، وبيطلعلك التشخيص الأرجح وكيف تأكده، أو الاحتمالات وشو تعمل لتحسم.")
        }
        Gap(16.dp)

        if (state.deviceRamBytes > 0) {
            Pill(
                text = "ذاكرة جهازك: ${gb(state.deviceRamBytes)} غيغا",
                brush = Hk.BrandSoft,
                icon = Icons.Rounded.Memory,
                textColor = Hk.Text,
            )
            Gap(16.dp)
        }

        ImportBlock(state.importState, onPickModel, onCancelImport)

        Gap(22.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Hk.TextFaint, modifier = Modifier.size(16.dp))
            Text(
                "  ما في إنترنت أبداً، وبيانات المرضى ما بتطلع من الجهاز.",
                style = MaterialTheme.typography.bodySmall,
                color = Hk.TextFaint,
            )
        }
        Gap(24.dp)
    }
}

@Composable
fun ImportBlock(st: ImportState, onPickModel: () -> Unit, onCancelImport: () -> Unit) {
    if (st.active) {
        GlassCard(Modifier.fillMaxWidth(), accent = Hk.Cyan) {
            Text("عم ينسخ النموذج…", style = MaterialTheme.typography.titleMedium)
            Gap(12.dp)
            GradientProgress(if (st.fraction >= 0f) st.fraction else 0f)
            Gap(10.dp)
            val pct = if (st.fraction >= 0f) "${(st.fraction * 100).toInt()}٪" else ""
            val sizes = if (st.total > 0) "${mb(st.copied)} / ${mb(st.total)} ميغا" else "${mb(st.copied)} ميغا"
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(pct, color = Hk.Cyan, fontWeight = FontWeight.Bold)
                Text(sizes, style = MaterialTheme.typography.bodySmall)
            }
            Gap(12.dp)
            GhostButton("إلغاء", onClick = onCancelImport, icon = Icons.Rounded.Close, modifier = Modifier.fillMaxWidth())
        }
    } else {
        GradientButton(
            text = "اختيار ملف النموذج",
            onClick = onPickModel,
            icon = Icons.Rounded.FolderOpen,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (st.error != null) {
        Gap(14.dp)
        GlassCard(Modifier.fillMaxWidth(), accent = Hk.Danger) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Hk.Danger)
                Text("  ${st.error}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun StepRow(n: Int, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Hk.Brand),
            contentAlignment = Alignment.Center,
        ) {
            Text("$n", color = Hk.Bg0, fontWeight = FontWeight.Black)
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun gb(bytes: Long): String = String.format(Locale.US, "%.1f", bytes / 1_073_741_824.0)

fun mb(bytes: Long): String = (bytes / 1_048_576L).toString()

package com.hakim.clinical.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hakim.clinical.ui.Stage
import com.hakim.clinical.ui.UiState
import com.hakim.clinical.ui.components.ActionIcon
import com.hakim.clinical.ui.components.AnswerCards
import com.hakim.clinical.ui.components.AppCard
import com.hakim.clinical.ui.components.Gap
import com.hakim.clinical.ui.components.InfoBanner
import com.hakim.clinical.ui.components.PrimaryButton
import com.hakim.clinical.ui.components.ReadingCard
import com.hakim.clinical.ui.components.References
import com.hakim.clinical.ui.components.SecondaryButton
import com.hakim.clinical.ui.components.SectionTitle
import com.hakim.clinical.ui.components.TopBar
import com.hakim.clinical.ui.components.TypingDots
import com.hakim.clinical.ui.components.WritingHint
import com.hakim.clinical.ui.theme.Hk
import java.util.Locale

@Composable
fun ResultScreen(
    state: UiState,
    onBack: () -> Unit,
    onStop: () -> Unit,
    onNewCase: () -> Unit,
    onEditAgain: () -> Unit,
) {
    val c = Hk.c
    val clipboard = LocalClipboardManager.current
    val parsed = state.parsed
    val generating = state.isGenerating

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp),
    ) {
        TopBar("نتيجة التحليل", onBack = onBack) {
            if (generating) {
                ActionIcon(Icons.Rounded.Stop, "إيقاف", onStop, tint = c.danger)
            } else if (state.output.isNotBlank()) {
                ActionIcon(Icons.Rounded.ContentCopy, "نسخ", { clipboard.setText(AnnotatedString(state.output.trim())) })
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CaseSummary(state.resultCase, state.resultVitals.takeIf { !it.isEmpty() }?.toArabic())
            if (state.stage == Stage.Reading) ReadingCard(state.promptProgress)
            val hasSections = parsed != null && parsed.sectionsFound > 0
            if (state.stage == Stage.Writing && !hasSections) WritingHint()
            if (parsed != null) AnswerCards(parsed, generating, state.output)
            if (generating && hasSections) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
                    TypingDots()
                    Text("  عم يكمّل الكتابة…", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (state.error != null) InfoBanner(state.error, c.danger, Icons.Rounded.ErrorOutline)
            if (state.stage == Stage.Stopped) {
                Text("تم إيقاف التحليل قبل ما يكمل.", style = MaterialTheme.typography.bodySmall.copy(color = c.warning))
            }
            if (state.tokensPerSec > 0f || state.elapsedMs > 0L) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null, tint = c.warning, modifier = Modifier.size(16.dp))
                    val speed = if (state.tokensPerSec > 0f) String.format(Locale.US, "%.1f", state.tokensPerSec) + " كلمة/ثانية" else ""
                    val secs = if (state.elapsedMs > 0) " · ${state.elapsedMs / 1000} ثانية" else ""
                    Text(" $speed$secs", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!generating) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("عدّل وأعد", onClick = onEditAgain, icon = Icons.Rounded.Edit, modifier = Modifier.weight(1f))
                    PrimaryButton("حالة جديدة", onClick = onNewCase, icon = Icons.Rounded.Add, modifier = Modifier.weight(1f))
                }
            }
            if (state.references.isNotEmpty()) {
                Column { References(state.references) }
            }
            if (state.output.isNotBlank()) RawOutput(state.output)
            Text(
                "DrAli أداة مساعدة للطبيب وما بتغني عن التقييم السريري. القرار النهائي إلك.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Gap(40.dp)
        }
    }
}

@Composable
private fun CaseSummary(text: String, vitals: String?) {
    val c = Hk.c
    var expanded by remember { mutableStateOf(false) }
    AppCard(Modifier.fillMaxWidth().animateContentSize(), padding = 14.dp, onClick = { expanded = !expanded }) {
        SectionTitle("الحالة", Icons.Rounded.Description, c.textDim, trailing = if (expanded) "إخفاء" else "عرض الكل")
        Gap(8.dp)
        Text(
            text + (vitals?.let { "\n$it" } ?: ""),
            style = MaterialTheme.typography.bodyMedium.copy(color = c.textDim),
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RawOutput(text: String) {
    val c = Hk.c
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { open = !open }
            .padding(6.dp)
    ) {
        Text(
            if (open) "إخفاء النص الكامل ▲" else "عرض النص الكامل ▼",
            style = MaterialTheme.typography.labelLarge.copy(color = c.textDim),
        )
        AnimatedVisibility(visible = open) {
            Text(text.trim(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

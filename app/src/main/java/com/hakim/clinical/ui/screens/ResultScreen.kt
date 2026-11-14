package com.hakim.clinical.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hakim.clinical.data.Discriminator
import com.hakim.clinical.data.Mode
import com.hakim.clinical.data.ParsedAnswer
import com.hakim.clinical.data.RefHit
import com.hakim.clinical.data.Urgency
import com.hakim.clinical.ui.Stage
import com.hakim.clinical.ui.UiState
import com.hakim.clinical.ui.components.Appear
import com.hakim.clinical.ui.components.BlinkDot
import com.hakim.clinical.ui.components.EcgLine
import com.hakim.clinical.ui.components.Gap
import com.hakim.clinical.ui.components.GhostButton
import com.hakim.clinical.ui.components.GlassCard
import com.hakim.clinical.ui.components.GradientButton
import com.hakim.clinical.ui.components.GradientProgress
import com.hakim.clinical.ui.components.GradientText
import com.hakim.clinical.ui.components.OutlineChip
import com.hakim.clinical.ui.components.Pill
import com.hakim.clinical.ui.components.RankBar
import com.hakim.clinical.ui.components.SectionTitle
import com.hakim.clinical.ui.components.TypingDots
import com.hakim.clinical.ui.theme.Hk

@Composable
fun ResultScreen(
    state: UiState,
    onBack: () -> Unit,
    onStop: () -> Unit,
    onNewCase: () -> Unit,
    onEditAgain: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val parsed = state.parsed
    val generating = state.isGenerating

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp),
    ) {
        // الشريط العلوي
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع", tint = Hk.Text)
            }
            Text("نتيجة التحليل", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (generating) {
                IconButton(onClick = onStop) {
                    Icon(Icons.Rounded.Stop, contentDescription = "إيقاف", tint = Hk.Danger)
                }
            } else if (state.output.isNotBlank()) {
                IconButton(onClick = { clipboard.setText(AnnotatedString(state.output.trim())) }) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "نسخ", tint = Hk.TextDim)
                }
            }
        }

        CaseSummary(state.resultCase, state.resultVitals.takeIf { !it.isEmpty() }?.toArabic())
        Gap(14.dp)

        if (state.stage == Stage.Reading) {
            ReadingCard(state.promptProgress)
            Gap(14.dp)
        }

        val hasSections = parsed != null && parsed.sectionsFound > 0
        if (state.stage == Stage.Writing && !hasSections) {
            WritingHint()
            Gap(14.dp)
        }

        if (parsed != null && hasSections) {
            AnswerCards(parsed, generating)
        } else if (!generating && state.output.isNotBlank()) {
            // النموذج ما التزم بالتنسيق: نعرض النص كما هو
            GlassCard(Modifier.fillMaxWidth()) {
                SectionTitle("التحليل", Icons.Rounded.Description, Hk.Sky)
                Gap(10.dp)
                Text(state.output.trim(), style = MaterialTheme.typography.bodyLarge)
            }
            Gap(14.dp)
        }

        if (generating && hasSections) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(6.dp)) {
                TypingDots()
                Text("  عم يكمّل الكتابة…", style = MaterialTheme.typography.bodySmall)
            }
            Gap(8.dp)
        }

        state.error?.let { err ->
            GlassCard(Modifier.fillMaxWidth(), accent = Hk.Danger) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Hk.Danger)
                    Text("  $err", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Gap(14.dp)
        }

        if (state.stage == Stage.Stopped) {
            Text("تم إيقاف التحليل قبل ما يكمل.", style = MaterialTheme.typography.bodySmall, color = Hk.Warning)
            Gap(10.dp)
        }

        if (state.tokensPerSec > 0f || state.elapsedMs > 0L) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Hk.Warning, modifier = Modifier.size(16.dp))
                val speed = if (state.tokensPerSec > 0f) String.format(java.util.Locale.US, "%.1f", state.tokensPerSec) + " كلمة/ثانية" else ""
                val secs = if (state.elapsedMs > 0) " · ${state.elapsedMs / 1000} ثانية" else ""
                Text(" $speed$secs", style = MaterialTheme.typography.bodySmall)
            }
            Gap(14.dp)
        }

        if (!generating) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GhostButton("عدّل وأعد التحليل", onClick = onEditAgain, icon = Icons.Rounded.Edit, modifier = Modifier.weight(1f))
                GradientButton("حالة جديدة", onClick = onNewCase, icon = Icons.Rounded.Add, modifier = Modifier.weight(1f))
            }
            Gap(20.dp)
        }

        if (state.references.isNotEmpty()) {
            References(state.references)
            Gap(16.dp)
        }

        if (state.output.isNotBlank()) {
            RawOutput(state.output)
            Gap(14.dp)
        }

        Text(
            "حكيم أداة مساعدة للطبيب وما بتغني عن التقييم السريري. القرار النهائي إلك.",
            style = MaterialTheme.typography.bodySmall,
            color = Hk.TextFaint,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Gap(40.dp)
    }
}

@Composable
private fun CaseSummary(text: String, vitals: String?) {
    var expanded by remember { mutableStateOf(false) }
    GlassCard(Modifier.fillMaxWidth().animateContentSize(), padding = 14.dp, onClick = { expanded = !expanded }) {
        SectionTitle("الحالة", Icons.Rounded.Description, Hk.TextDim, trailing = if (expanded) "إخفاء" else "عرض الكل")
        Gap(8.dp)
        Text(
            text + (vitals?.let { "\n$it" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = Hk.TextDim,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ReadingCard(progress: Float) {
    GlassCard(Modifier.fillMaxWidth(), accent = Hk.Cyan) {
        EcgLine(Modifier.fillMaxWidth().height(70.dp), periodMs = 1100, strokeWidth = 3.dp)
        Gap(10.dp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("عم يقرأ الحالة ويفكّر…", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${(progress * 100).toInt()}٪", color = Hk.Cyan, fontWeight = FontWeight.Bold)
        }
        Gap(10.dp)
        GradientProgress(progress)
        Gap(8.dp)
        Text("كل شي عم يصير على جهازك. الحالات الطويلة بتاخد وقت أطول.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WritingHint() {
    GlassCard(Modifier.fillMaxWidth(), accent = Hk.Violet) {
        EcgLine(Modifier.fillMaxWidth().height(50.dp), color = Hk.Violet, periodMs = 1300)
        Gap(6.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TypingDots(color = Hk.Violet)
            Text("  عم يكتب التحليل…", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun AnswerCards(p: ParsedAnswer, generating: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Appear { HeroCard(p) }

        if (p.urgencyText != null) Appear { UrgencyBanner(p.urgency, p.urgencyText) }

        val nextCard: @Composable () -> Unit = {
            if (p.nextText != null || p.nextBullets.isNotEmpty() || p.discriminators.isNotEmpty()) {
                Appear { NextCard(p) }
            }
        }
        val confirmCard: @Composable () -> Unit = {
            if (p.confirm.isNotEmpty()) Appear { ConfirmCard(p.confirm, p.mode) }
        }

        if (p.mode == Mode.MULTIPLE) {
            nextCard()
            confirmCard()
        } else {
            confirmCard()
        }

        if (p.differentials.isNotEmpty()) Appear { DifferentialCard(p.differentials) }

        if (p.mode != Mode.MULTIPLE) nextCard()

        if (p.redFlags != null) Appear { RedFlagsCard(p.redFlags) }

        if (!generating && p.footer != null) {
            Text(p.footer, style = MaterialTheme.typography.bodySmall, color = Hk.TextFaint, modifier = Modifier.padding(horizontal = 6.dp))
        }
    }
    Gap(14.dp)
}

@Composable
private fun HeroCard(p: ParsedAnswer) {
    val isMulti = p.mode == Mode.MULTIPLE
    GlassCard(Modifier.fillMaxWidth(), accent = if (isMulti) Hk.Warning else Hk.Success, padding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(
                text = if (isMulti) "عدة احتمالات" else "تشخيص مرجّح",
                brush = if (isMulti) Hk.MultiGrad else Hk.StrongGrad,
                icon = if (isMulti) Icons.Rounded.AltRoute else Icons.Rounded.CheckCircle,
            )
        }
        Gap(14.dp)
        if (!isMulti) {
            Text("التشخيص الأرجح", style = MaterialTheme.typography.bodySmall)
            GradientText(p.top ?: "…", fontSize = 26.sp, brush = Hk.StrongGrad, weight = FontWeight.Bold)
        } else {
            Text("الاحتمالات الأقرب (ترتيب تقريبي)", style = MaterialTheme.typography.bodySmall)
            Gap(10.dp)
            val colors = listOf(Hk.Warning, Hk.Pink, Hk.Violet, Hk.Sky, Hk.Cyan)
            p.candidates.take(6).forEachIndexed { i, c ->
                CandidateRow(i + 1, c, colors[i % colors.size], 1f - i * 0.16f)
                if (i < p.candidates.size - 1) Gap(10.dp)
            }
        }
        if (p.clues != null) {
            Gap(14.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = Hk.Sky, modifier = Modifier.size(16.dp))
                        Text("  القرائن", style = MaterialTheme.typography.labelLarge, color = Hk.Sky)
                    }
                    Gap(4.dp)
                    Text(p.clues, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(rank: Int, name: String, color: Color, fraction: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f))
                .border(1.dp, color.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$rank", color = color, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Gap(5.dp)
            RankBar(fraction.coerceIn(0.15f, 1f), color, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun UrgencyBanner(level: Urgency, text: String) {
    val (color, title) = when (level) {
        Urgency.EMERGENCY -> Hk.Danger to "حالة إسعافية"
        Urgency.URGENT -> Hk.Warning to "حالة عاجلة"
        Urgency.ROUTINE -> Hk.Success to "عيادة ومتابعة"
        Urgency.UNKNOWN -> Hk.Sky to "الاستعجال"
    }
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.08f))))
            .border(1.dp, color.copy(alpha = 0.6f), shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (level == Urgency.EMERGENCY) BlinkDot(color) else Icon(Icons.Rounded.LocalHospital, contentDescription = null, tint = color)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, color = color, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun NextCard(p: ParsedAnswer) {
    val multi = p.mode == Mode.MULTIPLE
    GlassCard(Modifier.fillMaxWidth(), accent = Hk.Violet) {
        SectionTitle(if (multi) "شو لازم تعمل لتحسم" else "الخطوة التالية", Icons.Rounded.Lightbulb, Hk.Violet)
        p.nextText?.let {
            Gap(10.dp)
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        if (p.discriminators.isNotEmpty()) {
            Gap(12.dp)
            p.discriminators.forEach { d ->
                DiscriminatorRow(d)
                Gap(8.dp)
            }
        }
        if (p.nextBullets.isNotEmpty()) {
            Gap(8.dp)
            p.nextBullets.forEach { b ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .padding(top = 9.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Hk.Violet)
                    )
                    Text(b, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun DiscriminatorRow(d: Discriminator) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Hk.Violet.copy(alpha = 0.25f), shape)
            .padding(12.dp)
    ) {
        Text("إذا لقيت: ${d.sign}", style = MaterialTheme.typography.bodyMedium)
        Gap(6.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("بيرجّح ", style = MaterialTheme.typography.bodySmall)
            Pill(d.favors, Hk.BrandSoft, textColor = Hk.Text)
        }
    }
}

@Composable
private fun ConfirmCard(items: List<String>, mode: Mode) {
    GlassCard(Modifier.fillMaxWidth(), accent = Hk.Cyan) {
        SectionTitle(
            if (mode == Mode.MULTIPLE) "الفحوص اللي بتحسم" else "كيف تأكد التشخيص",
            Icons.Rounded.FactCheck,
            Hk.Cyan,
        )
        Gap(10.dp)
        items.forEach { t ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Hk.Cyan, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                Text(t, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DifferentialCard(items: List<String>) {
    GlassCard(Modifier.fillMaxWidth(), accent = Hk.Sky) {
        SectionTitle("التشخيصات التفريقية", Icons.Rounded.AltRoute, Hk.Sky)
        Gap(12.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { OutlineChip(it, Hk.Sky) }
        }
    }
}

@Composable
private fun RedFlagsCard(text: String) {
    GlassCard(Modifier.fillMaxWidth(), accent = Hk.Danger) {
        SectionTitle("علامات الإنذار", Icons.Rounded.WarningAmber, Hk.Danger)
        Gap(10.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun References(refs: List<RefHit>) {
    SectionTitle(
        "حالات مشابهة من المكتبة",
        Icons.Rounded.CollectionsBookmark,
        Hk.Pink,
        trailing = "للمقارنة فقط",
    )
    Gap(10.dp)
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        refs.forEach { h ->
            GlassCard(Modifier.width(260.dp), padding = 14.dp) {
                Text(h.entry.specialty, style = MaterialTheme.typography.labelMedium, color = Hk.Pink)
                Gap(4.dp)
                Text(h.entry.dx, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Gap(6.dp)
                Text(
                    "${h.entry.complaint}، ${h.entry.detail}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Gap(8.dp)
                Text("فحوص: ${h.entry.tests}", style = MaterialTheme.typography.bodySmall, color = Hk.Cyan, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Gap(8.dp)
                RankBar(h.relative, Hk.Pink, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun RawOutput(text: String) {
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { open = !open }
            .padding(6.dp)
    ) {
        Text(if (open) "إخفاء النص الكامل ▲" else "عرض النص الكامل ▼", style = MaterialTheme.typography.labelLarge, color = Hk.TextDim)
        AnimatedVisibility(visible = open) {
            Text(
                text.trim(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

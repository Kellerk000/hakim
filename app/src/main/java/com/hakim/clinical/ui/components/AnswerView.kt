package com.hakim.clinical.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hakim.clinical.data.Discriminator
import com.hakim.clinical.data.Mode
import com.hakim.clinical.data.ParsedAnswer
import com.hakim.clinical.data.RefHit
import com.hakim.clinical.data.Urgency
import com.hakim.clinical.ui.theme.Hk

/** بطاقات نتيجة التحليل (بتنبني وهو عم يكتب). */
@Composable
fun AnswerCards(p: ParsedAnswer, generating: Boolean, rawText: String) {
    val c = Hk.c
    val usable = p.top != null || p.candidates.isNotEmpty() || p.confirm.isNotEmpty() || p.redFlags != null
    if (p.sectionsFound == 0 || (!usable && !generating)) {
        if (!generating && rawText.isNotBlank()) {
            AppCard(Modifier.fillMaxWidth()) {
                SectionTitle("التحليل", Icons.Rounded.Description, c.info)
                Gap(10.dp)
                Text(rawText.trim().replace("**", ""), style = MaterialTheme.typography.bodyLarge)
                if (p.sectionsFound > 0) {
                    Gap(10.dp)
                    Text(
                        "النموذج ما عبّى الأقسام متل المطلوب. جرّب تعيد التحليل، أو استعمل النموذج المدرّب.",
                        style = MaterialTheme.typography.bodySmall.copy(color = c.warning),
                    )
                }
            }
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Appear { HeroCard(p) }
        if (p.urgencyText != null) Appear { UrgencyBanner(p.urgency, p.urgencyText) }

        val next: @Composable () -> Unit = {
            if (p.nextText != null || p.nextBullets.isNotEmpty() || p.discriminators.isNotEmpty()) Appear { NextCard(p) }
        }
        val confirm: @Composable () -> Unit = {
            if (p.confirm.isNotEmpty()) Appear { ConfirmCard(p.confirm, p.mode) }
        }
        if (p.mode == Mode.MULTIPLE) {
            next()
            confirm()
        } else {
            confirm()
        }
        if (p.differentials.isNotEmpty()) Appear { DifferentialCard(p.differentials) }
        if (p.mode != Mode.MULTIPLE) next()
        if (p.redFlags != null) Appear { RedFlagsCard(p.redFlags) }
        if (!generating && p.footer != null) {
            Text(p.footer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 6.dp))
        }
    }
}

@Composable
private fun HeroCard(p: ParsedAnswer) {
    val c = Hk.c
    val multi = p.mode == Mode.MULTIPLE
    val accent = if (multi) c.warning else c.success
    AppCard(Modifier.fillMaxWidth(), accent = accent, padding = 20.dp) {
        Pill(
            text = if (multi) "عدة احتمالات" else "تشخيص مرجّح",
            color = accent,
            icon = if (multi) Icons.AutoMirrored.Rounded.AltRoute else Icons.Rounded.CheckCircle,
            filled = true,
        )
        Gap(14.dp)
        if (!multi) {
            Text("التشخيص الأرجح", style = MaterialTheme.typography.labelMedium)
            Gap(2.dp)
            Text(p.top ?: "…", style = MaterialTheme.typography.headlineSmall.copy(color = c.text, fontSize = 23.sp, lineHeight = 32.sp))
        } else {
            Text("الاحتمالات الأقرب (ترتيب تقريبي)", style = MaterialTheme.typography.labelMedium)
            Gap(4.dp)
            val colors = listOf(c.warning, c.pink, c.secondary, c.info, c.primary)
            p.candidates.take(6).forEachIndexed { i, name ->
                Gap(10.dp)
                CandidateRow(i + 1, name, colors[i % colors.size], 1f - i * 0.16f)
            }
        }
        if (p.clues != null) {
            Gap(14.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surfaceAlt)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = c.info, modifier = Modifier.size(16.dp))
                    Text("  القرائن", style = MaterialTheme.typography.labelLarge.copy(color = c.info))
                }
                Gap(4.dp)
                Text(p.clues, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CandidateRow(rank: Int, name: String, color: androidx.compose.ui.graphics.Color, fraction: Float) {
    val c = Hk.c
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(c.tint(color))
                .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$rank", style = MaterialTheme.typography.labelLarge.copy(color = color, fontWeight = FontWeight.Bold))
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Gap(5.dp)
            RankBar(fraction.coerceIn(0.15f, 1f), color, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun UrgencyBanner(level: Urgency, text: String) {
    val c = Hk.c
    val (color, title) = when (level) {
        Urgency.EMERGENCY -> c.danger to "حالة إسعافية"
        Urgency.URGENT -> c.warning to "حالة عاجلة"
        Urgency.ROUTINE -> c.success to "عيادة ومتابعة"
        Urgency.UNKNOWN -> c.info to "الاستعجال"
    }
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(c.tint(color), c.surface)))
            .border(1.dp, color.copy(alpha = 0.45f), shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (level == Urgency.EMERGENCY) BlinkDot(color) else Icon(Icons.Rounded.LocalHospital, contentDescription = null, tint = color)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(color = color, fontWeight = FontWeight.Bold))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun NextCard(p: ParsedAnswer) {
    val c = Hk.c
    val multi = p.mode == Mode.MULTIPLE
    AppCard(Modifier.fillMaxWidth(), accent = c.secondary) {
        SectionTitle(if (multi) "شو لازم تعمل لتحسم" else "الخطوة التالية", Icons.Rounded.Lightbulb, c.secondary)
        p.nextText?.let {
            Gap(10.dp)
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        p.discriminators.forEach { d ->
            Gap(8.dp)
            DiscriminatorRow(d)
        }
        if (p.nextBullets.isNotEmpty()) {
            Gap(6.dp)
            p.nextBullets.forEach { b ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .padding(top = 9.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(c.secondary)
                    )
                    Text(b, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun DiscriminatorRow(d: Discriminator) {
    val c = Hk.c
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surfaceAlt)
            .border(1.dp, c.border, shape)
            .padding(12.dp)
    ) {
        Text("إذا لقيت: ${d.sign}", style = MaterialTheme.typography.bodyMedium)
        Gap(6.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("بيرجّح  ", style = MaterialTheme.typography.bodySmall)
            Pill(d.favors, c.secondary)
        }
    }
}

@Composable
private fun ConfirmCard(items: List<String>, mode: Mode) {
    val c = Hk.c
    AppCard(Modifier.fillMaxWidth(), accent = c.primary) {
        SectionTitle(
            if (mode == Mode.MULTIPLE) "الفحوص اللي بتحسم" else "كيف تأكد التشخيص",
            Icons.AutoMirrored.Rounded.FactCheck,
            c.primary,
        )
        Gap(8.dp)
        items.forEach { t ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.primary, modifier = Modifier.padding(top = 3.dp).size(17.dp))
                Text(t, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DifferentialCard(items: List<String>) {
    val c = Hk.c
    AppCard(Modifier.fillMaxWidth(), accent = c.info) {
        SectionTitle("التشخيصات التفريقية", Icons.AutoMirrored.Rounded.AltRoute, c.info)
        Gap(12.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { OutlineChip(it, c.info) }
        }
    }
}

@Composable
private fun RedFlagsCard(text: String) {
    val c = Hk.c
    AppCard(Modifier.fillMaxWidth(), accent = c.danger) {
        SectionTitle("علامات الإنذار", Icons.Rounded.WarningAmber, c.danger)
        Gap(8.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ReadingCard(progress: Float) {
    val c = Hk.c
    AppCard(Modifier.fillMaxWidth(), accent = c.primary) {
        EcgLine(Modifier.fillMaxWidth().height(64.dp), periodMs = 1300, strokeWidth = 2.6.dp)
        Gap(10.dp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("عم يقرأ الحالة ويفكّر…", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${(progress * 100).toInt()}٪", style = MaterialTheme.typography.titleMedium.copy(color = c.primary))
        }
        Gap(10.dp)
        GradientProgress(progress)
        Gap(8.dp)
        Text("كل شي عم يصير على جهازك. الحالات الطويلة بتاخد وقت أطول.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun WritingHint() {
    val c = Hk.c
    AppCard(Modifier.fillMaxWidth(), accent = c.secondary) {
        EcgLine(Modifier.fillMaxWidth().height(46.dp), color = c.secondary, periodMs = 1500)
        Gap(6.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TypingDots(color = c.secondary)
            Text("  عم يكتب التحليل…", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun References(refs: List<RefHit>) {
    val c = Hk.c
    SectionTitle("حالات مشابهة من المكتبة", Icons.Rounded.CollectionsBookmark, c.pink, trailing = "للمقارنة فقط")
    Gap(10.dp)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        refs.forEach { h ->
            AppCard(Modifier.width(260.dp), padding = 14.dp) {
                Text(h.entry.specialty, style = MaterialTheme.typography.labelMedium.copy(color = c.pink))
                Gap(4.dp)
                Text(h.entry.dx, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Gap(6.dp)
                Text("${h.entry.complaint}، ${h.entry.detail}", style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Gap(8.dp)
                Text("فحوص: ${h.entry.tests}", style = MaterialTheme.typography.bodySmall.copy(color = c.primary), maxLines = 3, overflow = TextOverflow.Ellipsis)
                Gap(8.dp)
                RankBar(h.relative, c.pink, Modifier.fillMaxWidth())
            }
        }
    }
}

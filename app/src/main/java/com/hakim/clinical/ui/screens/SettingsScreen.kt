package com.hakim.clinical.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hakim.clinical.data.AppSettings
import com.hakim.clinical.ui.ModelStatus
import com.hakim.clinical.ui.UiState
import com.hakim.clinical.ui.components.Gap
import com.hakim.clinical.ui.components.GlassCard
import com.hakim.clinical.ui.components.SectionTitle
import com.hakim.clinical.ui.theme.Hk
import com.hakim.clinical.ui.theme.MonoSmall
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: UiState,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onPickModel: () -> Unit,
    onCancelImport: () -> Unit,
) {
    val s = state.settings
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
    ) {
        Gap(14.dp)
        Text("الإعدادات", style = MaterialTheme.typography.headlineSmall)
        Gap(16.dp)

        // النموذج
        GlassCard(Modifier.fillMaxWidth(), accent = Hk.Cyan) {
            SectionTitle("النموذج", Icons.Rounded.Memory, Hk.Cyan)
            Gap(10.dp)
            Text(s.modelName ?: "ما في نموذج", style = MaterialTheme.typography.titleMedium)
            val status = when (state.modelStatus) {
                ModelStatus.Ready -> "جاهز"
                ModelStatus.Loading -> "عم يتحمّل…"
                ModelStatus.Failed -> "فشل التحميل"
                ModelStatus.None -> "غير محمّل"
            }
            val size = if (s.modelSize > 0) " · ${gb(s.modelSize)} غيغا" else ""
            Text("$status$size", style = MaterialTheme.typography.bodySmall)
            if (state.modelDesc.isNotBlank()) Text(state.modelDesc, style = MonoSmall)
            if (state.deviceRamBytes > 0 && s.modelSize > 0 && s.modelSize > state.deviceRamBytes * 0.55) {
                Gap(8.dp)
                Text(
                    "تنبيه: النموذج كبير بالنسبة لذاكرة جهازك (${gb(state.deviceRamBytes)} غيغا). إذا صار بطيء أو سكّر، جرّب نموذج أصغر.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Hk.Warning,
                )
            }
            Gap(14.dp)
            ImportBlock(state.importState, onPickModel, onCancelImport)
        }
        Gap(14.dp)

        // الأداء
        GlassCard(Modifier.fillMaxWidth(), accent = Hk.Violet) {
            SectionTitle("الأداء والجودة", Icons.Rounded.Speed, Hk.Violet)
            Gap(8.dp)

            SliderRow(
                title = "عدد الأنوية",
                value = "${s.threads}",
                hint = "4 لـ 6 أنسب للموبايلات الحديثة.",
                current = s.threads.toFloat(),
                range = 1f..8f,
                steps = 6,
            ) { v -> onUpdate { it.copy(threads = v.roundToInt()) } }

            Text("حجم السياق", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            Text("أكبر = بيستوعب حالات أطول، بس بياخد ذاكرة أكتر.", style = MaterialTheme.typography.bodySmall)
            Gap(8.dp)
            Segmented(
                options = listOf(2048, 4096, 8192),
                selected = s.nCtx,
                label = { "$it" },
            ) { v -> onUpdate { it.copy(nCtx = v) } }

            SliderRow(
                title = "أقصى طول للجواب",
                value = "${s.maxTokens}",
                hint = "كلمات أكتر = جواب أطول ووقت أطول.",
                current = s.maxTokens.toFloat(),
                range = 300f..1600f,
                steps = 12,
            ) { v -> onUpdate { it.copy(maxTokens = (v / 100f).roundToInt() * 100) } }

            SliderRow(
                title = "درجة الإبداع (Temperature)",
                value = String.format(Locale.US, "%.1f", s.temperature),
                hint = "منخفضة = أجوبة ثابتة ومنضبطة (أنسب للطب).",
                current = s.temperature,
                range = 0f..1f,
                steps = 9,
            ) { v -> onUpdate { it.copy(temperature = (v * 10f).roundToInt() / 10f) } }
        }
        Gap(14.dp)

        // المكتبة
        GlassCard(Modifier.fillMaxWidth(), accent = Hk.Pink) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionTitle("المكتبة المرجعية بالتحليل", Icons.Rounded.CollectionsBookmark, Hk.Pink)
                }
                Switch(
                    checked = s.useReferences,
                    onCheckedChange = { c -> onUpdate { it.copy(useReferences = c) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Hk.Bg0,
                        checkedTrackColor = Hk.Pink,
                        uncheckedThumbColor = Hk.TextDim,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                        uncheckedBorderColor = Hk.Stroke,
                    ),
                )
            }
            Gap(8.dp)
            Text(
                "المكتبة فيها 184 تشخيص من ملف الحالات. الحالات المشابهة بتنعرض دائماً تحت النتيجة للمقارنة. " +
                    "إذا فعّلت هالخيار بتنبعت كمان للنموذج كتلميح. مفيد مع النموذج الأساسي، بس ممكن يشدّه لتشخيص غلط إذا الحالة مو بالمكتبة.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Gap(14.dp)

        // حول
        GlassCard(Modifier.fillMaxWidth()) {
            SectionTitle("حول حكيم", Icons.Rounded.Info, Hk.Sky)
            Gap(8.dp)
            Text(
                "حكيم أداة مساعدة للطبيب، مو بديل عن التقييم السريري. النموذج ممكن يغلط وهو واثق، فراجع كل اقتراح بحكمك وبالإرشادات المحلية. " +
                    "ما في إذن إنترنت بالتطبيق، وكل البيانات محفوظة على الجهاز بس.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.systemInfo.isNotBlank()) {
                Gap(10.dp)
                Text(state.systemInfo, style = MonoSmall)
            }
        }
        Gap(130.dp)
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: String,
    hint: String,
    current: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(value, color = Hk.Violet, fontWeight = FontWeight.Bold)
        }
        Text(hint, style = MaterialTheme.typography.bodySmall)
        Slider(
            value = current.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Hk.Violet,
                activeTrackColor = Hk.Violet,
                inactiveTrackColor = Color.White.copy(alpha = 0.10f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Hk.Stroke, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { o ->
            val isSel = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSel) Hk.Brand else SolidColor(Color.Transparent))
                    .clickable { onSelect(o) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(o),
                    color = if (isSel) Hk.Bg0 else Hk.TextDim,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

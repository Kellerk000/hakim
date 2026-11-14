package com.hakim.clinical.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hakim.clinical.ui.theme.Hk
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/** سلسلة قيم بالترتيب الزمني (null = ما انقاست بهالزيارة). */
data class ChartSeries(val label: String, val color: Color, val values: List<Float?>)

fun formatVital(v: Float?): String = when {
    v == null -> "—"
    v % 1f == 0f -> v.toInt().toString()
    else -> String.format(Locale.US, "%.1f", v)
}

@Composable
fun VitalChart(
    title: String,
    unit: String,
    icon: ImageVector,
    dates: List<Long>,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
) {
    val c = Hk.c
    val all = series.flatMap { s -> s.values.filterNotNull() }
    val main = series.first()
    AppCard(modifier.fillMaxWidth(), padding = 16.dp) {
        SectionTitle(title, icon, main.color, trailing = unit)
        Gap(10.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Bottom) {
            series.forEach { s ->
                Column {
                    Text(
                        formatVital(s.values.lastOrNull { it != null }),
                        style = MaterialTheme.typography.headlineSmall.copy(color = s.color),
                    )
                    Text(if (series.size > 1) "${s.label} · آخر قيمة" else "آخر قيمة", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (all.size < 2) {
            Gap(8.dp)
            Text("بدها زيارتين على الأقل ليطلع الرسم.", style = MaterialTheme.typography.bodySmall)
        } else {
            val minV = all.min()
            val maxV = all.max()
            val pad = max((maxV - minV) * 0.18f, 1f)
            val lo = minV - pad
            val hi = maxV + pad
            val gridColor = c.border
            val surface = c.surface
            Gap(12.dp)
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                val w = size.width
                val h = size.height
                val padX = 10.dp.toPx()
                val n = dates.size
                fun x(i: Int) = if (n <= 1) w / 2f else padX + i * (w - 2 * padX) / (n - 1)
                fun y(v: Float) = h - (v - lo) / (hi - lo) * h

                for (g in 0..3) {
                    val gy = h * g / 3f
                    drawLine(
                        gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                    )
                }
                series.forEachIndexed { si, s ->
                    val pts = s.values.mapIndexedNotNull { i, v -> v?.let { Offset(x(i), y(it)) } }
                    if (pts.isEmpty()) return@forEachIndexed
                    val path = Path().apply {
                        moveTo(pts[0].x, pts[0].y)
                        for (k in 1 until pts.size) lineTo(pts[k].x, pts[k].y)
                    }
                    if (si == 0 && pts.size > 1) {
                        val fill = Path().apply {
                            addPath(path)
                            lineTo(pts.last().x, h)
                            lineTo(pts.first().x, h)
                            close()
                        }
                        drawPath(fill, Brush.verticalGradient(listOf(s.color.copy(alpha = 0.22f), Color.Transparent)))
                    }
                    drawPath(path, s.color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    pts.forEach { p ->
                        drawCircle(surface, radius = 4.5.dp.toPx(), center = p)
                        drawCircle(s.color, radius = 4.5.dp.toPx(), center = p, style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }
            Gap(6.dp)
            val fmt = SimpleDateFormat("d/M/yy", Locale.US)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmt.format(Date(dates.first())), style = MaterialTheme.typography.labelSmall)
                    Text(fmt.format(Date(dates.last())), style = MaterialTheme.typography.labelSmall)
                }
            }
            Gap(4.dp)
            Text(
                "أقل قيمة ${formatVital(minV)} · أعلى قيمة ${formatVital(maxV)} · ${all.size} قياس",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            )
        }
    }
}

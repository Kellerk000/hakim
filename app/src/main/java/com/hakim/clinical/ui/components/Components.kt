package com.hakim.clinical.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hakim.clinical.ui.theme.Hk

/** خلفية داكنة مع أضواء ملونة بتتحرك ببطء. */
@Composable
fun AuroraBackground(content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "aurora")
    val p by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse),
        label = "aurora-p",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Hk.Bg0, Hk.Bg1, Hk.Bg0)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            fun orb(cx: Float, cy: Float, r: Float, c: Color, a: Float) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c.copy(alpha = a), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(cx, cy),
                )
            }
            orb(w * (0.10f + 0.25f * p), h * 0.10f, w * 0.75f, Hk.Cyan, 0.20f)
            orb(w * (0.95f - 0.30f * p), h * (0.42f + 0.10f * p), w * 0.80f, Hk.Violet, 0.22f)
            orb(w * (0.35f + 0.10f * p), h * (0.98f - 0.08f * p), w * 0.70f, Hk.Sky, 0.14f)
        }
        content()
    }
}

/** بطاقة زجاجية شفافة. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val borderBrush = Brush.linearGradient(
        listOf(
            (accent ?: Color.White).copy(alpha = if (accent != null) 0.60f else 0.20f),
            Color.White.copy(alpha = 0.04f),
        )
    )
    Column(
        modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        (accent ?: Color.White).copy(alpha = if (accent != null) 0.10f else 0.08f),
                        Color.White.copy(alpha = 0.03f),
                    )
                )
            )
            .border(1.dp, borderBrush, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** الزر الرئيسي المتوهج. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    brush: Brush = Hk.Brand,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "btn-scale")
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
            .shadow(if (enabled) 20.dp else 0.dp, shape, ambientColor = Hk.Cyan, spotColor = Hk.Violet)
            .clip(shape)
            .background(brush)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(vertical = 17.dp, horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = Hk.Bg0)
            Text(text, color = Hk.Bg0, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

/** زر ثانوي شفاف بإطار. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = Hk.Text,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, color.copy(alpha = 0.35f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

/** شارة صغيرة ملونة. */
@Composable
fun Pill(
    text: String,
    brush: Brush,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    textColor: Color = Hk.Bg0,
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(brush)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
        Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** شارة بإطار شفاف (للتشخيصات التفريقية مثلاً). */
@Composable
fun OutlineChip(text: String, color: Color = Hk.Sky, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.45f), shape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, color = Hk.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** نص بتدرّج لوني. */
@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 34.sp,
    brush: Brush = Hk.Brand,
    weight: FontWeight = FontWeight.Black,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        style = TextStyle(brush = brush, fontSize = fontSize, fontWeight = weight, lineHeight = fontSize * 1.3f),
    )
}

/** عنوان قسم مع أيقونة بدائرة ملونة. */
@Composable
fun SectionTitle(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodySmall)
    }
}

/** خط تخطيط قلب متحرك. */
@Composable
fun EcgLine(
    modifier: Modifier = Modifier,
    color: Color = Hk.Cyan,
    periodMs: Int = 1500,
    strokeWidth: Dp = 2.5.dp,
) {
    val t = rememberInfiniteTransition(label = "ecg")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing)),
        label = "ecg-phase",
    )
    val pts = remember {
        listOf(
            0f to 0f, 0.28f to 0f, 0.32f to 0.12f, 0.36f to 0f, 0.43f to 0f, 0.46f to -0.20f,
            0.50f to 1f, 0.54f to -0.45f, 0.57f to 0f, 0.66f to 0f, 0.72f to 0.24f, 0.78f to 0f, 1f to 0f,
        )
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val mid = h / 2f
        val beat = (w / 2.3f).coerceAtLeast(1f)
        val start = phase * beat - beat
        val path = Path()
        var first = true
        var k = 0
        while (start + k * beat < w + beat) {
            for ((px, py) in pts) {
                val x = start + k * beat + px * beat
                val y = mid - py * h * 0.42f
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
            }
            k++
        }
        val brush = Brush.horizontalGradient(
            listOf(Color.Transparent, color, color, Color.Transparent),
            startX = 0f,
            endX = w,
        )
        drawPath(path, brush, alpha = 0.25f, style = Stroke(width = strokeWidth.toPx() * 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, brush, style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** دائرة نابضة مع أيقونة بالنص. */
@Composable
fun PulseOrb(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 140.dp) {
    val t = rememberInfiniteTransition(label = "pulse")
    val p by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing)),
        label = "pulse-p",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            for (i in 0 until 3) {
                val q = (p + i / 3f) % 1f
                drawCircle(
                    color = Hk.Cyan.copy(alpha = (1f - q) * 0.45f),
                    radius = r * (0.45f + 0.55f * q),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Box(
            Modifier
                .size(size * 0.46f)
                .shadow(24.dp, CircleShape, ambientColor = Hk.Cyan, spotColor = Hk.Violet)
                .clip(CircleShape)
                .background(Hk.Brand),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Hk.Bg0, modifier = Modifier.size(size * 0.24f))
        }
    }
}

/** شريط تقدم بتدرّج. */
@Composable
fun GradientProgress(progress: Float, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(300), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(Hk.Brand)
        )
    }
}

/** ثلاث نقاط بتتحرك (عم يكتب...). */
@Composable
fun TypingDots(modifier: Modifier = Modifier, color: Color = Hk.Cyan) {
    val t = rememberInfiniteTransition(label = "dots")
    val p by t.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "dots-p",
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 3) {
            val active = p.toInt() == i
            Box(
                Modifier
                    .size(if (active) 9.dp else 7.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (active) 1f else 0.35f))
            )
        }
    }
}

/** نقطة نابضة (للحالات الإسعافية). */
@Composable
fun BlinkDot(color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "blink")
    val a by t.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "blink-a",
    )
    Box(
        modifier
            .size(10.dp)
            .graphicsLayer { alpha = a }
            .clip(CircleShape)
            .background(color)
    )
}

/** يظهر المحتوى بحركة لطيفة أول ما ينرسم. */
@Composable
fun Appear(delayMs: Int = 0, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(420, delayMillis = delayMs)) +
            slideInVertically(tween(420, delayMillis = delayMs, easing = FastOutSlowInEasing)) { it / 6 },
    ) {
        content()
    }
}

/** شريط ترتيب للاحتمالات (طول نسبي، مو احتمال رقمي). */
@Composable
fun RankBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(fraction, tween(700, easing = FastOutSlowInEasing), label = "rank")
    Box(
        modifier
            .height(6.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.5f))))
        )
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Hk.BrandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Hk.Cyan, modifier = Modifier.size(36.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

/** مسافة عمودية. */
@Composable
fun Gap(height: Dp) {
    Box(Modifier.height(height).width(1.dp))
}

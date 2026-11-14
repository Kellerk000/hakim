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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hakim.clinical.ui.theme.Hk

/** خلفية هادية: تدرّج خفيف مع ضوء ناعم. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    val c = Hk.c
    val glowA = c.primary.copy(alpha = if (c.isDark) 0.10f else 0.08f)
    val glowB = c.info.copy(alpha = if (c.isDark) 0.08f else 0.06f)
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.bg, c.bg2)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(listOf(glowA, Color.Transparent), center = Offset(w * 0.15f, h * 0.05f), radius = w * 0.9f),
                radius = w * 0.9f,
                center = Offset(w * 0.15f, h * 0.05f),
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(glowB, Color.Transparent), center = Offset(w * 0.95f, h * 0.65f), radius = w * 0.8f),
                radius = w * 0.8f,
                center = Offset(w * 0.95f, h * 0.65f),
            )
        }
        content()
    }
}

/** بطاقة: بالنهاري بيضا مع ظل ناعم، وبالليلي سطح داكن مع إطار. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Hk.c
    val shape = RoundedCornerShape(22.dp)
    val borderColor = when {
        accent != null -> accent.copy(alpha = if (c.isDark) 0.45f else 0.35f)
        else -> c.border
    }
    val bg = if (accent != null) {
        Brush.verticalGradient(listOf(mix(c.surface, accent, if (c.isDark) 0.08f else 0.05f), c.surface))
    } else {
        Brush.verticalGradient(listOf(c.surface, c.surface))
    }
    Column(
        modifier
            .then(if (!c.isDark) Modifier.shadow(6.dp, shape, ambientColor = Color(0x22102040), spotColor = Color(0x22102040)) else Modifier)
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** يمزج لونين (لتلوين خفيف ثابت بدون شفافية). */
fun mix(base: Color, tint: Color, amount: Float): Color = Color(
    red = base.red + (tint.red - base.red) * amount,
    green = base.green + (tint.green - base.green) * amount,
    blue = base.blue + (tint.blue - base.blue) * amount,
    alpha = 1f,
)

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    color: Color? = null,
) {
    val c = Hk.c
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "btn-scale")
    val shape = RoundedCornerShape(18.dp)
    val brush = if (color != null) Brush.linearGradient(listOf(color, color)) else c.brand
    val fg = if (color != null) Color.White else c.onPrimary
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.45f
            }
            .shadow(if (enabled) 10.dp else 0.dp, shape, ambientColor = c.primary, spotColor = c.primary)
            .clip(shape)
            .background(brush)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
            Text(text, color = fg, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = fg))
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color? = null,
    enabled: Boolean = true,
) {
    val c = Hk.c
    val col = color ?: c.text
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .clip(shape)
            .background(if (color != null) c.tint(color) else c.surfaceAlt)
            .border(1.dp, if (color != null) color.copy(alpha = 0.35f) else c.border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = col, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.labelLarge.copy(color = col), maxLines = 1)
    }
}

/** شارة: ملوّنة خفيف (أو معبّاية). */
@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = false,
) {
    val c = Hk.c
    val fg = if (filled) Color.White else color
    Row(
        modifier
            .clip(CircleShape)
            .background(if (filled) color else c.tint(color))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.labelMedium.copy(color = fg, fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun OutlineChip(text: String, color: Color, modifier: Modifier = Modifier) {
    val c = Hk.c
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .clip(shape)
            .background(c.tint(color))
            .border(1.dp, color.copy(alpha = 0.35f), shape)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** الاسم بتدرّج لوني. */
@Composable
fun BrandText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 30.sp,
    brush: Brush? = null,
    textAlign: TextAlign? = null,
) {
    val c = Hk.c
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        style = MaterialTheme.typography.headlineLarge.copy(
            brush = brush ?: c.brand,
            fontSize = fontSize,
            lineHeight = fontSize * 1.25f,
            fontWeight = FontWeight.Bold,
        ),
    )
}

@Composable
fun SectionTitle(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, trailing: String? = null) {
    val c = Hk.c
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(c.tint(color)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp).weight(1f))
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Hk.c
    Row(modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "رجوع",
                    tint = c.text,
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = if (onBack == null) 4.dp else 0.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

@Composable
fun ActionIcon(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color? = null) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = tint ?: Hk.c.textDim)
    }
}

/** دائرة بأول حرفين من الاسم ولون ثابت لكل مريض. */
@Composable
fun Avatar(seed: String, initials: String, size: Dp = 46.dp) {
    val c = Hk.c
    val palette = listOf(c.primary, c.info, c.secondary, c.pink, c.warning, c.success)
    val col = palette[(seed.hashCode() and 0x7fffffff) % palette.size]
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(col.copy(alpha = 0.85f), col.copy(alpha = 0.55f)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36f).sp,
            ),
        )
    }
}

@Composable
fun StatCard(value: String, label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    AppCard(modifier, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Hk.c.tint(color)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp)) }
        }
        Gap(10.dp)
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/** خط تخطيط قلب متحرك. */
@Composable
fun EcgLine(
    modifier: Modifier = Modifier,
    color: Color = Hk.c.primary,
    periodMs: Int = 1700,
    strokeWidth: Dp = 2.2.dp,
    animated: Boolean = true,
) {
    val t = rememberInfiniteTransition(label = "ecg")
    val phaseAnim by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing)),
        label = "ecg-phase",
    )
    val phase = if (animated) phaseAnim else 0.35f
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
        val brush = Brush.horizontalGradient(listOf(Color.Transparent, color, color, Color.Transparent), startX = 0f, endX = w)
        drawPath(path, brush, alpha = 0.18f, style = Stroke(width = strokeWidth.toPx() * 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, brush, style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun PulseOrb(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 130.dp) {
    val c = Hk.c
    val t = rememberInfiniteTransition(label = "pulse")
    val p by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing)),
        label = "pulse-p",
    )
    val ring = c.primary
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            for (i in 0 until 3) {
                val q = (p + i / 3f) % 1f
                drawCircle(color = ring.copy(alpha = (1f - q) * 0.35f), radius = r * (0.48f + 0.52f * q), style = Stroke(width = 1.5.dp.toPx()))
            }
        }
        Box(
            Modifier
                .size(size * 0.48f)
                .shadow(14.dp, CircleShape, ambientColor = c.primary, spotColor = c.primary)
                .clip(CircleShape)
                .background(c.brand),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(size * 0.24f))
        }
    }
}

@Composable
fun GradientProgress(progress: Float, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val c = Hk.c
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(300), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(c.surfaceAlt)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(c.brand)
        )
    }
}

@Composable
fun TypingDots(modifier: Modifier = Modifier, color: Color = Hk.c.primary) {
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
                    .size(if (active) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (active) 1f else 0.35f))
            )
        }
    }
}

@Composable
fun BlinkDot(color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "blink")
    val a by t.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
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

@Composable
fun Appear(delayMs: Int = 0, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(380, delayMillis = delayMs)) +
            slideInVertically(tween(380, delayMillis = delayMs, easing = FastOutSlowInEasing)) { it / 8 },
    ) {
        content()
    }
}

@Composable
fun RankBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val c = Hk.c
    val animated by animateFloatAsState(fraction, tween(700, easing = FastOutSlowInEasing), label = "rank")
    Box(
        modifier
            .height(6.dp)
            .clip(CircleShape)
            .background(c.surfaceAlt)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.55f))))
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val c = Hk.c
    Column(
        modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(c.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.primary, modifier = Modifier.size(38.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        if (action != null) {
            Gap(6.dp)
            action()
        }
    }
}

@Composable
fun InfoBanner(text: String, color: Color, icon: ImageVector, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val c = Hk.c
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.tint(color))
            .border(1.dp, color.copy(alpha = 0.3f), shape)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
        }
        if (action != null) {
            Gap(10.dp)
            action()
        }
    }
}

@Composable
fun Gap(height: Dp) {
    Box(Modifier.height(height).width(1.dp))
}

@Composable
fun HSpace(width: Dp) {
    Box(Modifier.width(width).height(1.dp))
}

val CaptionStyle: TextStyle
    @Composable get() = MaterialTheme.typography.labelMedium

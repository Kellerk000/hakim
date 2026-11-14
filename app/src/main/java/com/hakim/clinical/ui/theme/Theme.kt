package com.hakim.clinical.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp

object Hk {
    val Bg0 = Color(0xFF060A18)
    val Bg1 = Color(0xFF0B1230)
    val Bg2 = Color(0xFF111A3F)
    val Cyan = Color(0xFF2DE2C5)
    val Sky = Color(0xFF38BDF8)
    val Violet = Color(0xFF8B5CF6)
    val Pink = Color(0xFFF472B6)
    val Danger = Color(0xFFFF4D6D)
    val Warning = Color(0xFFFFB020)
    val Success = Color(0xFF34D399)
    val Text = Color(0xFFEAF0FF)
    val TextDim = Color(0xFFA3AFCF)
    val TextFaint = Color(0xFF67739A)
    val Stroke = Color(0x29FFFFFF)

    val Brand = Brush.linearGradient(listOf(Cyan, Sky, Violet))
    val BrandSoft = Brush.linearGradient(listOf(Cyan.copy(alpha = 0.22f), Violet.copy(alpha = 0.22f)))
    val StrongGrad = Brush.linearGradient(listOf(Success, Cyan))
    val MultiGrad = Brush.linearGradient(listOf(Warning, Pink, Violet))
    val DangerGrad = Brush.linearGradient(listOf(Danger, Pink))
}

private val scheme = darkColorScheme(
    primary = Hk.Cyan,
    onPrimary = Hk.Bg0,
    secondary = Hk.Violet,
    onSecondary = Hk.Text,
    background = Hk.Bg0,
    onBackground = Hk.Text,
    surface = Hk.Bg1,
    onSurface = Hk.Text,
    surfaceVariant = Hk.Bg2,
    onSurfaceVariant = Hk.TextDim,
    error = Hk.Danger,
    outline = Hk.Stroke,
)

private val base = Typography()

private val typography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black, color = Hk.Text),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Black, color = Hk.Text),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Hk.Text),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, color = Hk.Text),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, color = Hk.Text),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = Hk.Text),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold, color = Hk.Text),
    bodyLarge = base.bodyLarge.copy(fontSize = 17.sp, lineHeight = 28.sp, color = Hk.Text),
    bodyMedium = base.bodyMedium.copy(fontSize = 15.sp, lineHeight = 24.sp, color = Hk.Text),
    bodySmall = base.bodySmall.copy(fontSize = 13.sp, lineHeight = 20.sp, color = Hk.TextDim),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold),
)

val MonoSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, color = Hk.TextFaint)

@Composable
fun HakimTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography) {
        // التطبيق كله من اليمين لليسار
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            content()
        }
    }
}

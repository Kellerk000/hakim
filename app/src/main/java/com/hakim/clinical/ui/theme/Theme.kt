package com.hakim.clinical.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp

/** ألوان DrAli: هادية ومريحة للعين، بنسخة نهارية وليلية. */
@Immutable
data class AppColors(
    val isDark: Boolean,
    val bg: Color,
    val bg2: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val text: Color,
    val textDim: Color,
    val textFaint: Color,
    val primary: Color,
    val onPrimary: Color,
    val info: Color,
    val secondary: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val pink: Color,
) {
    val brand: Brush get() = Brush.linearGradient(listOf(primary, info))
    val brandSoft: Brush get() = Brush.linearGradient(listOf(primary.copy(alpha = 0.16f), info.copy(alpha = 0.16f)))
    val primarySoft: Color get() = primary.copy(alpha = if (isDark) 0.14f else 0.10f)
    fun tint(c: Color): Color = c.copy(alpha = if (isDark) 0.14f else 0.10f)
}

val DarkColors = AppColors(
    isDark = true,
    bg = Color(0xFF0B1120),
    bg2 = Color(0xFF0F172A),
    surface = Color(0xFF131C2E),
    surfaceAlt = Color(0xFF18233A),
    border = Color(0xFF243250),
    text = Color(0xFFE6EDF7),
    textDim = Color(0xFF9AA8BF),
    textFaint = Color(0xFF66758F),
    primary = Color(0xFF3CCFB8),
    onPrimary = Color(0xFF04201C),
    info = Color(0xFF5AB2F5),
    secondary = Color(0xFF8C9BF8),
    success = Color(0xFF52D59B),
    warning = Color(0xFFF2B552),
    danger = Color(0xFFF26D7D),
    pink = Color(0xFFE38BC0),
)

val LightColors = AppColors(
    isDark = false,
    bg = Color(0xFFF4F7FA),
    bg2 = Color(0xFFEAF0F6),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF6F9FC),
    border = Color(0xFFE1E8F0),
    text = Color(0xFF0F1E33),
    textDim = Color(0xFF52627A),
    textFaint = Color(0xFF8A99AE),
    primary = Color(0xFF0E9E8A),
    onPrimary = Color(0xFFFFFFFF),
    info = Color(0xFF2F8FE0),
    secondary = Color(0xFF5B6CF0),
    success = Color(0xFF17A673),
    warning = Color(0xFFD48A0C),
    danger = Color(0xFFDC3D52),
    pink = Color(0xFFC2418B),
)

val LocalAppColors = staticCompositionLocalOf { DarkColors }

object Hk {
    val c: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

fun resolveDark(themeMode: Int, systemDark: Boolean): Boolean = when (themeMode) {
    1 -> false
    2 -> true
    else -> systemDark
}

/** خط IBM Plex Sans Arabic (بينزل وقت البناء). إذا مو موجود منرجع لخط الجهاز. */
@OptIn(ExperimentalTextApi::class)
private fun loadFontFamily(context: Context): FontFamily {
    val files = runCatching { context.assets.list("fonts")?.toSet() }.getOrNull().orEmpty()
    val spec = listOf(
        "Regular" to FontWeight.Normal,
        "Medium" to FontWeight.Medium,
        "SemiBold" to FontWeight.SemiBold,
        "Bold" to FontWeight.Bold,
    )
    val fonts = spec.mapNotNull { (name, weight) ->
        val f = "IBMPlexSansArabic-$name.ttf"
        if (f in files) Font("fonts/$f", context.assets, weight) else null
    }
    return if (fonts.isEmpty()) FontFamily.Default else FontFamily(fonts)
}

private fun typography(family: FontFamily, c: AppColors): Typography {
    val b = Typography()
    fun TextStyle.f(color: Color = c.text) = copy(fontFamily = family, color = color)
    return Typography(
        displaySmall = b.displaySmall.f().copy(fontWeight = FontWeight.Bold),
        headlineLarge = b.headlineLarge.f().copy(fontWeight = FontWeight.Bold),
        headlineMedium = b.headlineMedium.f().copy(fontWeight = FontWeight.Bold),
        headlineSmall = b.headlineSmall.f().copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
        titleLarge = b.titleLarge.f().copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
        titleMedium = b.titleMedium.f().copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
        titleSmall = b.titleSmall.f().copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        bodyLarge = b.bodyLarge.f().copy(fontSize = 16.sp, lineHeight = 27.sp),
        bodyMedium = b.bodyMedium.f().copy(fontSize = 15.sp, lineHeight = 24.sp),
        bodySmall = b.bodySmall.f(c.textDim).copy(fontSize = 13.sp, lineHeight = 20.sp),
        labelLarge = b.labelLarge.f().copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        labelMedium = b.labelMedium.f(c.textDim).copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
        labelSmall = b.labelSmall.f(c.textFaint),
    )
}

@Composable
fun DrAliTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = resolveDark(themeMode, isSystemInDarkTheme())
    val c = if (dark) DarkColors else LightColors
    val context = LocalContext.current
    val family = remember { loadFontFamily(context) }
    val typo = remember(dark, family) { typography(family, c) }
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.primary, onPrimary = c.onPrimary, secondary = c.secondary,
            background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text,
            surfaceVariant = c.surfaceAlt, onSurfaceVariant = c.textDim, error = c.danger, outline = c.border,
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = c.onPrimary, secondary = c.secondary,
            background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text,
            surfaceVariant = c.surfaceAlt, onSurfaceVariant = c.textDim, error = c.danger, outline = c.border,
        )
    }
    MaterialTheme(colorScheme = scheme, typography = typo) {
        CompositionLocalProvider(
            LocalAppColors provides c,
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalContentColor provides c.text,
        ) {
            content()
        }
    }
}

val MonoSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp)

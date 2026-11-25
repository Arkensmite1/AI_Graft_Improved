package com.aigraft.farmer.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aigraft.farmer.R

@Immutable
data class AiGraftColors(
    val surface: Color = Color(0xFFFFFFFF),
    val onSurface: Color = Color(0xFF111827),
    val surfaceSecondary: Color = Color(0xFFF3F4F6),
    val onSurfaceSecondary: Color = Color(0xFF1F2937),
    val surfaceTertiary: Color = Color(0xFFE5E7EB),
    val onSurfaceTertiary: Color = Color(0xFF374151),
    val surfaceInverse: Color = Color(0xFF111827),
    val onSurfaceInverse: Color = Color(0xFFFFFFFF),
    val muted: Color = Color(0xFF6B7280),

    val brand: Color = Color(0xFF166534),
    val onBrand: Color = Color(0xFFFFFFFF),
    val brandPrimary: Color = Color(0xFF15803D),
    val onBrandPrimary: Color = Color(0xFFFFFFFF),
    val brandSecondary: Color = Color(0xFFDCFCE7),
    val onBrandSecondary: Color = Color(0xFF166534),
    val brandTertiary: Color = Color(0xFF86EFAC),
    val onBrandTertiary: Color = Color(0xFF14532D),

    val success: Color = Color(0xFF16A34A),
    val onSuccess: Color = Color(0xFFFFFFFF),
    val warning: Color = Color(0xFFEAB308),
    val onWarning: Color = Color(0xFF1F2937),
    val error: Color = Color(0xFFDC2626),
    val onError: Color = Color(0xFFFFFFFF),
    val info: Color = Color(0xFF1D4ED8),
    val onInfo: Color = Color(0xFFFFFFFF),

    val border: Color = Color(0xFFE5E7EB),
    val borderStrong: Color = Color(0xFFD1D5DB),
    val divider: Color = Color(0xFFF3F4F6),
)

val LightAiGraftColors = AiGraftColors()

val LocalAiGraftColors = staticCompositionLocalOf { LightAiGraftColors }

@OptIn(ExperimentalTextApi::class)
val Baloo2FontFamily: FontFamily = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    FontFamily(
        Font(
            resId = R.font.baloo_2,
            weight = FontWeight.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400))
        ),
        Font(
            resId = R.font.baloo_2,
            weight = FontWeight.Medium,
            variationSettings = FontVariation.Settings(FontVariation.weight(500))
        ),
        Font(
            resId = R.font.baloo_2,
            weight = FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(600))
        ),
        Font(
            resId = R.font.baloo_2,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(700))
        ),
        Font(
            resId = R.font.baloo_2,
            weight = FontWeight.ExtraBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(800))
        ),
    )
} else {
    FontFamily(
        Font(resId = R.font.baloo_2, weight = FontWeight.Normal),
        Font(resId = R.font.baloo_2, weight = FontWeight.SemiBold),
        Font(resId = R.font.baloo_2, weight = FontWeight.Bold),
        Font(resId = R.font.baloo_2, weight = FontWeight.ExtraBold),
    )
}

private val AiGraftMaterialColorScheme = lightColorScheme(
    primary = LightAiGraftColors.brandPrimary,
    onPrimary = LightAiGraftColors.onBrandPrimary,
    primaryContainer = LightAiGraftColors.brandSecondary,
    onPrimaryContainer = LightAiGraftColors.onBrandSecondary,
    secondary = LightAiGraftColors.brand,
    onSecondary = LightAiGraftColors.onBrand,
    secondaryContainer = LightAiGraftColors.brandSecondary,
    onSecondaryContainer = LightAiGraftColors.onBrandSecondary,
    tertiary = LightAiGraftColors.brandTertiary,
    onTertiary = LightAiGraftColors.onBrandTertiary,
    background = LightAiGraftColors.surface,
    onBackground = LightAiGraftColors.onSurface,
    surface = LightAiGraftColors.surface,
    onSurface = LightAiGraftColors.onSurface,
    surfaceVariant = LightAiGraftColors.surfaceSecondary,
    onSurfaceVariant = LightAiGraftColors.onSurfaceSecondary,
    outline = LightAiGraftColors.borderStrong,
    outlineVariant = LightAiGraftColors.border,
    error = LightAiGraftColors.error,
    onError = LightAiGraftColors.onError,
    inverseSurface = LightAiGraftColors.surfaceInverse,
    inverseOnSurface = LightAiGraftColors.onSurfaceInverse,
)

private val AiGraftTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Baloo2FontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun AiGraftTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAiGraftColors provides LightAiGraftColors) {
        MaterialTheme(
            colorScheme = AiGraftMaterialColorScheme,
            typography = AiGraftTypography,
            content = content,
        )
    }
}

object AiGraftTokens {
    val colors: AiGraftColors
        @Composable get() = LocalAiGraftColors.current
}

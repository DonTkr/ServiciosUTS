package com.uts.serviciosestudiantiles.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Identidad visual
 * - Verde institucional UTS como color principal, sobre un fondo mineral frío.
 * - El color tiene significado: verde = beca (ahorro), ámbar = recargo Icetex.
 * - El carné estudiantil es el único elemento "fuerte"; el resto se mantiene sobrio.
 */
private val Claro = lightColorScheme(
    primary = Color(0xFF0B5D3B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDE8D6),
    onPrimaryContainer = Color(0xFF002113),
    inversePrimary = Color(0xFF8FD5A9),
    secondary = Color(0xFF4D6356),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDEBE2),
    onSecondaryContainer = Color(0xFF0A1F14),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE6C4),
    onTertiaryContainer = Color(0xFF2C1A00),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF3F6F4),
    onBackground = Color(0xFF14201A),
    surface = Color(0xFFF3F6F4),
    onSurface = Color(0xFF14201A),
    surfaceVariant = Color(0xFFDCE5DE),
    onSurfaceVariant = Color(0xFF4A544D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEDF2EF),
    surfaceContainer = Color(0xFFE7EEEA),
    surfaceContainerHigh = Color(0xFFE1E9E4),
    surfaceContainerHighest = Color(0xFFDBE3DE),
    outline = Color(0xFF717973),
    outlineVariant = Color(0xFFD3DBD5)
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF8FD5A9),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF00522F),
    onPrimaryContainer = Color(0xFFABF2C4),
    inversePrimary = Color(0xFF0B5D3B),
    secondary = Color(0xFFB4CCBC),
    onSecondary = Color(0xFF1F3529),
    secondaryContainer = Color(0xFF2A3D32),
    onSecondaryContainer = Color(0xFFD0E8D8),
    tertiary = Color(0xFFFFB950),
    onTertiary = Color(0xFF482A00),
    tertiaryContainer = Color(0xFF4F3300),
    onTertiaryContainer = Color(0xFFFFDDB0),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF0E1411),
    onBackground = Color(0xFFDDE4DF),
    surface = Color(0xFF0E1411),
    onSurface = Color(0xFFDDE4DF),
    surfaceVariant = Color(0xFF414943),
    onSurfaceVariant = Color(0xFFC0C9C2),
    surfaceContainerLowest = Color(0xFF0A0F0C),
    surfaceContainerLow = Color(0xFF161C19),
    surfaceContainer = Color(0xFF1A201D),
    surfaceContainerHigh = Color(0xFF242A27),
    surfaceContainerHighest = Color(0xFF2F3532),
    outline = Color(0xFF8A938C),
    outlineVariant = Color(0xFF3A423D)
)

/** Colores propios del carné (no existen en el esquema M3). */
@Immutable
data class ColoresCarne(
    val fondo: Color,
    val fondoProfundo: Color,
    val texto: Color,
    val textoSuave: Color,
    val acento: Color
)

private val CarneClaro = ColoresCarne(
    fondo = Color(0xFF0B5D3B),
    fondoProfundo = Color(0xFF07432A),
    texto = Color(0xFFFFFFFF),
    textoSuave = Color(0xFFB9D9C5),
    acento = Color(0xFFC9E265)
)

private val CarneOscuro = ColoresCarne(
    fondo = Color(0xFF124A33),
    fondoProfundo = Color(0xFF0A3423),
    texto = Color(0xFFF1F7F3),
    textoSuave = Color(0xFFA6C9B3),
    acento = Color(0xFFC9E265)
)

val LocalColoresCarne = staticCompositionLocalOf { CarneClaro }

private val Base = Typography()

private val Tipografia = Base.copy(
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold)
)

/** Radios según jerarquía: chips 8, campos/cards 14-18, carné 24. */
private val Formas = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Cifras tabulares: los valores en pesos quedan alineados columna a columna. */
val EstiloCifras = TextStyle(fontFeatureSettings = "tnum")

/** Estilo del total grande (recibo y total acumulado). */
val EstiloTotal = TextStyle(
    fontSize = 34.sp,
    lineHeight = 40.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.8).sp,
    fontFeatureSettings = "tnum"
)

@Composable
fun ServiciosUTSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Sin color dinámico a propósito: la app mantiene su identidad institucional.
    androidx.compose.runtime.CompositionLocalProvider(
        LocalColoresCarne provides if (darkTheme) CarneOscuro else CarneClaro
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) Oscuro else Claro,
            typography = Tipografia,
            shapes = Formas,
            content = content
        )
    }
}

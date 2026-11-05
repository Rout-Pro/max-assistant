package com.rout.max.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/** MAX JARVIS-HUD palette: black base, neon cyan accents, thin glowing borders. */
object MaxColors {
    val Black = Color(0xFF000000)
    val NearBlack = Color(0xFF05070A)
    val Glass = Color(0x0D35E0FF)          // faint cyan-tinted glass surface
    val GlassStrong = Color(0x1A35E0FF)
    val Border = Color(0x2B35E0FF)         // thin cyan border
    val HudBorder = Color(0x4035E0FF)      // brighter HUD panel border
    val TextPrimary = Color(0xFFEAFBFF)
    val TextSecondary = Color(0xFF8FA8B8)  // muted blue-grey
    val Accent = Color(0xFF5CE1FF)         // neon cyan
    val HudCyan = Color(0xFF5CE1FF)
    val HudCyanBright = Color(0xFF9BF1FF)
    val HudBlue = Color(0xFF38BDF8)
    val AccentViolet = Color(0xFF38BDF8)   // kept name; now electric blue
    val AccentBlue = Color(0xFF2FB7FF)
    val Success = Color(0xFF6FE3A5)
    val Error = Color(0xFFFF7A7A)
    val Warning = Color(0xFFC9A96A)
}

val MaxColorScheme = darkColorScheme(
    primary = MaxColors.Accent,
    onPrimary = MaxColors.Black,
    background = MaxColors.Black,
    onBackground = MaxColors.TextPrimary,
    surface = MaxColors.NearBlack,
    onSurface = MaxColors.TextPrimary,
    surfaceVariant = MaxColors.Glass,
    onSurfaceVariant = MaxColors.TextSecondary,
    outline = MaxColors.Border
)

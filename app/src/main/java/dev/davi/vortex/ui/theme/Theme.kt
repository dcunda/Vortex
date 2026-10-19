package dev.davi.vortex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Paleta do Vortex: fundo quase preto com neon ciano → violeta → magenta. */
object VX {
    val Bg = Color(0xFF05060B)
    val Bg2 = Color(0xFF0C0E1A)
    val Cyan = Color(0xFF22E4FF)
    val Violet = Color(0xFF8B5CF6)
    val Magenta = Color(0xFFFF3DCB)
    val Lime = Color(0xFF7CFFB2)
    val Amber = Color(0xFFFFC857)
    val Danger = Color(0xFFFF5C7A)
    val Text = Color(0xFFEFF3FF)
    val Muted = Color(0xFF8A93B2)
    val Faint = Color(0xFF4A5170)
    val Neon = listOf(Cyan, Violet, Magenta)
}

@Composable
fun VortexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = VX.Cyan,
            onPrimary = VX.Bg,
            secondary = VX.Violet,
            background = VX.Bg,
            onBackground = VX.Text,
            surface = VX.Bg2,
            onSurface = VX.Text,
            error = VX.Danger,
        ),
        content = content,
    )
}

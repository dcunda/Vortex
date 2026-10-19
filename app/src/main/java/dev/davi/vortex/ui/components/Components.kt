package dev.davi.vortex.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.davi.vortex.data.MediaKind
import dev.davi.vortex.data.Platform
import dev.davi.vortex.data.QualityOption
import dev.davi.vortex.data.Format
import dev.davi.vortex.ui.theme.VX

/** Cartão de "vidro": fundo translúcido com borda em degradê. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.075f), Color.White.copy(alpha = 0.025f))))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.03f), VX.Cyan.copy(alpha = 0.16f))),
                shape,
            )
            .padding(contentPadding),
        content = content,
    )
}

/** Botão principal com degradê neon em movimento e efeito de pressão. */
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "scale",
    )
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f, label = "alpha")
    val t = rememberInfiniteTransition(label = "btn")
    val shift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "shift")
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .shadow(if (enabled) 24.dp else 0.dp, shape, ambientColor = VX.Cyan, spotColor = VX.Violet)
            .clip(shape)
            .drawBehind {
                val w = size.width
                val off = w * 2 * shift
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(VX.Cyan, VX.Violet, VX.Magenta, VX.Violet, VX.Cyan),
                        start = Offset(-off, 0f),
                        end = Offset(-off + w * 2, size.height),
                        tileMode = TileMode.Repeated,
                    ),
                )
                // brilho superior
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), endY = size.height * 0.6f))
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.2.sp)
            }
        }
    }
}

/** Barra de progresso neon. progress < 0 = indeterminada. */
@Composable
fun NeonProgress(progress: Float, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(350), label = "p")
    val indeterminate = progress < 0f
    val t = rememberInfiniteTransition(label = "pi")
    val shift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "shift")
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(Color.White.copy(alpha = 0.08f), cornerRadius = r)
        val brush = Brush.horizontalGradient(VX.Neon, startX = 0f, endX = size.width)
        if (indeterminate) {
            val w = size.width * 0.32f
            val x = -w + (size.width + w) * shift
            clipRect {
                drawRoundRect(brush, topLeft = Offset(x, 0f), size = Size(w, size.height), cornerRadius = r)
            }
        } else {
            val w = size.width * animated
            if (w > 0f) {
                drawRoundRect(brush, size = Size(w, size.height), cornerRadius = r)
                clipRect(right = w) {
                    val sx = -size.width * 0.3f + size.width * 1.6f * shift
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent),
                            startX = sx - 70f,
                            endX = sx + 70f,
                        ),
                    )
                }
            }
        }
    }
}

/** Alternador MP4 / MP3 com indicador deslizante. */
@Composable
fun FormatToggle(mode: MediaKind, videoEnabled: Boolean, onChange: (MediaKind) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(4.dp),
    ) {
        val half = maxWidth / 2
        val x by animateDpAsState(
            if (mode == MediaKind.VIDEO) 0.dp else half,
            spring(dampingRatio = 0.75f, stiffness = 420f),
            label = "toggle",
        )
        Box(
            Modifier
                .offset(x = x)
                .width(half)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(VX.Cyan, VX.Violet))),
        )
        Row(Modifier.fillMaxSize()) {
            ToggleItem("MP4", "Vídeo", Icons.Rounded.Movie, mode == MediaKind.VIDEO, videoEnabled, Modifier.weight(1f)) {
                onChange(MediaKind.VIDEO)
            }
            ToggleItem("MP3", "Áudio", Icons.Rounded.MusicNote, mode == MediaKind.AUDIO, true, Modifier.weight(1f)) {
                onChange(MediaKind.AUDIO)
            }
        }
    }
}

@Composable
private fun ToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(if (selected) Color.White else VX.Muted, label = "c")
    Row(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
        Spacer(Modifier.width(6.dp))
        Text(subtitle, color = color.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

/** Chip de qualidade (1080p, 320 kbps…) com tamanho estimado. */
@Composable
fun QualityChip(option: QualityOption, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val bg by animateColorAsState(
        if (selected) VX.Cyan.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.04f),
        label = "bg",
    )
    val scale by animateFloatAsState(if (selected) 1.04f else 1f, spring(0.55f, 520f), label = "s")
    val border: Brush = if (selected) Brush.linearGradient(listOf(VX.Cyan, VX.Violet)) else SolidColor(Color.White.copy(alpha = 0.08f))
    Column(
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            option.label,
            color = if (selected) VX.Text else VX.Text.copy(alpha = 0.85f),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        val sub = listOfNotNull(option.detail.takeIf { it.isNotBlank() }, option.sizeBytes?.let { "~" + Format.bytes(it) })
        Text(sub.joinToString(" • "), color = if (selected) VX.Cyan else VX.Muted, fontSize = 11.sp)
    }
}

/** Selo da plataforma (YouTube, Instagram, TikTok). */
@Composable
fun PlatformBadge(platform: Platform, modifier: Modifier = Modifier, active: Boolean = true, dim: Boolean = false) {
    val c = Color(platform.color)
    val bg by animateColorAsState(if (active) c.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f), label = "pb")
    val alpha by animateFloatAsState(if (dim) 0.35f else 1f, label = "pa")
    Row(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (active) c.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(c))
        Spacer(Modifier.width(6.dp))
        Text(platform.label, color = VX.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Tag(text: String, modifier: Modifier = Modifier, strong: Boolean = false) {
    Text(
        text,
        color = if (strong) Color.White else VX.Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (strong) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, color = VX.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = modifier)
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = VX.Text,
    size: Dp = 44.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}

/** Barra de navegação flutuante (pílula de vidro). */
@Composable
fun FloatingNav(selected: Int, activeCount: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier
            .width(268.dp)
            .height(64.dp)
            .shadow(28.dp, shape, ambientColor = VX.Violet, spotColor = VX.Violet)
            .clip(shape)
            .background(VX.Bg2.copy(alpha = 0.94f))
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f))), shape)
            .padding(6.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val itemW = maxWidth / 2
            val x by animateDpAsState(itemW * selected, spring(dampingRatio = 0.72f, stiffness = 420f), label = "nav")
            Box(
                Modifier
                    .offset(x = x)
                    .width(itemW)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.horizontalGradient(listOf(VX.Cyan.copy(alpha = 0.22f), VX.Violet.copy(alpha = 0.32f))))
                    .border(1.dp, VX.Cyan.copy(alpha = 0.35f), RoundedCornerShape(26.dp)),
            )
            Row(Modifier.fillMaxSize()) {
                NavItem(Icons.Rounded.Bolt, "Baixar", selected == 0, 0, Modifier.weight(1f)) { onSelect(0) }
                NavItem(Icons.Rounded.VideoLibrary, "Biblioteca", selected == 1, activeCount, Modifier.weight(1f)) { onSelect(1) }
            }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, badge: Int, modifier: Modifier, onClick: () -> Unit) {
    val color by animateColorAsState(if (selected) VX.Text else VX.Muted, label = "nc")
    Row(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(26.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Icon(icon, label, tint = if (selected) VX.Cyan else color, modifier = Modifier.size(22.dp))
            if (badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-6).dp)
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(VX.Magenta, VX.Violet))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$badge", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

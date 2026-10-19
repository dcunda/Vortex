package dev.davi.vortex.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.davi.vortex.data.Format
import dev.davi.vortex.data.MediaInfo
import dev.davi.vortex.data.MediaKind
import dev.davi.vortex.data.Platform
import dev.davi.vortex.data.UrlUtils
import dev.davi.vortex.engine.Engine
import dev.davi.vortex.ui.components.FormatToggle
import dev.davi.vortex.ui.components.GlassCard
import dev.davi.vortex.ui.components.GlassIconButton
import dev.davi.vortex.ui.components.LogoMark
import dev.davi.vortex.ui.components.NeonButton
import dev.davi.vortex.ui.components.NeonProgress
import dev.davi.vortex.ui.components.PlatformBadge
import dev.davi.vortex.ui.components.QualityChip
import dev.davi.vortex.ui.components.ScanningRing
import dev.davi.vortex.ui.components.SectionLabel
import dev.davi.vortex.ui.components.Tag
import dev.davi.vortex.ui.theme.VX

@Composable
fun HomeScreen(
    vm: MainViewModel,
    listState: LazyListState,
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
) {
    val engine by vm.engineState.collectAsStateWithLifecycle()
    val topPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPad + 14.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "header") { Header(engine, onOpenSettings) }
        item(key = "hero") { Hero() }
        item(key = "input") { InputSection(vm) }
        item(key = "error") { ErrorBanner(vm.error) }
        item(key = "result") { ResultSection(vm) }
    }
}

@Composable
private fun Header(engine: Engine.State, onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LogoMark(Modifier.size(42.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "VORTEX",
                style = TextStyle(
                    brush = Brush.linearGradient(VX.Neon),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                ),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                EngineDot(engine)
                Spacer(Modifier.width(6.dp))
                Text(
                    when (engine) {
                        Engine.State.Loading -> "preparando motor…"
                        Engine.State.Ready -> "motor pronto"
                        is Engine.State.Failed -> "erro no motor"
                    },
                    color = VX.Muted,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                )
            }
        }
        GlassIconButton(Icons.Rounded.Tune, "Configurações", onOpenSettings)
    }
}

@Composable
private fun EngineDot(engine: Engine.State) {
    val color = when (engine) {
        Engine.State.Loading -> VX.Amber
        Engine.State.Ready -> VX.Lime
        is Engine.State.Failed -> VX.Danger
    }
    val t = rememberInfiniteTransition(label = "dot")
    val a by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Box(
        Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (engine == Engine.State.Ready) 1f else a)),
    )
}

@Composable
private fun Hero() {
    Column {
        Text("Cole o link.", color = VX.Text, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
        Text(
            "A gente cuida do resto.",
            style = TextStyle(
                brush = Brush.linearGradient(listOf(VX.Cyan, VX.Violet)),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Vídeos em MP4 até 4K e músicas em MP3 — sem anúncios.",
            color = VX.Muted,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun InputSection(vm: MainViewModel) {
    val focus = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val detected = remember(vm.url) { if (vm.url.isBlank()) null else Platform.detect(vm.url) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Link, null, tint = VX.Cyan, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = vm.url,
                    onValueChange = vm::onUrlChange,
                    singleLine = true,
                    textStyle = TextStyle(color = VX.Text, fontSize = 16.sp),
                    cursorBrush = SolidColor(VX.Cyan),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        focus.clearFocus()
                        vm.analyze()
                    }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (vm.url.isEmpty()) {
                                Text("Cole o link do vídeo aqui…", color = VX.Faint, fontSize = 16.sp)
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.width(8.dp))
                AnimatedContent(vm.url.isEmpty(), label = "paste") { empty ->
                    if (empty) {
                        GlassIconButton(
                            Icons.Rounded.ContentPaste,
                            "Colar",
                            onClick = {
                                clipboard.getText()?.text?.let { t ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    vm.onUrlChange(UrlUtils.extract(t) ?: t.trim())
                                }
                            },
                            tint = VX.Cyan,
                            size = 40.dp,
                        )
                    } else {
                        GlassIconButton(Icons.Rounded.Close, "Limpar", onClick = vm::clear, size = 40.dp)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Platform.YOUTUBE, Platform.INSTAGRAM, Platform.TIKTOK).forEach { p ->
                    PlatformBadge(p, active = detected == p, dim = detected != null && detected != p)
                }
            }
        }
        NeonButton(
            text = "ANALISAR LINK",
            icon = Icons.Rounded.AutoAwesome,
            loading = vm.analyzing,
            enabled = vm.url.isNotBlank(),
            onClick = {
                focus.clearFocus()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.analyze()
            },
        )
    }
}

@Composable
private fun ErrorBanner(error: String?) {
    // Guarda a última mensagem para ela continuar visível durante a animação de saída.
    val holder = remember { arrayOf("") }
    if (error != null) holder[0] = error
    val last = holder[0]
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(VX.Danger.copy(alpha = 0.12f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.ErrorOutline, null, tint = VX.Danger, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(last, color = VX.Text, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun ResultSection(vm: MainViewModel) {
    val state = when {
        vm.analyzing -> 1
        vm.media != null -> 2
        else -> 0
    }
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            (fadeIn(tween(320)) + scaleIn(initialScale = 0.96f, animationSpec = tween(320))) togetherWith
                fadeOut(tween(160)) using SizeTransform(clip = false)
        },
        label = "result",
    ) { s ->
        when (s) {
            1 -> ScanningCard()
            2 -> vm.media?.let { MediaCard(vm, it) }
            else -> FeatureTiles()
        }
    }
}

@Composable
private fun ScanningCard() {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScanningRing(Modifier.size(54.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Escaneando o link…", color = VX.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("Buscando todas as qualidades disponíveis", color = VX.Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        NeonProgress(-1f)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MediaCard(vm: MainViewModel, media: MediaInfo) {
    val haptic = LocalHapticFeedback.current
    GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            AsyncImage(
                model = media.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.04f)),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to VX.Bg.copy(alpha = 0.92f),
                        ),
                    ),
            )
            PlatformBadge(media.platform, Modifier.align(Alignment.TopStart).padding(12.dp))
            if (media.durationSec > 0) {
                Tag(Format.duration(media.durationSec), Modifier.align(Alignment.BottomEnd).padding(12.dp), strong = true)
            }
        }
        Column(Modifier.padding(18.dp)) {
            Text(
                media.title,
                color = VX.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 24.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (media.uploader.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(media.uploader, color = VX.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(18.dp))
            FormatToggle(vm.mode, videoEnabled = media.videoOptions.isNotEmpty(), onChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                vm.selectMode(it)
            })
            Spacer(Modifier.height(18.dp))
            SectionLabel(if (vm.mode == MediaKind.VIDEO) "RESOLUÇÃO" else "QUALIDADE DO ÁUDIO")
            Spacer(Modifier.height(10.dp))
            AnimatedContent(vm.mode, label = "options") { m ->
                val options = if (m == MediaKind.VIDEO) media.videoOptions else media.audioOptions
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    options.forEach { opt ->
                        QualityChip(opt, vm.selected?.id == opt.id) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            vm.selected = opt
                        }
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            val sel = vm.selected
            NeonButton(
                text = if (sel == null) "ESCOLHA UMA QUALIDADE" else "BAIXAR ${sel.formatLabel} • ${sel.label}",
                icon = Icons.Rounded.Download,
                enabled = sel != null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.download()
                },
            )
        }
    }
}

@Composable
private fun FeatureTiles() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("POR QUE VORTEX", Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureTile(Icons.Rounded.HighQuality, "Até 4K", "vídeo em MP4", Modifier.weight(1f))
            FeatureTile(Icons.Rounded.GraphicEq, "320 kbps", "MP3 com capa", Modifier.weight(1f))
            FeatureTile(Icons.Rounded.Block, "Zero", "anúncios", Modifier.weight(1f))
        }
        GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Share, null, tint = VX.Violet, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "Dica: no YouTube, Instagram ou TikTok, toque em Compartilhar → Vortex e o link já chega aqui.",
                    color = VX.Muted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun FeatureTile(icon: ImageVector, title: String, subtitle: String, modifier: Modifier) {
    GlassCard(modifier, corner = 20.dp, contentPadding = PaddingValues(14.dp)) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(listOf(VX.Cyan.copy(alpha = 0.25f), VX.Violet.copy(alpha = 0.35f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = VX.Text, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, color = VX.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(subtitle, color = VX.Muted, fontSize = 11.sp)
    }
}

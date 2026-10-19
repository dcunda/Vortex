package dev.davi.vortex.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.davi.vortex.ui.components.FloatingNav
import dev.davi.vortex.ui.components.GlassCard
import dev.davi.vortex.ui.components.LogoMark
import dev.davi.vortex.ui.components.NeonButton
import dev.davi.vortex.ui.components.VortexBackground
import dev.davi.vortex.ui.theme.VX

@Composable
fun VortexRoot(vm: MainViewModel) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val homeState = rememberLazyListState()
    val libraryState = rememberLazyListState()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPad = 64.dp + 16.dp + navInset

    RequestPermissions()

    Box(Modifier.fillMaxSize().background(VX.Bg)) {
        VortexBackground(Modifier.fillMaxSize())

        AnimatedContent(
            targetState = vm.tab,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (fadeIn(tween(260)) + slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 6 * dir }) togetherWith
                    (fadeOut(tween(180)) + slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { -it / 6 * dir })
            },
            label = "tabs",
        ) { tab ->
            when (tab) {
                0 -> HomeScreen(vm, homeState, bottomPad, onOpenSettings = { showSettings = true })
                else -> LibraryScreen(vm, libraryState, bottomPad)
            }
        }

        FloatingNav(
            selected = vm.tab,
            activeCount = tasks.count { it.isActive },
            onSelect = { vm.tab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navInset + 16.dp),
        )
    }

    if (showSettings) {
        SettingsSheet(vm, onDismiss = { showSettings = false })
    }
}

@Composable
private fun RequestPermissions() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    LaunchedEffect(Unit) {
        val wanted = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        val missing = wanted.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) launcher.launch(missing.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val version by vm.engineVersion.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VX.Bg2,
        contentColor = VX.Text,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Configurações", color = VX.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)

            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LogoMark(Modifier.size(38.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Motor de download", color = VX.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("yt-dlp ${version ?: "—"}", color = VX.Muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Os sites mudam com frequência. O Vortex se atualiza sozinho a cada 3 dias, mas se um download começar a falhar, atualize agora.",
                    color = VX.Muted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(16.dp))
                NeonButton(
                    text = "ATUALIZAR MOTOR",
                    icon = Icons.Rounded.SystemUpdateAlt,
                    loading = vm.updating,
                    onClick = vm::updateEngine,
                )
                vm.updateMessage?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = VX.Cyan, fontSize = 12.sp)
                }
            }

            GlassCard(Modifier.fillMaxWidth()) {
                InfoRow(Icons.Rounded.Movie, "Vídeos", "Movies/Vortex")
                Spacer(Modifier.height(14.dp))
                InfoRow(Icons.Rounded.MusicNote, "Músicas", "Music/Vortex")
            }

            Text(
                "Vortex 1.0 • sem anúncios, sem rastreadores",
                color = VX.Faint,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(listOf(VX.Cyan.copy(alpha = 0.25f), VX.Violet.copy(alpha = 0.35f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = VX.Text, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = VX.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("Salvos em $value", color = VX.Muted, fontSize = 12.sp)
        }
    }
}

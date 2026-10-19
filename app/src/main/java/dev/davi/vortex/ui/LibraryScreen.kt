package dev.davi.vortex.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.davi.vortex.data.DownloadTask
import dev.davi.vortex.data.Format
import dev.davi.vortex.data.MediaKind
import dev.davi.vortex.data.TaskStatus
import dev.davi.vortex.ui.components.GlassCard
import dev.davi.vortex.ui.components.GlassIconButton
import dev.davi.vortex.ui.components.LogoMark
import dev.davi.vortex.ui.components.NeonProgress
import dev.davi.vortex.ui.components.Tag
import dev.davi.vortex.ui.theme.VX

@Composable
fun LibraryScreen(vm: MainViewModel, listState: LazyListState, bottomPadding: Dp) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val topPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val active = tasks.count { it.isActive }
    val done = tasks.count { it.status == TaskStatus.DONE }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topPad + 18.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "title") {
            Column(Modifier.padding(bottom = 6.dp)) {
                Text("Biblioteca", color = VX.Text, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("$active em andamento • $done concluídos", color = VX.Muted, fontSize = 13.sp)
            }
        }
        if (tasks.isEmpty()) {
            item(key = "empty") { EmptyLibrary() }
        }
        items(tasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                onCancel = { vm.cancel(task.id) },
                onRetry = { vm.retry(task.id) },
                onRemove = { vm.remove(task.id) },
                onOpen = { openMedia(context, task) },
                onShare = { shareMedia(context, task) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: DownloadTask,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier.fillMaxWidth(), corner = 22.dp, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
            ) {
                AsyncImage(
                    model = task.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(VX.Cyan, VX.Violet))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (task.option.kind == MediaKind.VIDEO) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    color = VX.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 19.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(task.option.formatLabel)
                    Tag(task.option.label)
                    task.sizeBytes?.takeIf { it > 0 }?.let {
                        Tag((if (task.status == TaskStatus.DONE) "" else "~") + Format.bytes(it))
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            when (task.status) {
                TaskStatus.QUEUED, TaskStatus.RUNNING, TaskStatus.PROCESSING ->
                    GlassIconButton(Icons.Rounded.Close, "Cancelar", onCancel, size = 38.dp)
                TaskStatus.DONE ->
                    GlassIconButton(Icons.Rounded.PlayArrow, "Abrir", onOpen, tint = VX.Cyan, size = 44.dp)
                TaskStatus.FAILED, TaskStatus.CANCELED ->
                    GlassIconButton(Icons.Rounded.Refresh, "Tentar de novo", onRetry, tint = VX.Cyan, size = 38.dp)
            }
        }

        AnimatedVisibility(task.isActive) {
            Column {
                Spacer(Modifier.height(12.dp))
                NeonProgress(task.progress)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.stage,
                        color = VX.Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    val pct = if (task.progress >= 0f) "${(task.progress * 100).toInt()}%" else "…"
                    val eta = Format.eta(task.etaSec).takeIf { it.isNotEmpty() }?.let { " • $it" }.orEmpty()
                    Text(pct + eta, color = VX.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (task.status == TaskStatus.FAILED || task.status == TaskStatus.CANCELED) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.ErrorOutline,
                    null,
                    tint = if (task.status == TaskStatus.FAILED) VX.Danger else VX.Muted,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (task.status == TaskStatus.FAILED) task.error ?: "Falhou" else "Cancelado",
                    color = if (task.status == TaskStatus.FAILED) VX.Danger else VX.Muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                SmallAction("Remover", Icons.Rounded.DeleteOutline, onRemove)
            }
        }

        if (task.status == TaskStatus.DONE) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallAction("Compartilhar", Icons.Rounded.Share, onShare, Modifier.weight(1f))
                SmallAction("Remover da lista", Icons.Rounded.DeleteOutline, onRemove, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SmallAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = VX.Muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = VX.Text.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyLibrary() {
    GlassCard(Modifier.fillMaxWidth().padding(top = 24.dp), contentPadding = PaddingValues(28.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            LogoMark(Modifier.size(64.dp))
            Spacer(Modifier.height(18.dp))
            Text("Nada por aqui ainda", color = VX.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Seus downloads aparecem aqui, com progresso em tempo real — mesmo com o app minimizado.",
                color = VX.Muted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun openMedia(context: Context, task: DownloadTask) {
    val uri = task.resultUri?.let(Uri::parse) ?: return
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, task.mime ?: "*/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Não encontrei o arquivo ou um app para abri-lo.", Toast.LENGTH_SHORT).show()
    }
}

private fun shareMedia(context: Context, task: DownloadTask) {
    val uri = task.resultUri?.let(Uri::parse) ?: return
    val intent = Intent(Intent.ACTION_SEND)
        .setType(task.mime ?: "*/*")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(Intent.createChooser(intent, "Compartilhar"))
    } catch (e: Exception) {
        Toast.makeText(context, "Não foi possível compartilhar.", Toast.LENGTH_SHORT).show()
    }
}

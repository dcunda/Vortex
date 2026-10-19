package dev.davi.vortex.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.davi.vortex.MainActivity
import dev.davi.vortex.R
import dev.davi.vortex.data.DownloadTask

object Notifications {

    const val CH_PROGRESS = "downloads"
    const val CH_DONE = "done"
    const val PROGRESS_ID = 1001
    private const val ACCENT = 0xFF22E4FF.toInt()

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_PROGRESS, "Downloads em andamento", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, "Downloads concluídos", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun progress(context: Context, active: List<DownloadTask>): Notification {
        val title = when (active.size) {
            0 -> "Vortex"
            1 -> active[0].title
            else -> "Baixando ${active.size} itens"
        }
        val known = active.filter { it.progress >= 0f }
        val pct = if (known.isEmpty()) 0 else (known.map { it.progress }.average() * 100).toInt()
        return NotificationCompat.Builder(context, CH_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ACCENT)
            .setContentTitle(title)
            .setContentText(active.firstOrNull()?.stage ?: "Preparando…")
            .setProgress(100, pct, known.isEmpty())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openApp(context))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    @SuppressLint("MissingPermission")
    fun notifyDone(context: Context, task: DownloadTask) {
        if (!canPost(context)) return
        val uri = task.resultUri?.let(Uri::parse)
        val intent = if (uri != null) {
            PendingIntent.getActivity(
                context,
                task.id.hashCode(),
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, task.mime ?: "*/*")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        } else {
            openApp(context)
        }
        val n = NotificationCompat.Builder(context, CH_DONE)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ACCENT)
            .setContentTitle("Download concluído")
            .setContentText("${task.option.formatLabel} ${task.option.label} • ${task.title}")
            .setAutoCancel(true)
            .setContentIntent(intent)
            .build()
        NotificationManagerCompat.from(context).notify(task.id.hashCode(), n)
    }

    @SuppressLint("MissingPermission")
    fun notifyFailed(context: Context, task: DownloadTask) {
        if (!canPost(context)) return
        val n = NotificationCompat.Builder(context, CH_DONE)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFFF5C7A.toInt())
            .setContentTitle("Falha no download")
            .setContentText(task.error ?: task.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${task.title}\n${task.error.orEmpty()}"))
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        NotificationManagerCompat.from(context).notify(task.id.hashCode(), n)
    }

    @SuppressLint("MissingPermission")
    fun update(context: Context, notification: Notification) {
        if (!canPost(context)) return
        NotificationManagerCompat.from(context).notify(PROGRESS_ID, notification)
    }
}

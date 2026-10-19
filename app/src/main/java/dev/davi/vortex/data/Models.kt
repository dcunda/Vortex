package dev.davi.vortex.data

import java.util.Locale
import kotlin.math.roundToInt

enum class MediaKind { VIDEO, AUDIO }

enum class Platform(val label: String, val color: Long) {
    YOUTUBE("YouTube", 0xFFFF3355),
    INSTAGRAM("Instagram", 0xFFE1306C),
    TIKTOK("TikTok", 0xFF25F4EE),
    OTHER("Web", 0xFF8A93B2);

    companion object {
        fun detect(url: String): Platform {
            val u = url.lowercase()
            return when {
                "youtube.com" in u || "youtu.be" in u -> YOUTUBE
                "instagram.com" in u || "instagr.am" in u -> INSTAGRAM
                "tiktok.com" in u -> TIKTOK
                else -> OTHER
            }
        }
    }
}

data class QualityOption(
    val id: String,
    val kind: MediaKind,
    val label: String,
    val detail: String,
    val sizeBytes: Long? = null,
    /** Altura máxima usada no filtro do yt-dlp (0 = melhor disponível). */
    val height: Int = 0,
    val audioBitrate: Int = 0,
    /** "mp3" ou "best" (áudio original, sem reconversão). */
    val audioFormat: String = "mp3",
) {
    val formatLabel: String
        get() = when {
            kind == MediaKind.VIDEO -> "MP4"
            audioFormat == "mp3" -> "MP3"
            else -> "ÁUDIO"
        }
}

data class MediaInfo(
    val url: String,
    val title: String,
    val uploader: String,
    val durationSec: Int,
    val thumbnail: String?,
    val platform: Platform,
    val videoOptions: List<QualityOption>,
    val audioOptions: List<QualityOption>,
)

enum class TaskStatus { QUEUED, RUNNING, PROCESSING, DONE, FAILED, CANCELED }

data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val thumbnail: String?,
    val platform: Platform,
    val option: QualityOption,
    val status: TaskStatus = TaskStatus.QUEUED,
    /** 0..1, ou negativo quando indeterminado. */
    val progress: Float = -1f,
    val etaSec: Long = -1,
    val stage: String = "Na fila…",
    val error: String? = null,
    val resultUri: String? = null,
    val mime: String? = null,
    val sizeBytes: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isActive: Boolean
        get() = status == TaskStatus.QUEUED || status == TaskStatus.RUNNING || status == TaskStatus.PROCESSING
}

object UrlUtils {
    private val regex = Regex("""https?://[^\s<>"']+""")

    fun extract(text: String): String? =
        regex.find(text)?.value?.trimEnd('.', ',', ')', ']', '!', '?')
}

object Errors {
    fun friendly(e: Throwable): String {
        val msg = (e.message ?: e.toString())
        val low = msg.lowercase()
        return when {
            "unsupported url" in low -> "Esse link não é suportado."
            "sign in to confirm" in low || "login required" in low || "log in" in low ||
                "private" in low || "age" in low && "restrict" in low ->
                "Esse conteúdo exige login (é privado ou tem restrição de idade)."
            "unable to resolve host" in low || "network is unreachable" in low ||
                "timed out" in low || "connection" in low && "failed" in low ->
                "Sem conexão com a internet. Verifique sua rede."
            "http error 403" in low || "forbidden" in low ->
                "O site recusou o acesso. Atualize o motor em Configurações e tente de novo."
            "http error 429" in low || "too many requests" in low ->
                "Muitas requisições seguidas. Espere alguns minutos e tente de novo."
            "video unavailable" in low || "not available" in low ->
                "Esse vídeo não está disponível."
            "no space" in low -> "Sem espaço no armazenamento do celular."
            else -> msg.lineSequence()
                .map { it.removePrefix("ERROR:").trim() }
                .firstOrNull { it.isNotBlank() }
                ?.take(180)
                ?: "Algo deu errado. Tente novamente."
        }
    }
}

object Format {
    private val br: Locale = Locale.forLanguageTag("pt-BR")

    fun bytes(b: Long): String {
        if (b <= 0) return "—"
        val units = arrayOf("B", "KB", "MB", "GB")
        var v = b.toDouble()
        var i = 0
        while (v >= 1024 && i < units.lastIndex) {
            v /= 1024; i++
        }
        return if (i >= 2) String.format(br, "%.1f %s", v, units[i]) else "${v.roundToInt()} ${units[i]}"
    }

    fun duration(sec: Int): String {
        if (sec <= 0) return ""
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) String.format(br, "%d:%02d:%02d", h, m, s) else String.format(br, "%d:%02d", m, s)
    }

    fun eta(sec: Long): String = when {
        sec <= 0 -> ""
        sec < 60 -> "${sec}s"
        sec < 3600 -> "${sec / 60}min ${sec % 60}s"
        else -> "${sec / 3600}h ${(sec % 3600) / 60}min"
    }
}

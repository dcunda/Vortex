package dev.davi.vortex.data

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lê as informações do link (título, capa, formatos) e monta
 * as opções de qualidade mostradas na tela.
 */
object MediaAnalyzer {

    private val standard = intArrayOf(4320, 2160, 1440, 1080, 720, 480, 360, 240, 144)

    suspend fun analyze(url: String): MediaInfo = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url).apply { addOption("--no-playlist") }
        val info = YoutubeDL.getInstance().getInfo(request)

        val duration = num(info.duration).toInt()
        val formats = info.formats?.toList().orEmpty()

        // Melhor faixa de áudio separada (para somar no tamanho dos vídeos sem áudio).
        val audioOnly = formats.filter { it.vcodec == "none" && it.acodec != null && it.acodec != "none" }
        val bestAudio = audioOnly.maxByOrNull { num(it.abr).takeIf { a -> a > 0 } ?: num(it.tbr) }
        val bestAudioSize = bestAudio?.let {
            sizeOf(it.fileSize, it.fileSizeApproximate, num(it.tbr).takeIf { t -> t > 0 } ?: num(it.abr), duration)
        } ?: 0L

        // Agrupa os formatos de vídeo por resolução "padrão" (1080p, 720p…).
        data class Bucket(val maxHeight: Int, val size: Long)
        val buckets = HashMap<Int, Bucket>()
        formats.filter { it.vcodec != "none" && num(it.height) > 0 }.forEach { f ->
            val h = num(f.height).toInt()
            val w = num(f.width).toInt()
            val shortSide = if (w > 0) minOf(w, h) else h
            val p = standard.firstOrNull { shortSide >= it * 0.9 } ?: shortSide
            val own = sizeOf(f.fileSize, f.fileSizeApproximate, num(f.tbr), duration) ?: 0L
            val total = own + if (f.acodec == "none") bestAudioSize else 0L
            val cur = buckets[p]
            buckets[p] = Bucket(
                maxHeight = maxOf(cur?.maxHeight ?: 0, h),
                size = maxOf(cur?.size ?: 0L, total),
            )
        }

        val videoOptions = buckets.entries
            .sortedByDescending { it.key }
            .map { (p, b) ->
                QualityOption(
                    id = "v$p",
                    kind = MediaKind.VIDEO,
                    label = videoLabel(p),
                    detail = videoDetail(p),
                    sizeBytes = b.size.takeIf { it > 0 },
                    height = b.maxHeight,
                )
            }
            .ifEmpty {
                listOf(QualityOption("vbest", MediaKind.VIDEO, "Melhor", "automático"))
            }

        val audioOptions = listOf(
            320 to "Máxima", 256 to "Alta", 192 to "Padrão", 128 to "Leve",
        ).map { (kbps, detail) ->
            QualityOption(
                id = "a$kbps",
                kind = MediaKind.AUDIO,
                label = "$kbps kbps",
                detail = detail,
                sizeBytes = if (duration > 0) kbps * 1000L / 8 * duration else null,
                audioBitrate = kbps,
                audioFormat = "mp3",
            )
        } + QualityOption(
            id = "aorig",
            kind = MediaKind.AUDIO,
            label = "Original",
            detail = "sem conversão",
            sizeBytes = bestAudioSize.takeIf { it > 0 },
            audioFormat = "best",
        )

        MediaInfo(
            url = url,
            title = info.title?.takeIf { it.isNotBlank() } ?: "Sem título",
            uploader = info.uploader.orEmpty(),
            durationSec = duration,
            thumbnail = info.thumbnail,
            platform = Platform.detect(url),
            videoOptions = videoOptions,
            audioOptions = audioOptions,
        )
    }

    private fun sizeOf(fileSize: Number?, approx: Number?, kbps: Double, duration: Int): Long? {
        val s = num(fileSize).toLong()
        if (s > 0) return s
        val a = num(approx).toLong()
        if (a > 0) return a
        if (kbps > 0 && duration > 0) return (kbps * 1000 / 8 * duration).toLong()
        return null
    }

    private fun num(v: Number?): Double = v?.toDouble() ?: 0.0

    private fun videoLabel(p: Int) = when {
        p >= 4320 -> "8K"
        p >= 2160 -> "4K"
        else -> "${p}p"
    }

    private fun videoDetail(p: Int) = when {
        p >= 4320 -> "Ultra HD"
        p >= 2160 -> "Ultra HD"
        p >= 1440 -> "Quad HD"
        p >= 1080 -> "Full HD"
        p >= 720 -> "HD"
        p >= 480 -> "SD"
        else -> "Leve"
    }
}

package dev.davi.vortex.data

import android.app.Application
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dev.davi.vortex.engine.Engine
import dev.davi.vortex.service.DownloadService
import dev.davi.vortex.service.Notifications
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Fila de downloads. Roda até 2 ao mesmo tempo, publica o progresso
 * num StateFlow (a UI e a notificação observam) e salva o resultado na galeria.
 */
object DownloadRepository {

    private lateinit var app: Application
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(2)
    private val jobs = ConcurrentHashMap<String, Job>()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val junkExt = setOf("part", "ytdl", "jpg", "jpeg", "png", "webp", "json", "tmp", "temp", "vtt", "srt")
    private val intermediate = Regex("""\.f\d+[a-z0-9-]*\.""")

    fun init(app: Application) {
        this.app = app
        _tasks.value = HistoryStore.load(app)
        File(app.cacheDir, "dl").deleteRecursively()
    }

    fun enqueue(media: MediaInfo, option: QualityOption) {
        val task = DownloadTask(
            id = UUID.randomUUID().toString().replace("-", "").take(12),
            url = media.url,
            title = media.title,
            thumbnail = media.thumbnail,
            platform = media.platform,
            option = option,
            sizeBytes = option.sizeBytes,
        )
        _tasks.update { listOf(task) + it }
        start(task.id)
    }

    fun retry(id: String) {
        val t = find(id) ?: return
        if (t.isActive) return
        patch(id) {
            it.copy(status = TaskStatus.QUEUED, progress = -1f, etaSec = -1, stage = "Na fila…", error = null)
        }
        start(id)
    }

    fun cancel(id: String) {
        runCatching { YoutubeDL.getInstance().destroyProcessById(id) }
        jobs.remove(id)?.cancel()
        patch(id) { if (it.isActive) it.copy(status = TaskStatus.CANCELED, stage = "Cancelado", progress = -1f) else it }
        File(app.cacheDir, "dl/$id").deleteRecursively()
        persist()
    }

    fun remove(id: String) {
        if (find(id)?.isActive == true) cancel(id)
        _tasks.update { list -> list.filterNot { it.id == id } }
        persist()
    }

    private fun start(id: String) {
        DownloadService.start(app)
        jobs[id] = scope.launch { run(id) }
    }

    private suspend fun run(id: String) {
        gate.withPermit {
            val task = find(id)?.takeIf { it.status == TaskStatus.QUEUED } ?: return@withPermit
            val dir = File(app.cacheDir, "dl/$id").apply { deleteRecursively(); mkdirs() }
            try {
                patch(id) { it.copy(status = TaskStatus.RUNNING, stage = "Conectando…", progress = -1f) }
                Engine.awaitReady()

                var parts = 0
                var lastEmit = 0L
                var stage = "Conectando…"

                val failure: Throwable? = try {
                    YoutubeDL.getInstance().execute(buildRequest(task, dir), id) { progress, eta, line ->
                        if ("[download] Destination:" in line) parts++
                        val next = when {
                            "[Merger]" in line -> "Unindo vídeo e áudio…"
                            "[ExtractAudio]" in line ->
                                if (task.option.audioFormat == "mp3") "Convertendo para MP3…" else "Extraindo áudio…"
                            "[VideoRemuxer]" in line || "[VideoConvertor]" in line -> "Finalizando MP4…"
                            "[EmbedThumbnail]" in line -> "Aplicando capa…"
                            "[Metadata]" in line -> "Gravando informações…"
                            "[download]" in line -> when {
                                task.option.kind == MediaKind.AUDIO -> "Baixando áudio…"
                                parts >= 2 -> "Baixando faixa de áudio…"
                                else -> "Baixando vídeo…"
                            }
                            else -> null
                        }
                        if (next != null) stage = next
                        val postProcessing = next != null && "[download]" !in line
                        val now = System.currentTimeMillis()
                        if (postProcessing || now - lastEmit > 120) {
                            lastEmit = now
                            patch(id) {
                                it.copy(
                                    stage = stage,
                                    progress = when {
                                        postProcessing -> -1f
                                        progress >= 0f -> (progress / 100f).coerceIn(0f, 1f)
                                        else -> it.progress
                                    },
                                    etaSec = if (postProcessing) -1 else eta,
                                )
                            }
                        }
                    }
                    null
                } catch (e: Throwable) {
                    e
                }

                currentCoroutineContext().ensureActive()

                // Às vezes o yt-dlp reclama de algo secundário (ex.: capa) mas o arquivo saiu certo.
                val output = findOutput(dir)
                    ?: throw (failure ?: IllegalStateException("O arquivo final não foi gerado."))

                patch(id) { it.copy(status = TaskStatus.PROCESSING, stage = "Salvando…", progress = -1f, etaSec = -1) }
                val saved = MediaSaver.save(app, output, task.option.kind)
                patch(id) {
                    it.copy(
                        status = TaskStatus.DONE,
                        stage = "Concluído",
                        progress = 1f,
                        resultUri = saved.uri.toString(),
                        mime = saved.mime,
                        sizeBytes = output.length(),
                    )
                }
                find(id)?.let { Notifications.notifyDone(app, it) }
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                if (currentCoroutineContext().isActive) {
                    patch(id) {
                        it.copy(status = TaskStatus.FAILED, stage = "Falhou", error = Errors.friendly(e), progress = -1f)
                    }
                    find(id)?.let { Notifications.notifyFailed(app, it) }
                }
            } finally {
                dir.deleteRecursively()
                jobs.remove(id)
                persist()
            }
        }
    }

    private fun buildRequest(task: DownloadTask, dir: File) = YoutubeDLRequest(task.url).apply {
        val o = task.option
        addOption("--no-playlist")
        addOption("--no-mtime")
        addOption("-o", File(dir, "%(title).80B.%(ext)s").absolutePath)
        addOption("--embed-metadata")
        if (o.kind == MediaKind.VIDEO) {
            val h = o.height
            val selector = if (h > 0) {
                "bv*[height<=$h]+ba/b[height<=$h]/bv*+ba/b"
            } else {
                "bv*+ba/b"
            }
            addOption("-f", selector)
            // Maior resolução primeiro; dentro dela prefere H.264 + AAC (toca em qualquer celular).
            addOption("-S", "res,vcodec:h264,acodec:m4a")
            addOption("--merge-output-format", "mp4")
            addOption("--remux-video", "mp4")
        } else {
            addOption("-f", "ba/b")
            addOption("-x")
            if (o.audioFormat == "mp3") {
                addOption("--audio-format", "mp3")
                addOption("--audio-quality", "${o.audioBitrate}K")
                addOption("--embed-thumbnail")
                addOption("--convert-thumbnails", "jpg")
            } else {
                addOption("--audio-format", "best")
            }
        }
    }

    private fun findOutput(dir: File): File? = dir.walkTopDown()
        .filter { it.isFile }
        .filter { it.extension.lowercase() !in junkExt }
        .filter { !intermediate.containsMatchIn(it.name) && ".temp." !in it.name && ".part" !in it.name }
        .maxByOrNull { it.length() }

    private fun find(id: String) = _tasks.value.firstOrNull { it.id == id }

    private fun patch(id: String, transform: (DownloadTask) -> DownloadTask) {
        _tasks.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    private fun persist() {
        val snapshot = _tasks.value
        scope.launch { HistoryStore.save(app, snapshot) }
    }
}

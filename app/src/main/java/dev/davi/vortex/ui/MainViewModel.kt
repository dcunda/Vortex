package dev.davi.vortex.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.davi.vortex.data.DownloadRepository
import dev.davi.vortex.data.Errors
import dev.davi.vortex.data.MediaAnalyzer
import dev.davi.vortex.data.MediaInfo
import dev.davi.vortex.data.MediaKind
import dev.davi.vortex.data.QualityOption
import dev.davi.vortex.data.UrlUtils
import dev.davi.vortex.engine.Engine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    var url by mutableStateOf("")
        private set
    var analyzing by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var media by mutableStateOf<MediaInfo?>(null)
        private set
    var mode by mutableStateOf(MediaKind.VIDEO)
        private set
    var selected by mutableStateOf<QualityOption?>(null)
    var tab by mutableIntStateOf(0)
    var updating by mutableStateOf(false)
        private set
    var updateMessage by mutableStateOf<String?>(null)
        private set

    val tasks = DownloadRepository.tasks
    val engineState = Engine.state
    val engineVersion = Engine.version

    private var analyzeJob: Job? = null

    fun onUrlChange(value: String) {
        url = value
        error = null
    }

    fun onSharedText(text: String) {
        url = UrlUtils.extract(text) ?: text.trim()
        tab = 0
        analyze()
    }

    fun analyze() {
        val link = UrlUtils.extract(url)
        if (link == null) {
            error = "Cole um link válido do YouTube, Instagram ou TikTok."
            return
        }
        analyzeJob?.cancel()
        analyzing = true
        error = null
        media = null
        analyzeJob = viewModelScope.launch {
            try {
                Engine.awaitReady()
                val info = MediaAnalyzer.analyze(link)
                media = info
                mode = MediaKind.VIDEO
                selected = defaultOption(info, MediaKind.VIDEO)
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                error = Errors.friendly(e)
            } finally {
                analyzing = false
            }
        }
    }

    fun selectMode(kind: MediaKind) {
        if (kind == mode) return
        mode = kind
        media?.let { selected = defaultOption(it, kind) }
    }

    fun download() {
        val m = media ?: return
        val opt = selected ?: return
        DownloadRepository.enqueue(m, opt)
        tab = 1
    }

    fun clear() {
        analyzeJob?.cancel()
        analyzing = false
        url = ""
        error = null
        media = null
        selected = null
    }

    fun cancel(id: String) = DownloadRepository.cancel(id)
    fun retry(id: String) = DownloadRepository.retry(id)
    fun remove(id: String) = DownloadRepository.remove(id)

    fun updateEngine() {
        if (updating) return
        updating = true
        updateMessage = null
        viewModelScope.launch {
            updateMessage = try {
                Engine.update(getApplication())
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                "Não foi possível atualizar: ${Errors.friendly(e)}"
            } finally {
                updating = false
            }
        }
    }

    private fun defaultOption(info: MediaInfo, kind: MediaKind): QualityOption? =
        if (kind == MediaKind.VIDEO) {
            info.videoOptions.firstOrNull { it.label == "1080p" } ?: info.videoOptions.firstOrNull()
        } else {
            info.audioOptions.firstOrNull()
        }
}

package dev.davi.vortex.engine

import android.app.Application
import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Inicializa e mantém o yt-dlp (Python embutido) e o ffmpeg.
 * Atualiza o yt-dlp sozinho a cada 3 dias, porque o YouTube muda com frequência.
 */
object Engine {

    sealed interface State {
        data object Loading : State
        data object Ready : State
        data class Failed(val message: String) : State
    }

    private const val PREFS = "engine"
    private const val KEY_LAST_UPDATE = "last_update"
    private const val AUTO_UPDATE_INTERVAL = 3L * 24 * 60 * 60 * 1000

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ready = CompletableDeferred<Unit>()
    private val updateLock = Mutex()

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _version = MutableStateFlow<String?>(null)
    val version: StateFlow<String?> = _version.asStateFlow()

    fun init(app: Application) {
        scope.launch {
            try {
                YoutubeDL.getInstance().init(app)
                FFmpeg.getInstance().init(app)
                _version.value = readVersion(app)
                _state.value = State.Ready
                ready.complete(Unit)
            } catch (e: Throwable) {
                _state.value = State.Failed(e.message ?: "Falha ao iniciar o motor")
                ready.completeExceptionally(e)
                return@launch
            }
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (System.currentTimeMillis() - prefs.getLong(KEY_LAST_UPDATE, 0L) > AUTO_UPDATE_INTERVAL) {
                runCatching { update(app) }
            }
        }
    }

    suspend fun awaitReady() = ready.await()

    /** Atualiza o yt-dlp. Retorna uma mensagem para mostrar ao usuário. */
    suspend fun update(context: Context): String = updateLock.withLock {
        withContext(Dispatchers.IO) {
            awaitReady()
            val app = context.applicationContext
            val status = YoutubeDL.getInstance().updateYoutubeDL(app)
            _version.value = readVersion(app)
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()
            if (status.toString().contains("UP_TO_DATE")) {
                "O motor já está na versão mais recente."
            } else {
                "Motor atualizado para ${_version.value ?: "a versão mais recente"}."
            }
        }
    }

    private fun readVersion(context: Context): String? =
        runCatching { YoutubeDL.getInstance().version(context) }.getOrNull()
}

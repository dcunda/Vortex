package dev.davi.vortex.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Copia o arquivo final para a galeria/músicas do celular:
 *  - vídeos → Movies/Vortex
 *  - áudios → Music/Vortex
 */
object MediaSaver {

    private const val FOLDER = "Vortex"

    data class Saved(val uri: Uri, val mime: String, val displayName: String)

    suspend fun save(context: Context, file: File, kind: MediaKind): Saved = withContext(Dispatchers.IO) {
        val mime = mimeFor(file, kind)
        val isVideo = mime.startsWith("video")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveScoped(context, file, mime, isVideo)
        } else {
            saveLegacy(context, file, mime, isVideo)
        }
    }

    private fun saveScoped(context: Context, file: File, mime: String, isVideo: Boolean): Saved {
        val resolver = context.contentResolver
        val collection = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val relative = (if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC) + "/" + FOLDER
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relative)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values)
            ?: throw IllegalStateException("Não foi possível criar o arquivo na galeria.")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out, 1 shl 16) }
            } ?: throw IllegalStateException("Não foi possível gravar o arquivo.")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
        return Saved(uri, mime, file.name)
    }

    @Suppress("DEPRECATION")
    private suspend fun saveLegacy(context: Context, file: File, mime: String, isVideo: Boolean): Saved {
        val base = Environment.getExternalStoragePublicDirectory(
            if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC,
        )
        val dir = File(base, FOLDER).apply { mkdirs() }
        var target = File(dir, file.name)
        var n = 1
        while (target.exists()) {
            target = File(dir, "${file.nameWithoutExtension} ($n).${file.extension}")
            n++
        }
        file.copyTo(target)
        val uri = suspendCancellableCoroutine<Uri?> { cont ->
            MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(mime)) { _, scanned ->
                if (cont.isActive) cont.resume(scanned)
            }
        } ?: Uri.fromFile(target)
        return Saved(uri, mime, target.name)
    }

    private fun mimeFor(file: File, kind: MediaKind): String {
        val ext = file.extension.lowercase()
        return when (ext) {
            "mp4", "m4v" -> "video/mp4"
            "webm" -> if (kind == MediaKind.VIDEO) "video/webm" else "audio/webm"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/mp4"
            "opus", "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
                ?: if (kind == MediaKind.VIDEO) "video/mp4" else "audio/mpeg"
        }
    }
}

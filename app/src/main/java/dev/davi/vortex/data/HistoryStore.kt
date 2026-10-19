package dev.davi.vortex.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Guarda a lista de downloads concluídos entre aberturas do app. */
object HistoryStore {

    private const val PREFS = "history"
    private const val KEY = "items"
    private const val LIMIT = 300

    fun load(context: Context): List<DownloadTask> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i -> runCatching { fromJson(arr.getJSONObject(i)) }.getOrNull() }
    }.getOrDefault(emptyList())

    fun save(context: Context, tasks: List<DownloadTask>) {
        val arr = JSONArray()
        tasks.filter { it.status == TaskStatus.DONE }.take(LIMIT).forEach { arr.put(toJson(it)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    private fun toJson(t: DownloadTask) = JSONObject().apply {
        put("id", t.id)
        put("url", t.url)
        put("title", t.title)
        put("thumbnail", t.thumbnail)
        put("platform", t.platform.name)
        put("resultUri", t.resultUri)
        put("mime", t.mime)
        put("sizeBytes", t.sizeBytes ?: 0L)
        put("createdAt", t.createdAt)
        put("option", JSONObject().apply {
            put("id", t.option.id)
            put("kind", t.option.kind.name)
            put("label", t.option.label)
            put("detail", t.option.detail)
            put("height", t.option.height)
            put("audioBitrate", t.option.audioBitrate)
            put("audioFormat", t.option.audioFormat)
        })
    }

    private fun fromJson(o: JSONObject): DownloadTask {
        val opt = o.getJSONObject("option")
        return DownloadTask(
            id = o.getString("id"),
            url = o.getString("url"),
            title = o.getString("title"),
            thumbnail = o.str("thumbnail"),
            platform = runCatching { Platform.valueOf(o.getString("platform")) }.getOrDefault(Platform.OTHER),
            option = QualityOption(
                id = opt.getString("id"),
                kind = MediaKind.valueOf(opt.getString("kind")),
                label = opt.getString("label"),
                detail = opt.optString("detail"),
                height = opt.optInt("height"),
                audioBitrate = opt.optInt("audioBitrate"),
                audioFormat = opt.optString("audioFormat", "mp3"),
            ),
            status = TaskStatus.DONE,
            progress = 1f,
            stage = "Concluído",
            resultUri = o.str("resultUri"),
            mime = o.str("mime"),
            sizeBytes = o.optLong("sizeBytes").takeIf { it > 0 },
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        )
    }

    private fun JSONObject.str(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}

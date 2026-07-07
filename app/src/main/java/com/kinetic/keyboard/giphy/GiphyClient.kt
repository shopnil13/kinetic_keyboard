package com.kinetic.keyboard.giphy

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One GIF/sticker result: small animated preview for the grid, full asset for insertion. */
data class GiphyItem(
    val id: String,
    val previewUrl: String,
    val gifUrl: String,
    val previewWidth: Int,
    val previewHeight: Int,
)

/** What the media panel is showing. */
enum class MediaKind(val endpoint: String) {
    GIF("gifs"),
    STICKER("stickers"),
}

/**
 * P5.10: minimal GIPHY REST client (api.giphy.com/v1) — search + trending for gifs and
 * stickers. The ONLY network code in the app besides preview/asset fetches; see PRIVACY.md.
 * JSON decoding is separated into [parse] so it unit-tests without network.
 */
class GiphyClient(private val apiKey: String) {

    val hasKey: Boolean get() = apiKey.isNotBlank()

    /** Blank [query] → trending. Call on Dispatchers.IO. */
    fun fetch(kind: MediaKind, query: String, limit: Int = 24): List<GiphyItem> {
        val base = "https://api.giphy.com/v1/${kind.endpoint}"
        val url = if (query.isBlank()) {
            "$base/trending?api_key=$apiKey&limit=$limit&rating=g"
        } else {
            "$base/search?api_key=$apiKey&q=${URLEncoder.encode(query, "UTF-8")}&limit=$limit&rating=g"
        }
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            parse(conn.inputStream.bufferedReader().readText())
        } finally {
            conn.disconnect()
        }
    }

    /** Download the full asset for [item] into [dir]; returns null on failure. */
    fun download(item: GiphyItem, dir: File): File? = runCatching {
        dir.mkdirs()
        val file = File(dir, "${item.id}.gif")
        if (!file.isFile || file.length() == 0L) {
            val conn = URL(item.gifUrl).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 8000
                conn.readTimeout = 15000
                conn.inputStream.use { input -> file.outputStream().use(input::copyTo) }
            } finally {
                conn.disconnect()
            }
        }
        file.takeIf { it.length() > 0L }
    }.getOrNull()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(body: String): List<GiphyItem> =
            json.decodeFromString<SearchResponse>(body).data.mapNotNull { gif ->
                val preview = gif.images.fixedWidth ?: return@mapNotNull null
                val full = gif.images.original ?: preview
                GiphyItem(
                    id = gif.id,
                    // Animated WebP previews are much smaller than GIF where offered.
                    previewUrl = preview.webp ?: preview.url ?: return@mapNotNull null,
                    gifUrl = full.url ?: return@mapNotNull null,
                    previewWidth = preview.width?.toIntOrNull() ?: 200,
                    previewHeight = preview.height?.toIntOrNull() ?: 200,
                )
            }
    }

    @Serializable
    private data class SearchResponse(val data: List<Gif> = emptyList())

    @Serializable
    private data class Gif(val id: String, val images: Images = Images())

    @Serializable
    private data class Images(
        @kotlinx.serialization.SerialName("fixed_width") val fixedWidth: Rendition? = null,
        val original: Rendition? = null,
    )

    @Serializable
    private data class Rendition(
        val url: String? = null,
        val webp: String? = null,
        val width: String? = null,
        val height: String? = null,
    )
}

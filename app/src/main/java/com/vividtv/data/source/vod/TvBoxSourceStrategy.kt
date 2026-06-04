package com.vividtv.data.source.vod

import com.vividtv.data.model.MediaItem
import com.vividtv.data.model.MediaRow
import com.vividtv.data.model.MediaSourceType
import com.vividtv.data.model.RowType
import com.vividtv.data.model.StreamResult
import com.vividtv.data.source.SourceStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TVBox 标准 JSON 点播接口适配器
 * 对接 ffzy 资源站格式：http://cj.ffzyapi.com/api.php/provide/vod
 */
@Singleton
class TvBoxSourceStrategy @Inject constructor() : SourceStrategy {

    override val sourceId: String = "tvbox_ffzy"
    private val baseUrl = "http://cj.ffzyapi.com/api.php/provide/vod"
    private val json = Json { ignoreUnknownKeys = true }

    override fun getSourceType(): MediaSourceType = MediaSourceType.VOD

    override suspend fun fetchHomeRows(): Result<List<MediaRow>> = runCatching {
        // 分类列表
        val categories = fetchCategories()
        val rows = mutableListOf<MediaRow>()

        // 首页推荐
        val latest = fetchByType(1, 12) // 电影
        if (latest.isNotEmpty()) {
            rows.add(MediaRow("🎬 热门电影", latest, RowType.RECOMMENDED))
        }

        val series = fetchByType(2, 12) // 电视剧
        if (series.isNotEmpty()) {
            rows.add(MediaRow("📺 热门电视剧", series, RowType.LATEST))
        }

        val variety = fetchByType(3, 12) // 综艺
        if (variety.isNotEmpty()) {
            rows.add(MediaRow("🎭 综艺", variety))
        }

        val anime = fetchByType(4, 12) // 动漫
        if (anime.isNotEmpty()) {
            rows.add(MediaRow("📽️ 动漫", anime))
        }

        rows
    }

    override suspend fun search(query: String): Result<List<MediaItem>> = runCatching {
        val url = "$baseUrl?ac=detail&wd=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val response = URL(url).readText()
        val result = json.decodeFromString<TvBoxResponse>(response)
        result.list.map { it.toMediaItem() }
    }

    override suspend fun resolveStream(mediaItem: MediaItem): Result<StreamResult> = runCatching {
        // TVBox 源的播放地址已经包含在 streamUrls 中
        val url = mediaItem.streamUrls.values.firstOrNull() ?: mediaItem.id
        StreamResult(
            streamUrl = url,
            isHls = url.contains(".m3u8"),
            preferredQuality = "auto",
        )
    }

    override suspend fun isAvailable(): Boolean = runCatching {
        val response = URL("$baseUrl?ac=list").readText()
        response.contains("\"code\":1")
    }.getOrDefault(false)

    private suspend fun fetchCategories(): List<TvBoxCategory> {
        val response = URL("$baseUrl?ac=list").readText()
        val result = json.decodeFromString<TvBoxResponse>(response)
        return result.classes
    }

    private suspend fun fetchByType(typeId: Int, limit: Int): List<MediaItem> {
        val url = "$baseUrl?ac=detail&t=$typeId&pg=1&pagesize=$limit"
        val response = URL(url).readText()
        val result = json.decodeFromString<TvBoxResponse>(response)
        return result.list.map { it.toMediaItem() }
    }

    // ── TVBox JSON 响应格式 ──

    @Serializable
    data class TvBoxResponse(
        val code: Int = 0,
        val msg: String = "",
        val page: String = "1",
        val total: Int = 0,
        val limit: String = "20",
        val list: List<TvBoxVod> = emptyList(),
        val `class`: List<TvBoxCategory> = emptyList(),
    )

    @Serializable
    data class TvBoxCategory(
        val type_id: Int = 0,
        val type_name: String = "",
        val type_pid: Int = 0,
    )

    @Serializable
    data class TvBoxVod(
        val vod_id: Long = 0,
        val type_id: Int = 0,
        val vod_name: String = "",
        val vod_sub: String = "",
        val vod_en: String = "",
        val vod_status: Int = 0,
        val vod_letter: String = "",
        val vod_class: String = "",
        val vod_pic: String = "",
        val vod_actor: String = "",
        val vod_director: String = "",
        val vod_content: String = "",
        val vod_play_url: String = "",
        val vod_pic_thumb: String = "",
        val vod_year: String = "",
        val vod_score: String = "0",
        val type_name: String = "",
        val vod_remarks: String = "",
    ) {
        fun toMediaItem(): MediaItem {
            val playUrl = vod_play_url
                .split("#")
                .firstOrNull()
                ?.split("\$")
                ?.getOrNull(1)
                ?: ""

            return MediaItem(
                id = vod_id.toString(),
                title = vod_name,
                description = vod_content.take(200),
                posterUrl = vod_pic,
                year = vod_year.toIntOrNull() ?: 0,
                rating = vod_score.toFloatOrNull() ?: 0f,
                category = type_name,
                genres = vod_class.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                sourceType = MediaSourceType.VOD,
                streamUrls = parsePlayUrls(vod_play_url),
            )
        }
    }

    companion object {
        /** 解析 TVBox 播放地址格式 "集数\$url#集数\$url" */
        private fun parsePlayUrls(playUrl: String): Map<String, String> {
            if (playUrl.isBlank()) return emptyMap()
            val urls = mutableMapOf<String, String>()
            playUrl.split("#").forEach { segment ->
                val parts = segment.split("\$")
                if (parts.size == 2) {
                    urls[parts[0].trim()] = parts[1].trim()
                }
            }
            return urls
        }
    }
}

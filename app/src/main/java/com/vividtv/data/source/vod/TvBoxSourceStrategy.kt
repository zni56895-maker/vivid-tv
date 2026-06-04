package com.vividtv.data.source.vod

import com.vividtv.data.model.MediaItem
import com.vividtv.data.model.MediaRow
import com.vividtv.data.model.MediaSourceType
import com.vividtv.data.model.RowType
import com.vividtv.data.model.StreamResult
import com.vividtv.data.source.SourceStrategy
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 多 TVBox 源聚合器 — 同时对接多个资源站
 * 可用于获取大量电影和电视剧数据
 */
@Singleton
class TvBoxSourceStrategy @Inject constructor() : SourceStrategy {

    override val sourceId: String = "tvbox_aggregator"

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /** 多个 TVBox 标准资源站 */
    private val sourceApis = listOf(
        TvBoxSource("ffzy", "http://cj.ffzyapi.com/api.php/provide/vod"),
        TvBoxSource("1080zy", "https://api.1080zyapi.com/api.php/provide/vod"),
        TvBoxSource("80sjsc", "http://cj.80sjsc.com/api.php/provide/vod"),
        // 以下为可选源，按需取消注释
        // TvBoxSource("tianyi", "https://api.tianyi.com/api.php/provide/vod"),
        // TvBoxSource("xunlei", "http://cj.xunleicdn.com/api.php/provide/vod"),
    )

    private val categoryMap = mapOf(
        1 to "电影", 2 to "电视剧", 3 to "综艺", 4 to "动漫",
        5 to "纪录片", 6 to "少儿", 7 to "动作片", 8 to "喜剧片",
        9 to "爱情片", 10 to "科幻片", 11 to "恐怖片", 12 to "剧情片",
        13 to "战争片", 14 to "国产剧", 15 to "港台剧", 16 to "日韩剧",
        17 to "欧美剧", 18 to "海外剧",
    )

    override fun getSourceType(): MediaSourceType = MediaSourceType.VOD

    override suspend fun fetchHomeRows(): Result<List<MediaRow>> = runCatching {
        coroutineScope {
            // 从多个源并行取数据
            val deferred = sourceApis.map { source ->
                async { fetchRowsFromSource(source) }
            }
            val allRows = deferred.flatMap { it.await() }

            // 合并去重 + 按分类排列
            val rows = mutableListOf<MediaRow>()
            val seenTitles = mutableSetOf<String>()

            // Banner 推荐（取第一个源的热门电影）
            val banner = allRows.filter { it.title.contains("电影") }.flatMap { it.items }.take(8)
            if (banner.isNotEmpty()) {
                rows.add(MediaRow("__banner__", banner, RowType.RECOMMENDED))
            }

            // 分类行
            listOf(
                "电影" to "🎬 热门电影",
                "电视剧" to "📺 热播电视剧",
                "动漫" to "📽️ 动漫番剧",
                "综艺" to "🎭 综艺娱乐",
                "纪录片" to "🌍 纪录片",
            ).forEach { (cat, label) ->
                val items = allRows.flatMap { it.items }
                    .filter { seenTitles.add(it.title) }
                    .filter { it.category.contains(cat) || it.genres.any { g -> g.contains(cat) } }
                    .take(18)
                if (items.isNotEmpty()) {
                    rows.add(MediaRow(label, items, if (cat == "电影") RowType.RECOMMENDED else RowType.LATEST))
                }
            }

            // 如果分类行少于4行，补通用推荐
            if (rows.size < 5) {
                val extras = allRows.flatMap { it.items }.distinctBy { it.id }.take(20)
                rows.add(MediaRow("🔥 大家都在看", extras.shuffled(), RowType.DEFAULT))
            }

            rows
        }
    }

    override suspend fun search(query: String): Result<List<MediaItem>> = runCatching {
        coroutineScope {
            val deferred = sourceApis.map { source ->
                async {
                    runCatching {
                        val url = "${source.apiUrl}?ac=detail&wd=${java.net.URLEncoder.encode(query, "UTF-8")}"
                        val response = URL(url).readText()
                        val result = json.decodeFromString<TvBoxResponse>(response)
                        result.list.map { it.toMediaItem() }
                    }.getOrDefault(emptyList())
                }
            }
            deferred.flatMap { it.await() }.distinctBy { it.id }.take(50)
        }
    }

    override suspend fun resolveStream(mediaItem: MediaItem): Result<StreamResult> = runCatching {
        val url = mediaItem.streamUrls.values.firstOrNull() ?: mediaItem.id
        StreamResult(streamUrl = url, isHls = url.contains(".m3u8"), preferredQuality = "auto")
    }

    override suspend fun isAvailable(): Boolean = runCatching {
        sourceApis.any {
            runCatching {
                val r = URL("${it.apiUrl}?ac=list").readText()
                r.contains("\"code\":1")
            }.getOrDefault(false)
        }
    }.getOrDefault(false)

    private suspend fun fetchRowsFromSource(source: TvBoxSource): List<MediaRow> {
        return runCatching {
            val rows = mutableListOf<MediaRow>()
            // 取电影 + 电视剧
            for (typeId in listOf(1, 2, 3, 4)) {
                val url = "${source.apiUrl}?ac=detail&t=$typeId&pg=1&pagesize=12"
                val response = URL(url).readText()
                val result = json.decodeFromString<TvBoxResponse>(response)
                if (result.list.isNotEmpty()) {
                    val items = result.list.map { it.toMediaItem() }
                    val catName = categoryMap[typeId] ?: "影视"
                    rows.add(MediaRow("[$source.name] $catName", items))
                }
            }
            rows
        }.getOrDefault(emptyList())
    }

    data class TvBoxSource(val name: String, val apiUrl: String)

    @Serializable
    data class TvBoxResponse(
        val code: Int = 0, val list: List<TvBoxVod> = emptyList(),
    )

    @Serializable
    data class TvBoxVod(
        val vod_id: Long = 0, val vod_name: String = "", val type_id: Int = 0,
        val vod_pic: String = "", val vod_actor: String = "", val vod_director: String = "",
        val vod_content: String = "", val vod_play_url: String = "",
        val vod_year: String = "", val vod_score: String = "0",
        val type_name: String = "", val vod_class: String = "",
    ) {
        fun toMediaItem(): MediaItem {
            val playUrl = vod_play_url.split("#").firstOrNull()?.split("$")?.getOrNull(1) ?: ""
            return MediaItem(
                id = "tvbox_$vod_id", title = vod_name,
                description = vod_content.take(200),
                posterUrl = vod_pic, year = vod_year.toIntOrNull() ?: 0,
                rating = vod_score.toFloatOrNull() ?: 0f,
                category = type_name, genres = vod_class.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                sourceType = MediaSourceType.VOD,
                streamUrls = parsePlayUrls(vod_play_url),
            )
        }
    }

    companion object {
        private fun parsePlayUrls(playUrl: String): Map<String, String> {
            if (playUrl.isBlank()) return emptyMap()
            val urls = mutableMapOf<String, String>()
            playUrl.split("#").forEach { seg ->
                val parts = seg.split("$")
                if (parts.size == 2) urls[parts[0].trim()] = parts[1].trim()
            }
            return urls
        }
    }
}

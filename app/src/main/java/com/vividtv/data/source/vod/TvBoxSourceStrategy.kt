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
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvBoxSourceStrategy @Inject constructor() : SourceStrategy {

    override val sourceId: String = "tvbox_aggregator"

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

    data class Provider(val name: String, val apiUrl: String)
    data class Category(val id: Int, val name: String, val parentId: Int = 0)

    private val providers = listOf(
        Provider("ffzy", "http://cj.ffzyapi.com/api.php/provide/vod"),
        Provider("1080zy", "https://api.1080zyapi.com/api.php/provide/vod"),
        Provider("80sjsc", "http://cj.80sjsc.com/api.php/provide/vod"),
    )

    // 分类分组规则
    private val groupRules = listOf(
        "电影" to listOf("电影", "动作", "喜剧", "爱情", "科幻", "恐怖", "剧情", "战争", "犯罪", "悬疑", "动画电影"),
        "电视剧" to listOf("剧", "电视剧", "连续剧"),
        "动漫" to listOf("动漫", "动画"),
        "综艺" to listOf("综艺"),
        "纪录片" to listOf("纪录"),
    )

    override fun getSourceType(): MediaSourceType = MediaSourceType.VOD

    override suspend fun fetchHomeRows(): Result<List<MediaRow>> = runCatching {
        // 先用空列表获取分类信息
        val categories = fetchCategories().ifEmpty {
            listOf(
                Category(7, "剧情片"), Category(9, "科幻片"), Category(10, "恐怖片"),
                Category(11, "喜剧片"), Category(13, "香港剧"), Category(14, "台湾剧"),
                Category(15, "国产剧"), Category(21, "日本剧"), Category(22, "韩国剧"),
                Category(24, "泰国剧"), Category(26, "国产动漫"), Category(29, "港台综艺"),
                Category(30, "日韩动漫"),
            )
        }

        val groupedCategories = groupCategories(categories)
        val rows = mutableListOf<MediaRow>()

        coroutineScope {
            val deferreds = groupedCategories.map { (groupName, cats) ->
                async {
                    val items = mutableListOf<MediaItem>()
                    for (cat in cats) {
                        val result = fetchItemsByCategory(cat.id, 6)
                        items.addAll(result)
                        if (items.size >= 12) break
                    }
                    groupName to items.take(12)
                }
            }

            val results = deferreds.map { it.await() }

            // 第一个分类作为 Banner（通常是最热门的）
            val firstItems = results.firstOrNull()?.second ?: emptyList()
            if (firstItems.isNotEmpty()) {
                rows.add(MediaRow("__banner__", firstItems.take(8), RowType.RECOMMENDED))
            }

            for ((groupName, items) in results) {
                if (items.isNotEmpty() && items != firstItems) {
                    val label = when (groupName) {
                        "电影" -> "🎬 热门电影"
                        "电视剧" -> "📺 热播电视剧"
                        "动漫" -> "📽️ 动漫番剧"
                        "综艺" -> "🎭 综艺娱乐"
                        "纪录片" -> "🌍 纪录片"
                        else -> "📺 $groupName"
                    }
                    rows.add(MediaRow(label, items.shuffled(), RowType.LATEST))
                }
            }
        }

        rows
    }

    override suspend fun search(query: String): Result<List<MediaItem>> = runCatching {
        coroutineScope {
            val deferreds = providers.map { provider ->
                async {
                    try {
                        val urlStr = "${provider.apiUrl}?ac=detail&wd=${java.net.URLEncoder.encode(query, "UTF-8")}"
                        val resp = httpGet(urlStr)
                        val result = json.decodeFromString<TvBoxResponse>(resp)
                        result.list.map { it.toMediaItem() }
                    } catch (_: Exception) { emptyList() }
                }
            }
            deferreds.flatMap { it.await() }.distinctBy { it.id }.take(50)
        }
    }

    override suspend fun resolveStream(mediaItem: MediaItem): Result<StreamResult> = runCatching {
        val url = mediaItem.streamUrls.values.firstOrNull() ?: mediaItem.id
        StreamResult(streamUrl = url, isHls = url.contains(".m3u8"), preferredQuality = "auto")
    }

    override suspend fun isAvailable(): Boolean = runCatching {
        providers.any { prov ->
            try {
                val r = httpGet("${prov.apiUrl}?ac=list")
                r.contains("\"code\":1")
            } catch (_: Exception) { false }
        }
    }.getOrDefault(false)

    // ── 分类获取 ──
    private suspend fun fetchCategories(): List<Category> {
        val cats = mutableMapOf<Int, Category>()
        for (prov in providers) {
            try {
                val r = httpGet("${prov.apiUrl}?ac=list")
                val response = json.decodeFromString<TvBoxResponse>(r)
            } catch (_: Exception) { continue }
        }
        // 如果没有分类信息，从实际数据中提取
        for (prov in providers) {
            try {
                val r = httpGet("${prov.apiUrl}?ac=detail&pg=1&pagesize=50")
                val response = json.decodeFromString<TvBoxResponse>(r)
                for (v in response.list) {
                    cats[v.type_id] = Category(v.type_id, v.type_name.ifEmpty { "未知" })
                }
            } catch (_: Exception) { continue }
        }
        return cats.values.toList()
    }

    private fun groupCategories(cats: List<Category>): List<Pair<String, List<Category>>> {
        val groups = mutableListOf<Pair<String, MutableList<Category>>>()
        val assigned = mutableSetOf<Int>()

        for ((groupName, keywords) in groupRules) {
            val matched = cats.filter { cat ->
                cat.id !in assigned && keywords.any { kw -> cat.name.contains(kw) }
            }.toMutableList()
            if (matched.isNotEmpty()) {
                assigned.addAll(matched.map { it.id })
                groups.add(groupName to matched)
            }
        }

        // 未分配的归入"其他"
        val others = cats.filter { it.id !in assigned }
        if (others.isNotEmpty()) {
            groups.add("其他" to others.toMutableList())
        }

        return groups
    }

    // ── TVBox 格式 ──

    @Serializable
    data class TvBoxResponse(
        val code: Int = 0,
        val list: List<TvBoxVod> = emptyList(),
        val `class`: List<TvBoxClass> = emptyList(),
    )

    @Serializable
    data class TvBoxVod(
        val vod_id: Long = 0,
        val type_id: Int = 0,
        val type_name: String = "",
        val vod_name: String = "",
        val vod_pic: String = "",
        val vod_content: String = "",
        val vod_play_url: String = "",
        val vod_year: String = "",
        val vod_score: String = "0",
        val type_id_1: Int = 0,
        val vod_actor: String = "",
        val vod_director: String = "",
        val vod_remarks: String = "",
    ) {
        fun toMediaItem(): MediaItem {
            val playUrl = parseFirstPlayUrl(vod_play_url)
            @Suppress("MagicNumber")
            return MediaItem(
                id = "tv_$vod_id",
                title = vod_name,
                description = vod_content.take(200).replace(Regex("&[^;]+;"), " "),
                posterUrl = vod_pic,
                year = vod_year.take(4).toIntOrNull() ?: 0,
                rating = vod_score.toFloatOrNull()?.let { if (it > 10) it / 10 else it } ?: 0f,
                category = type_name,
                sourceType = MediaSourceType.VOD,
                streamUrls = if (playUrl.isNotEmpty()) mapOf("play" to playUrl) else emptyMap(),
            )
        }
    }

    @Serializable
    data class TvBoxClass(
        val type_id: Int = 0,
        val type_name: String = "",
        val type_pid: Int = 0,
    )

    companion object {
        private fun parseFirstPlayUrl(playUrl: String): String {
            if (playUrl.isBlank()) return ""
            return playUrl.split("#").firstOrNull()
                ?.split("$")
                ?.getOrNull(1)
                ?: ""
        }

        private fun httpGet(urlStr: String): String {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            return conn.inputStream.bufferedReader().readText()
        }
    }

    // ── 按分类取数据 ──
    private suspend fun fetchItemsByCategory(typeId: Int, limit: Int): List<MediaItem> {
        return try {
            // 尝试用 type_id 过滤
            val url = "${providers.first().apiUrl}?ac=detail&t=$typeId&pg=1&pagesize=$limit"
            var resp = httpGet(url)
            var result = json.decodeFromString<TvBoxResponse>(resp)
            if (result.list.isNotEmpty()) {
                return result.list.map { it.toMediaItem() }
            }
            // 如果没有结果，用 type_id_1
            val url2 = "${providers.first().apiUrl}?ac=detail&type_id=$typeId&pg=1&pagesize=$limit"
            resp = httpGet(url2)
            result = json.decodeFromString<TvBoxResponse>(resp)
            if (result.list.isNotEmpty()) {
                return result.list.map { it.toMediaItem() }
            }
            // 兜底：全量取并按 type_id 过滤
            val url3 = "${providers.first().apiUrl}?ac=detail&pg=1&pagesize=$limit"
            resp = httpGet(url3)
            result = json.decodeFromString<TvBoxResponse>(resp)
            result.list.filter { it.type_id == typeId }.map { it.toMediaItem() }.take(limit)
        } catch (_: Exception) { emptyList() }
    }
}

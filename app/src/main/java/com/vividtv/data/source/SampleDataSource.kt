package com.vividtv.data.source

import com.vividtv.data.model.MediaItem
import com.vividtv.data.model.MediaRow
import com.vividtv.data.model.MediaSourceType
import com.vividtv.data.model.RowType
import com.vividtv.data.model.StreamResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 内置示例数据源 — 无需后端即可展示完整 UI
 * 当其他数据源不可用时作为降级方案
 */
@Singleton
class SampleDataSource @Inject constructor() : SourceStrategy {

    override val sourceId: String = "sample"

    override fun getSourceType(): MediaSourceType = MediaSourceType.VOD

    override suspend fun fetchHomeRows(): Result<List<MediaRow>> = Result.success(sampleRows)

    override suspend fun search(query: String): Result<List<MediaItem>> = Result.success(
        sampleItems.filter { it.title.contains(query, ignoreCase = true) }
    )

    override suspend fun resolveStream(mediaItem: MediaItem): Result<StreamResult> = Result.success(
        StreamResult(
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            isHls = true,
            preferredQuality = "auto",
        )
    )

    override suspend fun isAvailable(): Boolean = true

    companion object {
        private val sampleItems = listOf(
            MediaItem(
                id = "s1", title = "流浪地球2", description = "人类面临太阳危机，联合政府决定实施移山计划。",
                posterUrl = "https://picsum.photos/seed/movie1/400/225", year = 2023, rating = 8.3f,
                category = "科幻", genres = listOf("科幻", "冒险", "灾难"),
                streamUrls = mapOf("1080p" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            ),
            MediaItem(
                id = "s2", title = "满江红", description = "南宋年间，一场围绕秦桧的阴谋正在展开。",
                posterUrl = "https://picsum.photos/seed/movie2/400/225", year = 2023, rating = 7.4f,
                category = "剧情", genres = listOf("剧情", "悬疑", "喜剧"),
                streamUrls = mapOf("1080p" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            ),
            MediaItem(
                id = "s3", title = "奥本海默", description = "美国物理学家奥本海默领导曼哈顿计划的故事。",
                posterUrl = "https://picsum.photos/seed/movie3/400/225", year = 2023, rating = 8.8f,
                category = "剧情", genres = listOf("剧情", "传记", "历史"),
                streamUrls = mapOf("1080p" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            ),
            MediaItem(
                id = "s4", title = "哪吒之魔童闹海", description = "哪吒重生后对抗龙族的故事。",
                posterUrl = "https://picsum.photos/seed/movie4/400/225", year = 2024, rating = 8.5f,
                category = "动画", genres = listOf("动画", "奇幻", "喜剧"),
                streamUrls = mapOf("1080p" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            ),
            MediaItem(
                id = "s5", title = "繁花", description = "90年代上海黄河路，阿宝的传奇人生。",
                posterUrl = "https://picsum.photos/seed/movie5/400/225", year = 2023, rating = 8.7f,
                category = "电视剧", genres = listOf("剧情", "年代"),
                streamUrls = mapOf("1080p" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            ),
            MediaItem(
                id = "s6", title = "三体", description = "科学家们发现物理学不存在了。",
                posterUrl = "https://picsum.photos/seed/movie6/400/225", year = 2023, rating = 8.7f,
                category = "电视剧", genres = listOf("科幻", "悬疑"),
                streamUrls = mapOf("1080p" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            ),
            MediaItem(
                id = "s7", title = "飞驰人生2", description = "曾经的赛车手张驰重返赛道。",
                posterUrl = "https://picsum.photos/seed/movie7/400/225", year = 2024, rating = 7.9f,
                category = "喜剧", genres = listOf("喜剧", "运动"),
                streamUrls = mapOf("1080p" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            ),
            MediaItem(
                id = "s8", title = "沙丘2", description = "保罗·厄崔迪的复仇之路。",
                posterUrl = "https://picsum.photos/seed/movie8/400/225", year = 2024, rating = 8.6f,
                category = "科幻", genres = listOf("科幻", "冒险"),
                streamUrls = mapOf("4K" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            ),
            MediaItem(
                id = "s9", title = "热辣滚烫", description = "宅女乐莹通过拳击找到自我。",
                posterUrl = "https://picsum.photos/seed/movie9/400/225", year = 2024, rating = 7.8f,
                category = "喜剧", genres = listOf("喜剧", "励志"),
                streamUrls = mapOf("1080p" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            ),
            MediaItem(
                id = "s10", title = "周处除三害", description = "通缉犯陈桂林要死在所有人前面。",
                posterUrl = "https://picsum.photos/seed/movie10/400/225", year = 2023, rating = 8.1f,
                category = "犯罪", genres = listOf("犯罪", "动作"),
                streamUrls = mapOf("1080p" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            ),
            MediaItem(
                id = "s11", title = "死侍3", description = "死侍加入漫威电影宇宙。",
                posterUrl = "https://picsum.photos/seed/movie11/400/225", year = 2024, rating = 8.0f,
                category = "动作", genres = listOf("动作", "喜剧", "科幻"),
                streamUrls = mapOf("1080p" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            ),
            MediaItem(
                id = "s12", title = "头脑特工队2", description = "小女孩莱莉的大脑总部迎来新情绪。",
                posterUrl = "https://picsum.photos/seed/movie12/400/225", year = 2024, rating = 8.4f,
                category = "动画", genres = listOf("动画", "冒险", "喜剧"),
                streamUrls = mapOf("1080p" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            ),
        )

        private val sampleRows = listOf(
            MediaRow(title = "🔥 热门推荐", items = sampleItems.take(6), rowType = RowType.RECOMMENDED),
            MediaRow(title = "🎬 最新上线", items = sampleItems.drop(6).take(6), rowType = RowType.LATEST),
            MediaRow(title = "⭐ 高分经典", items = sampleItems.shuffled().take(6), rowType = RowType.DEFAULT),
            MediaRow(title = "🎭 国产佳作", items = listOf(sampleItems[0], sampleItems[1], sampleItems[4], sampleItems[8], sampleItems[9])),
            MediaRow(title = "🌍 海外大片", items = listOf(sampleItems[2], sampleItems[7], sampleItems[10], sampleItems[11])),
            MediaRow(title = "📺 热播剧集", items = listOf(sampleItems[4], sampleItems[5])),
        )
    }
}

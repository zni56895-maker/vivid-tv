package com.vividtv.data.source.iptv

import com.vividtv.data.model.IptvChannel
import com.vividtv.data.model.MediaItem
import com.vividtv.data.model.MediaRow
import com.vividtv.data.model.MediaSourceType
import com.vividtv.data.model.RowType
import com.vividtv.data.model.StreamResult
import com.vividtv.data.source.SourceStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IPTV 直播数据源
 * 支持动态加载远程 M3U8 播放列表
 */
@Singleton
class IptvSourceStrategy @Inject constructor(
    private val okHttpClient: OkHttpClient,
) : SourceStrategy {

    override val sourceId: String = "iptv_live"

    private var cachedChannels: List<IptvChannel> = emptyList()
    private var m3u8Urls: List<String> = emptyList()
    private var lastUpdateTime: Long = 0L
    private val updateIntervalMs = 24 * 60 * 60 * 1000L // 24 hours

    override fun getSourceType(): MediaSourceType = MediaSourceType.IPTV

    /** Update the M3U8 playlist URLs dynamically */
    fun updateM3u8Sources(urls: List<String>) {
        m3u8Urls = urls
        cachedChannels = emptyList()
    }

    /** Add a single M3U8 source */
    fun addM3u8Source(url: String) {
        m3u8Urls = m3u8Urls + url
        cachedChannels = emptyList()
    }

    override suspend fun fetchHomeRows(): Result<List<MediaRow>> {
        return runCatching {
            refreshChannelsIfNeeded()
            if (cachedChannels.isEmpty()) return@runCatching emptyList()

            // Group channels by group name
            val grouped = cachedChannels.groupBy { it.group }
            grouped.map { (group, channels) ->
                MediaRow(
                    title = "📺 $group",
                    items = channels.map { it.toMediaItem() },
                    rowType = RowType.IPTV_CHANNELS,
                )
            }
        }
    }

    override suspend fun search(query: String): Result<List<MediaItem>> {
        return runCatching {
            refreshChannelsIfNeeded()
            cachedChannels
                .filter { it.name.contains(query, ignoreCase = true) }
                .map { it.toMediaItem() }
        }
    }

    override suspend fun resolveStream(mediaItem: MediaItem): Result<StreamResult> {
        // For IPTV, the stream URL is already in the media item
        return Result.success(
            StreamResult(
                streamUrl = mediaItem.streamUrls.values.firstOrNull()
                    ?: mediaItem.id, // id acts as streamUrl fallback
                isHls = true,
                preferredQuality = "auto",
            )
        )
    }

    override suspend fun isAvailable(): Boolean {
        return m3u8Urls.isNotEmpty()
    }

    private suspend fun refreshChannelsIfNeeded() {
        val now = System.currentTimeMillis()
        if (cachedChannels.isNotEmpty() && (now - lastUpdateTime) < updateIntervalMs) return
        refreshChannels()
    }

    private suspend fun refreshChannels() {
        if (m3u8Urls.isEmpty()) return

        val allChannels = mutableListOf<IptvChannel>()
        for (url in m3u8Urls) {
            try {
                val channels = parseM3u8Url(url)
                allChannels.addAll(channels)
            } catch (_: Exception) {
                // Skip failed URLs, continue with others
            }
        }
        cachedChannels = allChannels
        lastUpdateTime = System.currentTimeMillis()
    }

    private suspend fun parseM3u8Url(url: String): List<IptvChannel> {
        return withContext(Dispatchers.IO) {
            val request = okhttp3.Request.Builder()
                .url(url)
                .addHeader("User-Agent", "VividTv/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()

            parseM3u8Content(body)
        }
    }

    /** Parse M3U8 playlist content into channel entries */
    private fun parseM3u8Content(content: String): List<IptvChannel> {
        val channels = mutableListOf<IptvChannel>()
        val lines = content.lines()
        var index = 0
        var currentExtinf: String? = null

        while (index < lines.size) {
            val line = lines[index].trim()

            when {
                line.startsWith("#EXTINF:") -> {
                    currentExtinf = line
                }
                line.startsWith("#EXTVLCOPT:") -> {
                    // Skip VLC specific options
                }
                line.startsWith("http://") || line.startsWith("https://") -> {
                    val channel = parseChannel(line, currentExtinf, channels.size + 1)
                    channels.add(channel)
                    currentExtinf = null
                }
            }
            index++
        }

        return channels
    }

    /** Parse a single channel from EXTINF + URL line */
    private fun parseChannel(
        url: String,
        extinf: String?,
        channelNumber: Int,
    ): IptvChannel {
        val name = if (extinf != null) {
            // EXTINF:-1 tvg-id="" tvg-name="Channel Name" group-title="Group",Channel Name
            val nameMatch = Regex("""tvg-name="([^"]*)"""").find(extinf)
            val groupMatch = Regex("""group-title="([^"]*)"""").find(extinf)
            val logoMatch = Regex("""tvg-logo="([^"]*)"""").find(extinf)

            val channelName = nameMatch?.groupValues?.getOrNull(1)
                ?: extinf.substringAfterLast(",").trim()
            val group = groupMatch?.groupValues?.getOrNull(1) ?: "General"
            val logo = logoMatch?.groupValues?.getOrNull(1) ?: ""

            IptvChannel(
                id = "iptv_$channelNumber",
                name = channelName,
                logoUrl = logo,
                streamUrl = url,
                group = group,
            )
        } else {
            IptvChannel(
                id = "iptv_$channelNumber",
                name = "Channel $channelNumber",
                streamUrl = url,
            )
        }
        return name
    }

    private fun IptvChannel.toMediaItem(): MediaItem = MediaItem(
        id = this.id,
        title = this.name,
        posterUrl = this.logoUrl,
        streamUrls = mapOf("live" to this.streamUrl),
        sourceType = MediaSourceType.IPTV,
        isLive = true,
        category = this.group,
        description = this.group,
    )
}

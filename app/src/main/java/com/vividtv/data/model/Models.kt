package com.vividtv.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaItem(
    val id: String,
    val title: String,
    val description: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val year: Int = 0,
    val rating: Float = 0f,
    val category: String = "",
    val genres: List<String> = emptyList(),
    val sourceType: MediaSourceType = MediaSourceType.VOD,
    val streamUrls: Map<String, String> = emptyMap(), // quality -> url
    val duration: Long = 0L,
    val isLive: Boolean = false,
)

@Serializable
data class MediaRow(
    val title: String,
    val items: List<MediaItem>,
    val rowType: RowType = RowType.DEFAULT,
)

@Serializable
enum class RowType {
    DEFAULT,
    CONTINUE_WATCHING,
    RECOMMENDED,
    LATEST,
    GENRE,
    IPTV_CHANNELS,
}

@Serializable
enum class MediaSourceType {
    VOD,
    IPTV,
    OVERSEAS,
}

@Serializable
data class StreamResult(
    val streamUrl: String,
    val isHls: Boolean = false,
    val isDash: Boolean = false,
    val drmScheme: String? = null,
    val drmLicenseUrl: String? = null,
    val availableQualities: List<String> = emptyList(),
    val preferredQuality: String = "auto",
    val headers: Map<String, String> = emptyMap(),
)

@Serializable
data class MediaSourceConfig(
    val name: String,
    val type: String,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val config: Map<String, String> = emptyMap(),
)

@Serializable
data class IptvChannel(
    val id: String,
    val name: String,
    val logoUrl: String = "",
    val streamUrl: String,
    val epgUrl: String = "",
    val group: String = "General",
    val isActive: Boolean = true,
)

@Serializable
data class OverseasNode(
    val name: String,
    val baseUrl: String,
    val regions: List<String> = emptyList(),
    val latency: Int = 0,
    val isActive: Boolean = true,
)

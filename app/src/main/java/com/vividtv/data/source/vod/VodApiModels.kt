package com.vividtv.data.source.vod

import com.vividtv.data.model.MediaItem
import kotlinx.serialization.Serializable

@Serializable
data class ApiHomeResponse(
    val rows: List<ApiRow>,
)

@Serializable
data class ApiRow(
    val title: String,
    val type: String = "default",
    val items: List<ApiMediaItem>,
)

@Serializable
data class ApiMediaItem(
    val id: String,
    val title: String,
    val description: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val year: Int = 0,
    val rating: Float = 0f,
    val category: String = "",
    val genres: List<String> = emptyList(),
    val duration: Long = 0L,
) {
    fun toMediaItem(): MediaItem = MediaItem(
        id = id,
        title = title,
        description = description,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        year = year,
        rating = rating,
        category = category,
        genres = genres,
        duration = duration,
        sourceType = com.vividtv.data.model.MediaSourceType.VOD,
    )
}

@Serializable
data class ApiSearchResponse(
    val items: List<ApiMediaItem>,
)

@Serializable
data class ApiStreamResponse(
    val url: String,
    val format: String = "hls",
    val qualities: List<String> = emptyList(),
    val drm: String? = null,
    val drmLicenseUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
)

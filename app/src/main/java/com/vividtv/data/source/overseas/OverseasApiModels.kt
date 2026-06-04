package com.vividtv.data.source.overseas

import com.vividtv.data.model.MediaItem
import kotlinx.serialization.Serializable

@Serializable
data class OverseasHomeResponse(
    val rows: List<OverseasRow>,
)

@Serializable
data class OverseasRow(
    val title: String,
    val items: List<OverseasMediaItem>,
)

@Serializable
data class OverseasMediaItem(
    val id: String,
    val title: String,
    val description: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val year: Int = 0,
    val rating: Float = 0f,
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
        duration = duration,
        sourceType = com.vividtv.data.model.MediaSourceType.OVERSEAS,
    )
}

@Serializable
data class OverseasSearchResponse(
    val items: List<OverseasMediaItem>,
)

@Serializable
data class OverseasStreamResponse(
    val url: String,
    val format: String = "hls",
    val qualities: List<String> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
)

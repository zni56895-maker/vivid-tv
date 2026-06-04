package com.vividtv.data.source

import com.vividtv.data.model.MediaItem
import com.vividtv.data.model.MediaRow
import com.vividtv.data.model.MediaSourceType
import com.vividtv.data.model.StreamResult

interface SourceStrategy {
    /** Unique identifier for this source */
    val sourceId: String

    /** The type of media source */
    fun getSourceType(): MediaSourceType

    /** Fetch home page rows (rows of media items) */
    suspend fun fetchHomeRows(): Result<List<MediaRow>>

    /** Search across this source */
    suspend fun search(query: String): Result<List<MediaItem>>

    /** Resolve a playable stream URL from a video URL or identifier */
    suspend fun resolveStream(mediaItem: MediaItem): Result<StreamResult>

    /** Whether this source is currently available */
    suspend fun isAvailable(): Boolean = true
}

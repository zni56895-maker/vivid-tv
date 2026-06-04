package com.vividtv.data.repository

import com.vividtv.data.model.MediaItem
import com.vividtv.data.model.MediaRow
import com.vividtv.data.model.StreamResult
import com.vividtv.data.source.SourceStrategy
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central repository that aggregates all media sources (VOD, IPTV, Overseas).
 * Uses parallel fetching with structured concurrency.
 */
@Singleton
class MediaRepository @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards SourceStrategy>,
) {

    /**
     * Fetch home rows from all available sources in parallel,
     * then merge them into a single ordered list.
     */
    suspend fun getHomeRows(): Result<List<MediaRow>> = runCatching {
        coroutineScope {
            val deferred = sources
                .filter { it.isAvailable() }
                .map { source ->
                    async {
                        source.fetchHomeRows()
                    }
                }
            // Await all sources in parallel
            val results = deferred.mapNotNull { it.await().getOrNull() }
            results.flatten()
        }
    }

    /** Search across all available sources */
    suspend fun search(query: String): Result<List<MediaItem>> = runCatching {
        coroutineScope {
            val deferred = sources
                .filter { it.isAvailable() }
                .map { source ->
                    async { source.search(query) }
                }
            val results = deferred.mapNotNull { it.await().getOrNull() }
            results.flatten()
        }
    }

    /** Resolve a playable stream URL */
    suspend fun resolveStream(mediaItem: MediaItem): Result<StreamResult> {
        // Find the source that can handle this media item type
        val source = sources.firstOrNull { it.getSourceType() == mediaItem.sourceType }
            ?: return Result.failure(IllegalStateException("No source found for type: ${mediaItem.sourceType}"))

        return source.resolveStream(mediaItem)
    }
}

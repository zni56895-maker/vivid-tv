package com.vividtv.data.source.vod

import com.vividtv.data.model.MediaItem
import com.vividtv.data.model.MediaRow
import com.vividtv.data.model.MediaSourceType
import com.vividtv.data.model.RowType
import com.vividtv.data.model.StreamResult
import com.vividtv.data.remote.VodApiService
import com.vividtv.data.source.SourceStrategy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VodSourceStrategy @Inject constructor(
    private val vodApi: VodApiService,
) : SourceStrategy {

    override val sourceId: String = "vod_default"
    private var cachedRows: List<MediaRow> = emptyList()

    override fun getSourceType(): MediaSourceType = MediaSourceType.VOD

    override suspend fun fetchHomeRows(): Result<List<MediaRow>> {
        return runCatching {
            val response = vodApi.getHomePage()
            response.rows.map { apiRow ->
                MediaRow(
                    title = apiRow.title,
                    items = apiRow.items.map { it.toMediaItem() },
                    rowType = when (apiRow.type) {
                        "continue" -> RowType.CONTINUE_WATCHING
                        "recommended" -> RowType.RECOMMENDED
                        "latest" -> RowType.LATEST
                        else -> RowType.DEFAULT
                    }
                )
            }.also { cachedRows = it }
        }
    }

    override suspend fun search(query: String): Result<List<MediaItem>> {
        return runCatching {
            vodApi.search(query).items.map { it.toMediaItem() }
        }
    }

    override suspend fun resolveStream(mediaItem: MediaItem): Result<StreamResult> {
        return runCatching {
            val response = vodApi.resolveStream(mediaItem.id)
            StreamResult(
                streamUrl = response.url,
                isHls = response.format == "hls",
                isDash = response.format == "dash",
                availableQualities = response.qualities,
                preferredQuality = "auto",
            )
        }
    }

    override suspend fun isAvailable(): Boolean {
        return runCatching {
            vodApi.healthCheck()
            true
        }.getOrDefault(false)
    }
}

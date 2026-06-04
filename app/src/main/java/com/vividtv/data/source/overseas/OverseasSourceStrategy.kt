package com.vividtv.data.source.overseas

import com.vividtv.data.model.MediaItem
import com.vividtv.data.model.MediaRow
import com.vividtv.data.model.MediaSourceType
import com.vividtv.data.model.OverseasNode
import com.vividtv.data.model.StreamResult
import com.vividtv.data.remote.OverseasApiService
import com.vividtv.data.source.SourceStrategy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 海外多专线节点聚合源
 * 支持根据区域自动选择最优节点，支持故障转移
 */
@Singleton
class OverseasSourceStrategy @Inject constructor(
    private val overseasApi: OverseasApiService,
) : SourceStrategy {

    override val sourceId: String = "overseas_aggregate"
    private var activeNodes: List<OverseasNode> = emptyList()
    private var preferredRegions: List<String> = listOf("asia", "global")

    override fun getSourceType(): MediaSourceType = MediaSourceType.OVERSEAS

    suspend fun updateNodes(nodes: List<OverseasNode>) {
        activeNodes = nodes.filter { it.isActive }
            .sortedBy { it.latency }
    }

    suspend fun setPreferredRegions(regions: List<String>) {
        preferredRegions = regions
    }

    /** Get the best available node based on latency and region preference */
    private suspend fun getBestNode(): OverseasNode? {
        // Try preferred region nodes first, sorted by latency
        val preferred = activeNodes
            .filter { it.regions.any { r -> preferredRegions.contains(r) } }
            .sortedBy { it.latency }
        if (preferred.isNotEmpty()) return preferred.first()

        // Fallback to any active node
        return activeNodes.minByOrNull { it.latency }
    }

    override suspend fun fetchHomeRows(): Result<List<MediaRow>> {
        return runCatching {
            val node = getBestNode()
                ?: return@runCatching emptyList()

            val response = overseasApi.getHomePage(node.baseUrl)
            response.rows.map { apiRow ->
                MediaRow(
                    title = "[海外] ${apiRow.title}",
                    items = apiRow.items.map { it.toMediaItem() },
                )
            }
        }
    }

    override suspend fun search(query: String): Result<List<MediaItem>> {
        return runCatching {
            val node = getBestNode()
                ?: return@runCatching emptyList()

            overseasApi.search(node.baseUrl, query).items.map { it.toMediaItem() }
        }
    }

    override suspend fun resolveStream(mediaItem: MediaItem): Result<StreamResult> {
        return runCatching {
            val node = getBestNode()
                ?: throw IllegalStateException("No available overseas nodes")

            val response = overseasApi.resolveStream(node.baseUrl, mediaItem.id)
            StreamResult(
                streamUrl = response.url,
                isHls = response.format == "hls",
                isDash = response.format == "dash",
                availableQualities = response.qualities,
                preferredQuality = "auto",
                headers = response.headers,
            )
        }
    }

    override suspend fun isAvailable(): Boolean {
        return runCatching {
            val node = getBestNode() ?: return@runCatching false
            overseasApi.healthCheck(node.baseUrl)
            true
        }.getOrDefault(false)
    }
}

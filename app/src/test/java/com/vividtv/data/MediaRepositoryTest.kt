package com.vividtv.data

import com.vividtv.data.model.MediaItem
import com.vividtv.data.model.MediaRow
import com.vividtv.data.model.MediaSourceType
import com.vividtv.data.model.StreamResult
import com.vividtv.data.repository.MediaRepository
import com.vividtv.data.source.SourceStrategy
import com.vividtv.data.source.vod.VodSourceStrategy
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MediaRepositoryTest {

    @Test
    fun `repository gets home rows from multiple sources`() = runTest {
        // Mock VOD source
        val vodSource = mockk<VodSourceStrategy>()
        every { vodSource.sourceId } returns "vod"
        every { vodSource.isAvailable() } returns true
        every { vodSource.getSourceType() } returns MediaSourceType.VOD
        coEvery { vodSource.fetchHomeRows() } returns Result.success(
            listOf(MediaRow(title = "VOD Row", items = emptyList()))
        )

        val repository = MediaRepository(setOf(vodSource))
        val result = repository.getHomeRows()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("VOD Row", result.getOrNull()?.get(0)?.title)
    }

    @Test
    fun `repository handles empty sources`() = runTest {
        val repository = MediaRepository(emptySet())
        val result = repository.getHomeRows()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() ?: false)
    }

    @Test
    fun `repository skips unavailable sources`() = runTest {
        val source = mockk<SourceStrategy>()
        every { source.sourceId } returns "unavailable"
        every { source.isAvailable() } returns false
        every { source.getSourceType() } returns MediaSourceType.VOD

        val repository = MediaRepository(setOf(source))
        val result = repository.getHomeRows()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() ?: false)
    }

    @Test
    fun `repository resolves stream correctly`() = runTest {
        val source = mockk<SourceStrategy>()
        every { source.sourceId } returns "vod"
        every { source.isAvailable() } returns true
        every { source.getSourceType() } returns MediaSourceType.VOD
        coEvery { source.resolveStream(any()) } returns Result.success(
            StreamResult(
                streamUrl = "https://example.com/stream.m3u8",
                isHls = true,
            )
        )

        val repository = MediaRepository(setOf(source))
        val item = MediaItem(id = "123", title = "Test", sourceType = MediaSourceType.VOD)
        val result = repository.resolveStream(item)
        assertTrue(result.isSuccess)
        assertEquals("https://example.com/stream.m3u8", result.getOrNull()?.streamUrl)
    }

    @Test
    fun `repository search aggregates from all sources`() = runTest {
        val source1 = mockk<SourceStrategy>()
        every { source1.sourceId } returns "src1"
        every { source1.isAvailable() } returns true
        every { source1.getSourceType() } returns MediaSourceType.VOD
        coEvery { source1.search(any()) } returns Result.success(
            listOf(MediaItem(id = "1", title = "Result 1", sourceType = MediaSourceType.VOD))
        )

        val source2 = mockk<SourceStrategy>()
        every { source2.sourceId } returns "src2"
        every { source2.isAvailable() } returns true
        every { source2.getSourceType() } returns MediaSourceType.IPTV
        coEvery { source2.search(any()) } returns Result.success(
            listOf(MediaItem(id = "2", title = "Result 2", sourceType = MediaSourceType.IPTV))
        )

        val repository = MediaRepository(setOf(source1, source2))
        val result = repository.search("test")
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
    }
}

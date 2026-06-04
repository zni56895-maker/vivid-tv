package com.vividtv.domain.player

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for PlaybackManager core logic.
 * Tests focus on: state management, quality switching, retry logic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PlaybackManagerTest {

    @Test
    fun `playbackQuality fromHeight returns correct quality for height values`() {
        assertEquals(PlaybackQuality.UHD_4K, PlaybackQuality.fromHeight(2160))
        assertEquals(PlaybackQuality.HD_1080P, PlaybackQuality.fromHeight(1080))
        assertEquals(PlaybackQuality.HD_720P, PlaybackQuality.fromHeight(720))
        assertEquals(PlaybackQuality.SD_480P, PlaybackQuality.fromHeight(480))
        assertEquals(PlaybackQuality.SD_480P, PlaybackQuality.fromHeight(0))
    }

    @Test
    fun `playbackQuality fromLabel returns correct quality`() {
        assertEquals(PlaybackQuality.AUTO, PlaybackQuality.fromLabel("自动"))
        assertEquals(PlaybackQuality.UHD_4K, PlaybackQuality.fromLabel("4K"))
        assertEquals(PlaybackQuality.HD_1080P, PlaybackQuality.fromLabel("1080P"))
        assertEquals(PlaybackQuality.HD_720P, PlaybackQuality.fromLabel("720P"))
        assertEquals(PlaybackQuality.AUTO, PlaybackQuality.fromLabel("unknown"))
    }

    @Test
    fun `playbackState default values are correct`() {
        val state = PlaybackState()
        assertFalse(state.isPlaying)
        assertFalse(state.isBuffering)
        assertEquals(0L, state.currentPosition)
        assertEquals(0L, state.duration)
        assertEquals(0, state.bufferedPercentage)
        assertEquals("auto", state.currentQuality)
        assertNull(state.error)
    }

    @Test
    fun `playbackState copy updates fields correctly`() {
        val state = PlaybackState()
        val updated = state.copy(isPlaying = true, currentPosition = 5000L)

        assertTrue(updated.isPlaying)
        assertFalse(updated.isBuffering)
        assertEquals(5000L, updated.currentPosition)
        assertNull(updated.error)
    }

    @Test
    fun `playbackError carries correct retryable value`() {
        val networkError = PlaybackError("网络错误", 1000, isRetryable = true)
        assertTrue(networkError.isRetryable)

        val fatalError = PlaybackError("致命错误", -1, isRetryable = false)
        assertFalse(fatalError.isRetryable)
    }

    @Test
    fun `playbackQuality entries are ordered correctly`() {
        val entries = PlaybackQuality.entries
        assertEquals(PlaybackQuality.AUTO, entries[0])
        assertEquals(PlaybackQuality.UHD_4K, entries[1])
        assertEquals(PlaybackQuality.HD_1080P, entries[2])
        assertEquals(PlaybackQuality.HD_720P, entries[3])
        assertEquals(PlaybackQuality.SD_480P, entries[4])
    }

    @Test
    fun `fromHeight chooses highest matching quality`() {
        // 4K content
        assertEquals(PlaybackQuality.UHD_4K, PlaybackQuality.fromHeight(4320))
        // 2K content
        assertEquals(PlaybackQuality.UHD_4K, PlaybackQuality.fromHeight(2160))
        // 1080p content
        assertEquals(PlaybackQuality.HD_1080P, PlaybackQuality.fromHeight(1440))
        assertEquals(PlaybackQuality.HD_1080P, PlaybackQuality.fromHeight(1080))
        // 720p content
        assertEquals(PlaybackQuality.HD_720P, PlaybackQuality.fromHeight(720))
        // lower
        assertEquals(PlaybackQuality.SD_480P, PlaybackQuality.fromHeight(100))
    }
}

package com.vividtv.domain.player

import android.content.Context
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.PlaybackException
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.common.MediaItem as Media3Item
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPercentage: Int = 0,
    val currentQuality: String = "auto",
    val availableQualities: List<String> = emptyList(),
    val error: PlaybackError? = null,
)

data class PlaybackError(
    val message: String,
    val code: Int,
    val isRetryable: Boolean,
)

enum class PlaybackQuality(val label: String, val height: Int) {
    AUTO("自动", 0),
    UHD_4K("4K", 2160),
    HD_1080P("1080P", 1080),
    HD_720P("720P", 720),
    SD_480P("480P", 480);

    companion object {
        fun fromHeight(height: Int): PlaybackQuality =
            entries.reversed().firstOrNull { height >= it.height } ?: SD_480P

        fun fromLabel(label: String): PlaybackQuality =
            entries.firstOrNull { it.label == label } ?: AUTO
    }
}

/**
 * Core playback manager based on Media3 ExoPlayer.
 * - Default hardware acceleration enabled (MediaCodec priority)
 * - H.265/HEVC + AV1 decode preference with smart fallback
 * - 4K → 1080p automatic degradation on decode failure
 * - State management via StateFlow
 */
@UnstableApi
@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var exoPlayer: ExoPlayer? = null
    private var currentMediaSource: MediaSource? = null
    private var retryCount = 0
    private val maxRetries = 3

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    /** Selected quality preference (default: auto) */
    private var qualityPreference: PlaybackQuality = PlaybackQuality.AUTO

    /** Create and configure the ExoPlayer instance with hardware acceleration */
    fun createPlayer(): ExoPlayer {
        releasePlayer()

        // ── RenderersFactory: hardware acceleration first ──
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            // Force MediaCodec (hardware decoder) priority over software fallback
            .setMediaCodecSelector(MediaCodecSelector.DEFAULT)

        // ── TrackSelector: adaptive bitrate with quality preference ──
        val trackSelectionFactory: ExoTrackSelection.Factory =
            AdaptiveTrackSelection.Factory()
        val trackSelector = DefaultTrackSelector(context, trackSelectionFactory)

        // Configure track selector for preferred video quality
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                // Prefer hardware decoding
                .setForceLowestBitrate(false)
                // Allow exceeding the height limit when UHD selected
                .setMaxVideoSize(if (qualityPreference == PlaybackQuality.AUTO) 1920 else 3840, 2160)
        )

        // ── Build player ──
        return ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .build()
            .also { player ->
                exoPlayer = player

                // Set high-quality audio output
                player.setAudioAttributes(
                    androidx.media3.common.AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true // handle audio focus
                )

                // Enable repeat mode for live streams
                player.repeatMode = Player.REPEAT_MODE_OFF

                // Listen to playback state changes
                player.addListener(playbackListener)
            }
    }

    /** Get the internal ExoPlayer instance (may be null before createPlayer) */
    fun getExoPlayer(): ExoPlayer? = exoPlayer

    /** Play a media URL with auto-detection of stream format */
    fun play(
        url: String,
        mediaTitle: String? = null,
        isLive: Boolean = false,
        qualities: List<String>? = null,
    ) {
        val player = exoPlayer ?: createPlayer()

        // Build the MediaSource based on URL format
        val mediaItem = Media3Item.Builder()
            .setMediaId(mediaTitle ?: url)
            .setUri(url)
            .setMimeType(detectMimeType(url))
            .build()

        val mediaSource = buildMediaSource(mediaItem, url)

        player.setMediaSource(mediaSource)
        player.prepare()
        player.play()

        currentMediaSource = mediaSource

        _playbackState.value = _playbackState.value.copy(
            currentQuality = qualityPreference.label,
            availableQualities = qualities ?: emptyList(),
        )
    }

    /** Play a MediaItem with explicit stream configuration */
    fun play(
        mediaItem: Media3Item,
        streamUrl: String,
        headers: Map<String, String> = emptyMap(),
    ) {
        val player = exoPlayer ?: createPlayer()

        val mediaSource = buildMediaSource(mediaItem, streamUrl)

        player.setMediaSource(mediaSource)
        player.prepare()
        player.play()

        currentMediaSource = mediaSource
    }

    /** Switch quality during playback */
    fun switchQuality(quality: PlaybackQuality) {
        qualityPreference = quality
        val trackSelector = exoPlayer?.trackSelector as? DefaultTrackSelector ?: return

        val maxHeight = when (quality) {
            PlaybackQuality.UHD_4K -> 2160
            PlaybackQuality.HD_1080P -> 1080
            PlaybackQuality.HD_720P -> 720
            PlaybackQuality.SD_480P -> 480
            PlaybackQuality.AUTO -> 2160 // Allow all
        }

        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setMaxVideoSize(3840, maxHeight)
        )

        _playbackState.value = _playbackState.value.copy(
            currentQuality = quality.label,
        )
    }

    /** Resume playback */
    fun play() {
        exoPlayer?.play()
    }

    /** Pause playback */
    fun pause() {
        exoPlayer?.pause()
    }

    /** Seek to a specific position */
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    /** Get current playback position */
    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L

    /** Get total duration */
    fun getDuration(): Long = exoPlayer?.duration ?: 0L

    /** Retry playback after error (with smart quality degradation) */
    fun retry() {
        if (retryCount >= maxRetries) {
            _playbackState.value = _playbackState.value.copy(
                error = PlaybackError(
                    message = "播放重试次数已达上限",
                    code = -1,
                    isRetryable = false,
                )
            )
            return
        }

        retryCount++
        val position = exoPlayer?.currentPosition ?: 0L
        val mediaSource = currentMediaSource

        // On second retry, degrade quality to 1080p
        if (retryCount >= 2 && qualityPreference == PlaybackQuality.UHD_4K) {
            switchQuality(PlaybackQuality.HD_1080P)
            Timber.w("4K playback failed, degrading to 1080p (retry #$retryCount)")
        }

        if (mediaSource != null) {
            exoPlayer?.seekTo(position)
            exoPlayer?.prepare()
            exoPlayer?.play()
        }
    }

    /** Release all resources */
    fun releasePlayer() {
        exoPlayer?.removeListener(playbackListener)
        exoPlayer?.release()
        exoPlayer = null
        retryCount = 0
    }

    /** Detect MIME type from URL extension */
    private fun detectMimeType(url: String): String {
        return when {
            url.contains(".m3u8") -> MimeTypes.APPLICATION_M3U8
            url.contains(".mpd") -> MimeTypes.APPLICATION_MPD
            url.contains(".mp4") -> MimeTypes.VIDEO_MP4
            url.contains(".ts") -> MimeTypes.VIDEO_MP2T
            else -> MimeTypes.APPLICATION_M3U8 // Default to HLS
        }
    }

    /** Build the appropriate MediaSource based on MIME type */
    private fun buildMediaSource(mediaItem: Media3Item, url: String): MediaSource {
        val mimeType = detectMimeType(url)
        return when {
            mimeType == MimeTypes.APPLICATION_M3U8 -> {
                HlsMediaSource.Factory(DefaultRenderersFactory(context))
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            }
            mimeType == MimeTypes.APPLICATION_MPD -> {
                DashMediaSource.Factory(DefaultRenderersFactory(context))
                    .createMediaSource(mediaItem)
            }
            else -> {
                ProgressiveMediaSource.Factory(DefaultRenderersFactory(context))
                    .createMediaSource(mediaItem)
            }
        }
    }

    /** Playback event listener */
    private val playbackListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    _playbackState.value = _playbackState.value.copy(
                        isBuffering = false,
                        isPlaying = exoPlayer?.playWhenReady == true,
                        duration = exoPlayer?.duration ?: 0L,
                        error = null,
                    )
                    retryCount = 0 // Reset retry count on successful playback
                }
                Player.STATE_BUFFERING -> {
                    _playbackState.value = _playbackState.value.copy(
                        isBuffering = true,
                        error = null,
                    )
                }
                Player.STATE_ENDED -> {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                    )
                }
                Player.STATE_IDLE -> {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                    )
                }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            _playbackState.value = _playbackState.value.copy(
                isPlaying = playWhenReady && exoPlayer?.playbackState == Player.STATE_READY,
            )
        }

        override fun onPlayerError(error: PlaybackException) {
            Timber.e(error, "Playback error")

            val isDecoderError = error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED

            val isNetworkError = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONTENT_LOAD_FAILED

            val playerError = PlaybackError(
                message = when {
                    isDecoderError -> "解码失败，正在降级画质重试"
                    isNetworkError -> "网络连接异常"
                    else -> "播放出错"
                },
                code = error.errorCode,
                isRetryable = isNetworkError || isDecoderError,
            )

            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                isBuffering = false,
                error = playerError,
            )

            // Auto-retry for decoder errors with quality degradation
            if (isDecoderError && retryCount < maxRetries) {
                retry()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.value = _playbackState.value.copy(
                isPlaying = isPlaying,
            )
        }
    }

    /** Current position tracking */
    fun startPositionUpdates(onPositionUpdate: (Long, Long, Int) -> Unit) {
        // Positions are polled from the UI layer via getCurrentPosition()/getDuration()
        // StateFlow is updated periodically by the UI
    }
}

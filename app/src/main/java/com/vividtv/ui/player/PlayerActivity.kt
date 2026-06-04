package com.vividtv.ui.player

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.compose.material3.Text
import com.vividtv.domain.player.PlaybackManager
import com.vividtv.domain.player.PlaybackState
import com.vividtv.ui.theme.VividColors
import com.vividtv.ui.theme.VividTvTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    @Inject
    lateinit var playbackManager: PlaybackManager

    private var showControls = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val videoUrl = intent.getStringExtra("video_url") ?: ""
        val videoTitle = intent.getStringExtra("video_title") ?: ""
        val isLive = intent.getBooleanExtra("is_live", false)

        setContent {
            VividTvTheme {
                PlayerScreen(
                    videoUrl = videoUrl,
                    videoTitle = videoTitle,
                    isLive = isLive,
                    playbackManager = playbackManager,
                    onClose = { finish() },
                    controlsVisible = showControls,
                )
            }
        }
    }

    // 监听遥控器按键
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_SPACE -> {
                // OK 键 → 切换控制条显示 / 如果控制条已显示则暂停/播放
                showControls = !showControls
                if (showControls) {
                    // 控制条刚显示，只是切换，不做播放暂停
                }
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                playbackManager.play()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                playbackManager.pause()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                // 左方向键：快退 10 秒
                val pos = playbackManager.getCurrentPosition()
                playbackManager.seekTo((pos - 10000).coerceAtLeast(0))
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // 右方向键：快进 10 秒
                val pos = playbackManager.getCurrentPosition()
                val dur = playbackManager.getDuration()
                playbackManager.seekTo((pos + 10000).coerceAtMost(dur))
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun PlayerScreen(
    videoUrl: String,
    videoTitle: String,
    isLive: Boolean,
    playbackManager: PlaybackManager,
    onClose: () -> Unit,
    controlsVisible: Boolean,
) {
    val playbackState by playbackManager.playbackState.collectAsState()
    var showControls by remember { mutableStateOf(true) }

    // 同步 Activity 的 controlsVisible 状态
    LaunchedEffect(controlsVisible) {
        showControls = controlsVisible
    }

    // Auto-hide controls after 5 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            kotlinx.coroutines.delay(5000)
            showControls = false
        }
    }

    // Start playback when screen is ready
    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotEmpty()) {
            playbackManager.play(
                url = videoUrl,
                mediaTitle = videoTitle,
                isLive = isLive,
            )
        }
    }

    val exoPlayer = playbackManager.getExoPlayer()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VividColors.BackgroundDarkest),
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = false
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Top info bar
        if (showControls) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VividColors.scrim)
                    .padding(16.dp),
            ) {
                Text(
                    text = videoTitle,
                    color = VividColors.TextPrimary,
                    fontSize = 18.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isLive) "直播" else playbackState.currentQuality,
                    color = VividColors.TextSecondary,
                    fontSize = 14.sp,
                )
            }
        }

        // Bottom controls
        if (showControls) {
            PlayerControlsRow(
                playbackState = playbackState,
                onPlayPause = {
                    if (playbackState.isPlaying) playbackManager.pause()
                    else playbackManager.play()
                },
                onClose = onClose,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(VividColors.scrim)
                    .padding(16.dp),
            )
        }

        // Error overlay
        playbackState.error?.let { error ->
            PlayerErrorOverlay(
                message = error.message,
                isRetryable = error.isRetryable,
                onRetry = { playbackManager.retry() },
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun PlayerControlsRow(
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (playbackState.isPlaying) "⏸ 暂停" else "▶ 播放",
            color = VividColors.TextPrimary,
            fontSize = 16.sp,
            modifier = Modifier.focusable(true),
        )
        Text(
            text = formatDuration(playbackState.currentPosition),
            color = VividColors.TextSecondary,
            fontSize = 14.sp,
        )
        Text(
            text = "× 关闭",
            color = VividColors.TextSecondary,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun PlayerErrorOverlay(
    message: String,
    isRetryable: Boolean,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                color = VividColors.Error,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (isRetryable) {
                Text(
                    text = "按 OK 重试",
                    color = VividColors.AccentBlue,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

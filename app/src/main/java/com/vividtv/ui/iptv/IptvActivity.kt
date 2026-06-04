package com.vividtv.ui.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Text
import com.vividtv.data.model.IptvChannel
import com.vividtv.data.source.iptv.IptvSourceStrategy
import com.vividtv.ui.common.dpadFocusCard
import com.vividtv.ui.common.rememberFocusHandle
import com.vividtv.ui.theme.VividColors
import com.vividtv.ui.theme.VividTvTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class IptvActivity : ComponentActivity() {

    @Inject
    lateinit var iptvSource: IptvSourceStrategy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VividTvTheme {
                IptvScreen()
            }
        }
    }
}

@Composable
fun IptvScreen(
    viewModel: IptvViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(VividColors.BackgroundDarkest),
    ) {
        // Channel list (left panel)
        ChannelListPanel(
            channels = uiState.channels,
            selectedChannelId = uiState.selectedChannel?.id,
            onChannelSelected = { viewModel.selectChannel(it) },
            modifier = Modifier
                .width(400.dp)
                .fillMaxHeight(),
        )

        // Channel preview (right panel)
        ChannelPreviewPanel(
            selectedChannel = uiState.selectedChannel,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun ChannelListPanel(
    channels: List<IptvChannel>,
    selectedChannelId: String?,
    onChannelSelected: (IptvChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(VividColors.BackgroundDark)) {
        Text(
            text = "📺 直播频道",
            color = VividColors.TextPrimary,
            fontSize = 20.sp,
            modifier = Modifier.padding(16.dp),
        )

        if (channels.isEmpty()) {
            Text(
                text = "暂无频道\n按 OK 刷新",
                color = VividColors.TextSecondary,
                fontSize = 16.sp,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                items(channels, key = { it.id }) { channel ->
                    ChannelRow(
                        channel = channel,
                        isSelected = channel.id == selectedChannelId,
                        onClick = { onChannelSelected(channel) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(
    channel: IptvChannel,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val focusHandle = rememberFocusHandle()
    val bgColor = when {
        isSelected -> VividColors.BackgroundCardFocused
        focusHandle.isFocused -> VividColors.BackgroundCardFocused.copy(alpha = 0.5f)
        else -> VividColors.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = channel.name,
            color = if (isSelected) VividColors.AccentBlue else VividColors.TextPrimary,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Text(
                text = "▶",
                color = VividColors.AccentBlue,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun ChannelPreviewPanel(
    selectedChannel: IptvChannel?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(VividColors.BackgroundDarkest),
        contentAlignment = Alignment.Center,
    ) {
        if (selectedChannel != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = selectedChannel.name,
                    color = VividColors.TextPrimary,
                    fontSize = 24.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "按 OK 播放",
                    color = VividColors.AccentBlue,
                    fontSize = 18.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "直播中",
                    color = VividColors.AccentRed,
                    fontSize = 14.sp,
                )
            }
        } else {
            Text(
                text = "选择一个频道开始观看",
                color = VividColors.TextSecondary,
                fontSize = 18.sp,
            )
        }
    }
}

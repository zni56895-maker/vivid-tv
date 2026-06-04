package com.vividtv.ui.detail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.vividtv.data.model.MediaItem
import com.vividtv.ui.theme.VividColors
import com.vividtv.ui.theme.VividTvTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VividTvTheme {
                DetailContent()
            }
        }
    }
}

@Composable
fun DetailContent() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VividColors.BackgroundDarkest)
            .verticalScroll(scrollState),
    ) {
        // Backdrop
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(VividColors.BackgroundDark),
        ) {
            // Placeholder for backdrop image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VividColors.BackgroundMedium),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "剧集海报",
                    color = VividColors.TextDisabled,
                )
            }
        }

        // Info section
        DetailInfoSection()
    }
}

@Composable
private fun DetailInfoSection() {
    Column(
        modifier = Modifier.padding(24.dp),
    ) {
        // Title
        Text(
            text = "剧集名称",
            color = VividColors.TextPrimary,
            fontSize = 28.sp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Meta info
        Text(
            text = "2024 · 动作 · ⭐ 8.5",
            color = VividColors.TextSecondary,
            fontSize = 16.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Play button
        Text(
            text = "▶ 播放",
            color = VividColors.AccentRed,
            fontSize = 20.sp,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Description
        Text(
            text = "剧集描述信息将从数据源加载。这里显示的是占位内容。实际应用中将从 VOD 或海外节点获取完整的描述、演员、评价等信息。",
            color = VividColors.TextSecondary,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

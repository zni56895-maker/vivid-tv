package com.vividtv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text as TvText
import coil.compose.AsyncImage
import com.vividtv.data.model.MediaItem
import com.vividtv.data.model.MediaRow
import com.vividtv.ui.common.dpadFocusCard
import com.vividtv.ui.common.rememberFocusHandle
import com.vividtv.ui.theme.VividColors

/**
 * 主页网格布局 - 4列 x N行
 * 使用 Leanback 风格的 LazyColumn + LazyRow 实现
 * D-pad 焦点通过 focusable + dpadFocusCard 管理
 */
@Composable
fun MediaContentGrid(
    rows: List<MediaRow>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VividColors.BackgroundDarkest)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(rows) { row ->
            MediaRowSection(
                row = row,
                onItemClick = onItemClick,
            )
        }
        // Bottom padding for scroll
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MediaRowSection(
    row: MediaRow,
    onItemClick: (MediaItem) -> Unit,
) {
    Column {
        // Row title
        TvText(
            text = row.title,
            color = VividColors.TextPrimary,
            fontSize = 22.sp,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        // Row items (horizontal scrolling)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = row.items,
                key = { it.id },
            ) { item ->
                MediaCard(
                    item = item,
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}

/**
 * 媒体卡片 - 4列网格中的单张
 * Focus: 发光边框动画（不缩放）
 * Aspect ratio: 16:9
 */
@Composable
private fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusHandle = rememberFocusHandle()

    Column(
        modifier = modifier
            .width(280.dp)
            .clickable(onClick = onClick)
            .focusable(true, focusHandle.interactionSource)
            .dpadFocusCard(isFocused = focusHandle.isFocused)
            .clip(RoundedCornerShape(8.dp))
            .background(VividColors.BackgroundCard),
    ) {
        // Poster image
        AsyncImage(
            model = item.posterUrl.ifEmpty { null },
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(VividColors.BackgroundMedium),
            contentScale = ContentScale.Crop,
        )

        // Title
        TvText(
            text = item.title,
            color = VividColors.TextPrimary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )

        // Year & rating
        if (item.year > 0) {
            TvText(
                text = "${item.year} · ⭐ ${item.rating}",
                color = VividColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

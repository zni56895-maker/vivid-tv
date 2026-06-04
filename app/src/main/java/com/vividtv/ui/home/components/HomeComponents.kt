package com.vividtv.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.vividtv.data.model.MediaItem
import com.vividtv.data.model.MediaRow
import com.vividtv.ui.common.dpadFocusCard
import com.vividtv.ui.common.rememberFocusHandle
import com.vividtv.ui.home.HomeUiState
import com.vividtv.ui.theme.VividColors

/**
 * 仿爱奇艺 TV 版首页 — 顶部分类导航 + 轮播 Banner + 分类卡片行
 */

// ── 分类导航 ──
val categories = listOf(
    null to "🏠 首页",
    "电影" to "🎬 电影",
    "电视剧" to "📺 电视剧",
    "动漫" to "📽️ 动漫",
    "综艺" to "🎭 综艺",
    "纪录片" to "🌍 纪录",
    "少儿" to "🧒 少儿",
)

@Composable
fun TopCategoryBar(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(VividColors.BackgroundDark)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Logo
        Text(
            text = "Vivid TV",
            color = VividColors.AccentRed,
            fontSize = 22.sp,
            modifier = Modifier.align(Alignment.CenterVertically),
        )

        Spacer(modifier = Modifier.width(24.dp))

        categories.forEach { (cat, label) ->
            val isSelected = selectedCategory == cat
            val bgColor = if (isSelected) VividColors.AccentRed.copy(alpha = 0.2f)
            else VividColors.Transparent
            val textColor = if (isSelected) VividColors.AccentRed else VividColors.TextSecondary

            Text(
                text = label,
                color = textColor,
                fontSize = 16.sp,
                modifier = Modifier
                    .background(bgColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .focusable(true),
            )
        }
    }
}

// ── Banner 轮播 ──
@Composable
fun BannerCarousel(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(VividColors.BackgroundDark),
    ) {
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("🔥 热门推荐", color = VividColors.TextDisabled, fontSize = 18.sp)
            }
        } else {
            LazyRow {
                items(items) { item ->
                    BannerCard(item = item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}

@Composable
private fun BannerCard(
    item: MediaItem,
    onClick: () -> Unit,
) {
    val focusHandle = rememberFocusHandle()

    Box(
        modifier = Modifier
            .width(600.dp)
            .fillMaxHeight()
            .padding(8.dp)
            .focusable(true, focusHandle.interactionSource)
            .dpadFocusCard(isFocused = focusHandle.isFocused)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        AsyncImage(
            model = item.posterUrl.ifEmpty { null },
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // 底部渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(VividColors.scrim)
                .padding(16.dp),
        ) {
            Column {
                Text(item.title, color = VividColors.TextPrimary, fontSize = 20.sp)
                item.description.take(40).let { desc ->
                    if (desc.isNotEmpty()) {
                        Text(desc, color = VividColors.TextSecondary, fontSize = 14.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ── 卡片行（分类内容）──
@Composable
fun MediaCardRow(
    row: MediaRow,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 8.dp)) {
        // 行标题（Banner 行不显示标题）
        if (row.title != "__banner__") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.title,
                    color = VividColors.TextPrimary,
                    fontSize = 20.sp,
                )
                Text(
                    text = "更多 →",
                    color = VividColors.AccentBlue,
                    fontSize = 14.sp,
                )
            }
        }

        // Banner 处理
        if (row.title == "__banner__") {
            BannerCarousel(items = row.items, onItemClick = onItemClick)
        } else {
            // 卡片横向滚动
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(row.items, key = { it.id }) { item ->
                    MediaCard(item = item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}

// ── 大卡片（320dp 宽）──
@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusHandle = rememberFocusHandle()

    Column(
        modifier = modifier
            .width(320.dp)
            .focusable(true, focusHandle.interactionSource)
            .dpadFocusCard(isFocused = focusHandle.isFocused)
            .clip(RoundedCornerShape(10.dp))
            .background(VividColors.BackgroundCard),
    ) {
        // 海报
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            AsyncImage(
                model = item.posterUrl.ifEmpty { null },
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
                contentScale = ContentScale.Crop,
            )
            // 评分角标
            if (item.rating > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(VividColors.AccentRed.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("⭐ ${item.rating}", color = VividColors.TextPrimary, fontSize = 12.sp)
                }
            }
        }

        // 信息
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = item.title,
                color = VividColors.TextPrimary, fontSize = 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                if (item.year > 0) {
                    Text("${item.year}", color = VividColors.TextSecondary, fontSize = 12.sp)
                }
                if (item.category.isNotEmpty()) {
                    Text(item.category, color = VividColors.AccentBlue.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        }
    }
}

// ── 首页主内容 ──
@Composable
fun HomeContent(
    state: HomeUiState,
    onItemClick: (MediaItem) -> Unit,
    onRetry: () -> Unit,
    onCategorySelected: (String?) -> Unit,
    selectedCategory: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 分类导航
        TopCategoryBar(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
        )

        // 主内容
        when {
            state.isLoading -> CategoryLoadingState()
            state.error != null -> CategoryErrorState(state.error!!, onRetry)
            state.rows.isEmpty() -> CategoryEmptyState()
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.rows) { row ->
                        MediaCardRow(row = row, onItemClick = onItemClick)
                    }
                    // 底部留白
                    item { Spacer(Modifier.height(48.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CategoryLoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("正在加载…", color = VividColors.TextSecondary, fontSize = 18.sp)
    }
}

@Composable
private fun CategoryErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = VividColors.Error, fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))
        Text("按 OK 重试", color = VividColors.AccentBlue, fontSize = 16.sp)
    }
}

@Composable
private fun CategoryEmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("暂无内容", color = VividColors.TextSecondary, fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text("按 OK 刷新", color = VividColors.TextDisabled, fontSize = 14.sp)
        }
    }
}

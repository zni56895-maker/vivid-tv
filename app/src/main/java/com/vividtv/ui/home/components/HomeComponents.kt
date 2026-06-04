package com.vividtv.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.vividtv.data.model.MediaItem
import com.vividtv.data.model.MediaRow
import com.vividtv.ui.common.rememberFocusHandle
import com.vividtv.ui.home.HomeUiState
import com.vividtv.ui.theme.VividColors

private val catItems = listOf(
    null to "首页", "电影" to "电影", "电视剧" to "电视剧",
    "动漫" to "动漫", "综艺" to "综艺", "纪录片" to "纪录片",
)

@Composable
fun TopCategoryBar(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141414))
            .padding(top = 14.dp, bottom = 0.dp),
    ) {
        // Logo + 分类
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Vivid TV Logo
            Box(
                modifier = Modifier
                    .background(VividColors.AccentRed, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "VIVID",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
            Text(
                text = " TV",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
            )
        }

        // 分类导航选项卡
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            catItems.forEach { (cat, label) ->
                val isSelected = selectedCategory == cat
                Column(
                    modifier = Modifier
                        .clickable { onCategorySelected(cat) }
                        .padding(horizontal = 14.dp, vertical = 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF888888),
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    // 选中下划线指示器
                    Box(
                        modifier = Modifier
                            .width(if (isSelected) 24.dp else 0.dp)
                            .height(3.dp)
                            .background(
                                if (isSelected) VividColors.AccentRed else Color.Transparent,
                                RoundedCornerShape(2.dp),
                            ),
                    )
                }
            }
        }

        // 底部分隔线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color(0xFF2A2A2A)),
        )
    }
}

// Banner
@Composable
fun BannerSection(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
    ) {
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🔥 热门推荐", color = Color(0xFF555555), fontSize = 20.sp)
            }
        } else {
            LazyRow {
                items(items.take(6)) { item ->
                    BannerCard(item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}

@Composable
private fun BannerCard(item: MediaItem, onClick: () -> Unit) {
    val handle = rememberFocusHandle()
    val animatedScale by animateFloatAsState(
        targetValue = if (handle.isFocused) 1.03f else 1f, label = "bannerScale",
    )

    Box(
        modifier = Modifier
            .width(380.dp)
            .fillMaxHeight()
            .padding(8.dp)
            .clickable { onClick() }
            .focusable(true, handle.interactionSource)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                ),
        ) {
            AsyncImage(
                model = item.posterUrl.ifEmpty { null },
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // 渐变遮罩（从上到下）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xE6141414)),
                        ),
                    ),
            )

            // 标题 + 评分
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.rating > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐", fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", item.rating),
                            color = Color(0xFFFFAA00),
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

// 分类行
@Composable
fun MediaSection(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        // 行标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "更多 ›",
                color = Color(0xFF888888),
                fontSize = 14.sp,
            )
        }

        // 卡片行
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.id }) { item ->
                MediaCard(item, onClick = { onItemClick(item) })
            }
        }
    }
}

// 卡片
@Composable
fun MediaCard(item: MediaItem, onClick: () -> Unit) {
    val handle = rememberFocusHandle()
    val animatedElevation by animateDpAsState(
        targetValue = if (handle.isFocused) 16.dp else 0.dp, label = "cardElevation",
    )
    val animatedBorderAlpha by animateFloatAsState(
        targetValue = if (handle.isFocused) 1f else 0f, label = "cardBorder",
    )

    Column(
        modifier = Modifier
            .width(280.dp)
            .clickable { onClick() }
            .focusable(true, handle.interactionSource)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E)),
    ) {
        // 海报
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            AsyncImage(
                model = item.posterUrl.ifEmpty { null },
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                contentScale = ContentScale.Crop,
            )

            // 焦点边框
            if (handle.isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .border(
                            width = 3.dp,
                            color = VividColors.AccentRed.copy(alpha = animatedBorderAlpha),
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                        ),
                )
            }

            // 评分角标
            if (item.rating > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            Color(0xCC141414),
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐", fontSize = 10.sp)
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", item.rating),
                            color = Color(0xFFFFAA00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // 信息
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = item.title,
                color = Color(0xFFE8E8E8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.year > 0) {
                    Text(
                        text = "${item.year}",
                        color = Color(0xFF777777),
                        fontSize = 12.sp,
                    )
                }
                if (item.year > 0 && item.category.isNotEmpty()) {
                    Text(
                        text = " · ",
                        color = Color(0xFF555555),
                        fontSize = 12.sp,
                    )
                }
                if (item.category.isNotEmpty()) {
                    Text(
                        text = item.category,
                        color = Color(0xFF777777),
                        fontSize = 12.sp,
                    )
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
    Column(modifier = modifier.fillMaxSize().background(Color(0xFF141414))) {
        TopCategoryBar(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
        )

        when {
            state.isLoading -> LoadingView()
            state.error != null -> ErrorView(state.error!!, onRetry)
            state.rows.isEmpty() -> EmptyView()
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    state.rows.forEachIndexed { index, row ->
                        if (row.title == "__banner__") {
                            item(key = "__banner__") {
                                BannerSection(row.items, onItemClick)
                            }
                        } else {
                            item(key = row.title) {
                                MediaSection(
                                    title = row.title,
                                    items = row.items,
                                    onItemClick = onItemClick,
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("加载中…", color = Color(0xFF666666), fontSize = 16.sp)
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = VividColors.Error, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))
        Text("重试", color = VividColors.AccentRed, fontSize = 16.sp)
    }
}

@Composable
private fun EmptyView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("暂无内容", color = Color(0xFF555555), fontSize = 18.sp)
    }
}

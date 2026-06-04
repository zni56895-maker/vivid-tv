package com.vividtv.ui.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Text
import com.vividtv.ui.theme.VividColors

@Composable
fun TopNavBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // App Logo / Title
        Text(
            text = "Vivid TV",
            color = VividColors.AccentRed,
            fontSize = 28.sp,
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Navigation items
        NavItem("首页", isSelected = true)
        Spacer(modifier = Modifier.width(24.dp))
        NavItem("直播")
        Spacer(modifier = Modifier.width(24.dp))
        NavItem("搜索")
    }
}

@Composable
private fun NavItem(label: String, isSelected: Boolean = false) {
    val color = if (isSelected) VividColors.AccentBlue else VividColors.TextSecondary
    Text(
        text = label,
        color = color,
        fontSize = 18.sp,
    )
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "正在加载…",
            color = VividColors.TextSecondary,
            fontSize = 18.sp,
        )
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = VividColors.Error,
            fontSize = 18.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "按 OK 重试",
            color = VividColors.AccentBlue,
            fontSize = 16.sp,
        )
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "暂无内容",
                color = VividColors.TextSecondary,
                fontSize = 20.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "按 OK 刷新",
                color = VividColors.TextDisabled,
                fontSize = 14.sp,
            )
        }
    }
}

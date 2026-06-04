package com.vividtv.ui.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vividtv.ui.home.components.HomeContent
import com.vividtv.ui.theme.VividColors
import com.vividtv.ui.theme.VividTvTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VividTvTheme {
                HomeScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VividColors.BackgroundDarkest),
    ) {
        HomeContent(
            state = uiState,
            onItemClick = { viewModel.onMediaItemClicked(it) },
            onRetry = { viewModel.loadHome() },
            onCategorySelected = { viewModel.selectCategory(it) },
            selectedCategory = uiState.selectedCategory,
        )
    }
}

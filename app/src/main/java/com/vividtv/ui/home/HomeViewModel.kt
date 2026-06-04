package com.vividtv.ui.home

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vividtv.data.model.MediaItem
import com.vividtv.data.model.MediaRow
import com.vividtv.data.repository.MediaRepository
import com.vividtv.ui.detail.DetailActivity
import com.vividtv.ui.player.PlayerActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val rows: List<MediaRow> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true, error = null)

            mediaRepository.getHomeRows()
                .onSuccess { rows ->
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        rows = rows,
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        error = throwable.message ?: "加载失败",
                    )
                }
        }
    }

    fun onMediaItemClicked(item: MediaItem) {
        // TODO: Navigate using context properly
    }
}

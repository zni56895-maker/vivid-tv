package com.vividtv.ui.home

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vividtv.data.model.MediaItem
import com.vividtv.data.model.MediaRow
import com.vividtv.data.repository.MediaRepository
import com.vividtv.data.updater.AppUpdater
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
    private val application: Application,
    private val mediaRepository: MediaRepository,
    private val appUpdater: AppUpdater,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
        checkForUpdate()
    }

    private fun checkForUpdate() {
        viewModelScope.launch {
            val update = appUpdater.checkForUpdate()
            if (update.hasUpdate) {
                appUpdater.downloadUpdate(update.apkUrl)
            }
        }
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
        val context = getApplication<Application>()
        val streamUrl = item.streamUrls.values.firstOrNull() ?: return

        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra("video_url", streamUrl)
            putExtra("video_title", item.title)
            putExtra("is_live", item.isLive)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

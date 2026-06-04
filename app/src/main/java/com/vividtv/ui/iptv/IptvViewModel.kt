package com.vividtv.ui.iptv

import androidx.lifecycle.ViewModel
import com.vividtv.data.model.IptvChannel
import com.vividtv.data.source.iptv.IptvSourceStrategy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class IptvUiState(
    val channels: List<IptvChannel> = emptyList(),
    val selectedChannel: IptvChannel? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class IptvViewModel @Inject constructor(
    private val iptvSource: IptvSourceStrategy,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IptvUiState())
    val uiState: StateFlow<IptvUiState> = _uiState.asStateFlow()

    fun selectChannel(channel: IptvChannel) {
        _uiState.value = _uiState.value.copy(selectedChannel = channel)
    }
}

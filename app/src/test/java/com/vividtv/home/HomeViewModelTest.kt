package com.vividtv.home

import com.vividtv.data.model.MediaItem
import com.vividtv.data.model.MediaRow
import com.vividtv.data.model.MediaSourceType
import com.vividtv.data.model.StreamResult
import com.vividtv.data.repository.MediaRepository
import com.vividtv.ui.home.HomeUiState
import com.vividtv.ui.home.HomeViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var repository: MediaRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        repository = mockk()
    }

    @Test
    fun `initial state is loading`() = runTest {
        coEvery { repository.getHomeRows() } returns Result.success(emptyList())
        viewModel = HomeViewModel(repository)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.rows.isEmpty())
    }

    @Test
    fun `loadHome returns rows successfully`() = runTest {
        val testRows = listOf(
            MediaRow(
                title = "Test Row",
                items = listOf(
                    MediaItem(
                        id = "1",
                        title = "Test Movie",
                        sourceType = MediaSourceType.VOD,
                    )
                ),
            )
        )
        coEvery { repository.getHomeRows() } returns Result.success(testRows)
        viewModel = HomeViewModel(repository)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.rows.size)
        assertEquals("Test Row", viewModel.uiState.value.rows[0].title)
        assertEquals("Test Movie", viewModel.uiState.value.rows[0].items[0].title)
    }

    @Test
    fun `loadHome handles error`() = runTest {
        coEvery { repository.getHomeRows() } returns Result.failure(
            RuntimeException("Network error")
        )
        viewModel = HomeViewModel(repository)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Network error", ignoreCase = true))
    }

    @Test
    fun `loadHome sets loading state correctly`() = runTest {
        coEvery { repository.getHomeRows() } returns Result.success(emptyList())
        viewModel = HomeViewModel(repository)
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }
}

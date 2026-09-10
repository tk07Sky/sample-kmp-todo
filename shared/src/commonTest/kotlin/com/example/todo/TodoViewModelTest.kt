package com.example.todo

import com.example.todo.domain.TodoFilter
import com.example.todo.ui.TodoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

    // viewModelScope は Dispatchers.Main を使うため、テスト用に差し替える
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun blank_title_is_rejected() = runTest {
        val viewModel = TodoViewModel(InMemoryTodoRepository())

        assertFalse(viewModel.add("   "), "空白のみのタイトルは追加できない")
        assertTrue(viewModel.add("買い物"), "通常のタイトルは追加できる")
    }

    @Test
    fun counts_and_filter_reflect_repository() = runTest {
        val repository = InMemoryTodoRepository()
        val viewModel = TodoViewModel(repository)

        // stateIn(WhileSubscribed) は購読者がいる間だけ動くため、収集を開始しておく
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.add("買い物")
        viewModel.add("掃除")
        testScheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.totalCount)
        assertEquals(2, viewModel.uiState.value.activeCount)

        val firstId = viewModel.uiState.value.todos.last().id
        viewModel.setDone(firstId, true)
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.activeCount)
        assertEquals(1, viewModel.uiState.value.completedCount)

        viewModel.setFilter(TodoFilter.Completed)
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(firstId), viewModel.uiState.value.todos.map { it.id })

        viewModel.clearCompleted()
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.totalCount)
    }
}

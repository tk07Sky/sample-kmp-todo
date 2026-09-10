package com.example.todo

import com.example.todo.domain.Priority
import com.example.todo.domain.TodoFilter
import com.example.todo.domain.TodoSort
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

    @Test
    fun details_are_saved_with_normalized_tags() = runTest {
        val viewModel = TodoViewModel(InMemoryTodoRepository())
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.add("買い物")
        testScheduler.advanceUntilIdle()
        val id = viewModel.uiState.value.todos.single().id

        val saved = viewModel.updateDetails(
            id = id,
            title = "  牛乳を買う  ",
            notes = "  帰りに  ",
            dueAtEpochMillis = 1_800_000_000_000,
            priority = Priority.High,
            tags = listOf(" 買い物 ", "買い物", "", "私用"),
        )
        testScheduler.advanceUntilIdle()

        assertTrue(saved)
        val todo = viewModel.uiState.value.todos.single()
        assertEquals("牛乳を買う", todo.title, "タイトルは trim される")
        assertEquals("帰りに", todo.notes)
        assertEquals(1_800_000_000_000, todo.dueAtEpochMillis)
        assertEquals(Priority.High, todo.priority)
        assertEquals(listOf("私用", "買い物"), todo.tags, "空白と重複を除き、文字コード順に並ぶ")
    }

    @Test
    fun blank_title_does_not_overwrite_details() = runTest {
        val viewModel = TodoViewModel(InMemoryTodoRepository())
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.add("買い物")
        testScheduler.advanceUntilIdle()
        val id = viewModel.uiState.value.todos.single().id

        val saved = viewModel.updateDetails(
            id = id,
            title = "   ",
            notes = "上書きされない",
            dueAtEpochMillis = null,
            priority = Priority.High,
            tags = emptyList(),
        )
        testScheduler.advanceUntilIdle()

        assertFalse(saved)
        assertEquals("買い物", viewModel.uiState.value.todos.single().title)
        assertEquals("", viewModel.uiState.value.todos.single().notes)
    }

    @Test
    fun sort_changes_the_listed_order() = runTest {
        val viewModel = TodoViewModel(InMemoryTodoRepository())
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.add("先に作った")
        viewModel.add("あとで作った")
        testScheduler.advanceUntilIdle()

        // 既定は作成が新しい順
        val newest = viewModel.uiState.value.todos.first().id
        val oldest = viewModel.uiState.value.todos.last().id

        // 先に作ったほうにだけ優先度を付けると、優先度順では入れ替わる
        viewModel.updateDetails(
            id = oldest,
            title = "先に作った",
            notes = "",
            dueAtEpochMillis = null,
            priority = Priority.High,
            tags = emptyList(),
        )
        viewModel.setSort(TodoSort.PriorityDesc)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(oldest, newest), viewModel.uiState.value.todos.map { it.id })
        assertEquals(TodoSort.PriorityDesc, viewModel.uiState.value.sort)
    }
}

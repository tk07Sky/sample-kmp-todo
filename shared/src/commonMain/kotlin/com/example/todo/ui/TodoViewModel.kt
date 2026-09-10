package com.example.todo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.data.TodoRepository
import com.example.todo.domain.Priority
import com.example.todo.domain.Todo
import com.example.todo.domain.TodoFilter
import com.example.todo.domain.TodoSort
import com.example.todo.domain.normalizeTags
import com.example.todo.domain.sortedBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 画面に表示する状態。UI は基本これだけを見る。 */
data class TodoUiState(
    val todos: List<Todo> = emptyList(),
    val filter: TodoFilter = TodoFilter.All,
    val sort: TodoSort = TodoSort.CreatedDesc,
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val isLoading: Boolean = true,
)

/**
 * Todo 一覧の状態を保持する ViewModel。
 * androidx.lifecycle の Compose Multiplatform 版を使うため、Android / iOS / Web で共通に動く。
 *
 * 詳細画面もこの ViewModel を共有する（[observeTodo] / [updateDetails]）。
 * 画面ごとに ViewModel を分けるとリポジトリの購読が二重になるうえ、
 * 一覧に戻ったときの状態も作り直しになるため、1 つにまとめている。
 */
class TodoViewModel(
    private val repository: TodoRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(TodoFilter.All)
    private val sort = MutableStateFlow(TodoSort.CreatedDesc)

    val uiState: StateFlow<TodoUiState> =
        combine(repository.observeAll(), filter, sort) { todos, activeFilter, activeSort ->
            TodoUiState(
                todos = todos.filter(activeFilter::matches).sortedBy(activeSort),
                filter = activeFilter,
                sort = activeSort,
                totalCount = todos.size,
                activeCount = todos.count { !it.isDone },
                completedCount = todos.count { it.isDone },
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodoUiState(),
        )

    /** 入力補完に出すタグの候補。 */
    val knownTags: StateFlow<List<String>> =
        repository.observeAllTags().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun setFilter(value: TodoFilter) {
        filter.value = value
    }

    fun setSort(value: TodoSort) {
        sort.value = value
    }

    /** 詳細画面で 1 件を監視する。削除されると null が流れる。 */
    fun observeTodo(id: Long) = repository.observeById(id)

    /** 空文字・空白のみのタイトルは追加しない。追加できたら true を返す。 */
    fun add(title: String, notes: String = ""): Boolean {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return false
        viewModelScope.launch { repository.add(trimmedTitle, notes.trim()) }
        return true
    }

    /**
     * 詳細画面の編集内容を保存する。タイトルが空なら何もせず false を返す。
     * タグはここで正規化するため、UI 側は入力されたままの一覧を渡してよい。
     */
    fun updateDetails(
        id: Long,
        title: String,
        notes: String,
        dueAtEpochMillis: Long?,
        priority: Priority,
        tags: List<String>,
    ): Boolean {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return false
        viewModelScope.launch {
            repository.updateDetails(
                id = id,
                title = trimmedTitle,
                notes = notes.trim(),
                dueAtEpochMillis = dueAtEpochMillis,
                priority = priority,
                tags = normalizeTags(tags),
            )
        }
        return true
    }

    fun setDone(id: Long, isDone: Boolean) {
        viewModelScope.launch { repository.setDone(id, isDone) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun clearCompleted() {
        viewModelScope.launch { repository.clearCompleted() }
    }
}

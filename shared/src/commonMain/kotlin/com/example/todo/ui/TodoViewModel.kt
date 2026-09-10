package com.example.todo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.data.TodoRepository
import com.example.todo.domain.Todo
import com.example.todo.domain.TodoFilter
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
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val isLoading: Boolean = true,
)

/**
 * Todo 一覧の状態を保持する ViewModel。
 * androidx.lifecycle の Compose Multiplatform 版を使うため、Android / iOS / Web で共通に動く。
 */
class TodoViewModel(
    private val repository: TodoRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(TodoFilter.All)

    val uiState: StateFlow<TodoUiState> =
        combine(repository.observeAll(), filter) { todos, activeFilter ->
            TodoUiState(
                todos = todos.filter(activeFilter::matches),
                filter = activeFilter,
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

    fun setFilter(value: TodoFilter) {
        filter.value = value
    }

    /** 空文字・空白のみのタイトルは追加しない。追加できたら true を返す。 */
    fun add(title: String, notes: String = ""): Boolean {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return false
        viewModelScope.launch { repository.add(trimmedTitle, notes.trim()) }
        return true
    }

    fun updateContent(id: Long, title: String, notes: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        viewModelScope.launch { repository.updateContent(id, trimmedTitle, notes.trim()) }
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

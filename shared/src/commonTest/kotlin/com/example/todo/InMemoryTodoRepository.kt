package com.example.todo

import com.example.todo.data.TodoRepository
import com.example.todo.domain.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * テスト用のメモリ実装。
 * 並び順は SQLDelight 実装の ORDER BY isDone ASC, createdAt DESC に合わせている。
 */
class InMemoryTodoRepository(initial: List<Todo> = emptyList()) : TodoRepository {

    private val state = MutableStateFlow(initial.sortedForDisplay())
    private var nextId: Long = (initial.maxOfOrNull { it.id } ?: 0L) + 1L

    /** 作成時刻はテストで順序を確定させたいので単調増加のカウンタにする。 */
    private var clock: Long = 0L

    override fun observeAll(): Flow<List<Todo>> = state.asStateFlow()

    override suspend fun add(title: String, notes: String) = mutate { current ->
        current + Todo(
            id = nextId++,
            title = title,
            notes = notes,
            isDone = false,
            createdAtEpochMillis = ++clock,
        )
    }

    override suspend fun updateContent(id: Long, title: String, notes: String) = mutate { current ->
        current.map { if (it.id == id) it.copy(title = title, notes = notes) else it }
    }

    override suspend fun setDone(id: Long, isDone: Boolean) = mutate { current ->
        current.map { if (it.id == id) it.copy(isDone = isDone) else it }
    }

    override suspend fun delete(id: Long) = mutate { current ->
        current.filterNot { it.id == id }
    }

    override suspend fun clearCompleted() = mutate { current ->
        current.filterNot { it.isDone }
    }

    private fun mutate(transform: (List<Todo>) -> List<Todo>) {
        state.value = transform(state.value).sortedForDisplay()
    }
}

private fun List<Todo>.sortedForDisplay(): List<Todo> =
    sortedWith(compareBy<Todo> { it.isDone }.thenByDescending { it.createdAtEpochMillis })

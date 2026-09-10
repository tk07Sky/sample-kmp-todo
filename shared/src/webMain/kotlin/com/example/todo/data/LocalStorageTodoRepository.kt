package com.example.todo.data

import com.example.todo.domain.Priority
import com.example.todo.domain.Todo
import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * ブラウザの localStorage に JSON で保存する [TodoRepository] 実装。
 *
 * SQLDelight の Web ドライバ（sql.js）はデータベースをメモリ上にしか持てず、
 * リロードで内容が消えてしまう。Todo アプリとしては保存されないと意味がないため、
 * Web だけは localStorage を使っている。
 * 共有しているのはドメイン・ViewModel・UI で、差し替わるのはこのクラスだけ。
 */
class LocalStorageTodoRepository(
    private val storageKey: String = "todo-kmp.todos",
) : TodoRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val state = MutableStateFlow(load())

    override fun observeAll(): Flow<List<Todo>> = state.asStateFlow()

    override fun observeAllTags(): Flow<List<String>> =
        state.map { todos -> todos.flatMap { it.tags }.distinct().sorted() }

    override suspend fun add(title: String, notes: String) = mutate { current ->
        val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        current + Todo(
            id = nextId,
            title = title,
            notes = notes,
            isDone = false,
            createdAtEpochMillis = currentTimeMillis(),
        )
    }

    override suspend fun updateDetails(
        id: Long,
        title: String,
        notes: String,
        dueAtEpochMillis: Long?,
        priority: Priority,
        tags: List<String>,
    ) = mutate { current ->
        current.map {
            if (it.id == id) {
                it.copy(
                    title = title,
                    notes = notes,
                    dueAtEpochMillis = dueAtEpochMillis,
                    priority = priority,
                    tags = tags,
                )
            } else {
                it
            }
        }
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
        val next = transform(state.value).sortedForDisplay()
        state.value = next
        localStorage.setItem(storageKey, json.encodeToString(next.map(Todo::toStored)))
    }

    private fun load(): List<Todo> {
        val raw = localStorage.getItem(storageKey) ?: return emptyList()
        // 壊れた値が入っていてもアプリが起動しなくなるのは避け、空扱いにする
        return runCatching {
            json.decodeFromString<List<StoredTodo>>(raw).map(StoredTodo::toDomain)
        }.getOrElse { emptyList() }.sortedForDisplay()
    }
}

/** SQLDelight 側の ORDER BY isDone ASC, createdAt DESC と並びを揃える。 */
private fun List<Todo>.sortedForDisplay(): List<Todo> =
    sortedWith(compareBy<Todo> { it.isDone }.thenByDescending { it.createdAtEpochMillis })

/**
 * localStorage に書く形。
 *
 * あとから足した項目には既定値を持たせてある。こうしておくと、
 * 期限・優先度・タグが入る前に保存された JSON もそのまま読める。
 */
@Serializable
private data class StoredTodo(
    val id: Long,
    val title: String,
    val notes: String,
    val isDone: Boolean,
    val createdAtEpochMillis: Long,
    val dueAtEpochMillis: Long? = null,
    val priority: Int = Priority.None.storedValue,
    val tags: List<String> = emptyList(),
)

private fun Todo.toStored() = StoredTodo(
    id = id,
    title = title,
    notes = notes,
    isDone = isDone,
    createdAtEpochMillis = createdAtEpochMillis,
    dueAtEpochMillis = dueAtEpochMillis,
    priority = priority.storedValue,
    tags = tags,
)

private fun StoredTodo.toDomain() = Todo(
    id = id,
    title = title,
    notes = notes,
    isDone = isDone,
    createdAtEpochMillis = createdAtEpochMillis,
    dueAtEpochMillis = dueAtEpochMillis,
    priority = Priority.fromStored(priority),
    tags = tags,
)

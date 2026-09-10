package com.example.todo.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import com.example.todo.db.TodoDatabase
import com.example.todo.db.TodoEntity
import com.example.todo.db.TodoQueries
import com.example.todo.domain.Priority
import com.example.todo.domain.Todo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * SQLite（SQLDelight）による [TodoRepository] 実装。Android と iOS で共有する。
 *
 * [driver] はプラットフォーム側で組み立てて渡す:
 *  - Android : AndroidSqliteDriver
 *  - iOS     : NativeSqliteDriver
 */
class SqlDelightTodoRepository(
    driver: SqlDriver,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val now: () -> Long = ::currentTimeMillis,
) : TodoRepository {

    private val queries = TodoDatabase(driver).todoQueries

    /**
     * Todo 本体とタグの対応を別々に監視し、Kotlin 側で突き合わせる。
     * Todo ごとにタグを引くと件数分のクエリになるため、対応表は 1 回で読む。
     */
    override fun observeAll(): Flow<List<Todo>> =
        combine(
            queries.selectAll().asFlow().mapToList(dispatcher),
            queries.selectAllTagLinks().asFlow().mapToList(dispatcher),
        ) { rows, links ->
            val tagsByTodoId = links.groupBy({ it.todoId }, { it.name })
            rows.map { it.toDomain(tagsByTodoId[it.id].orEmpty()) }
        }

    override fun observeAllTags(): Flow<List<String>> =
        queries.selectAllTagNames().asFlow().mapToList(dispatcher)

    override suspend fun add(title: String, notes: String) {
        withContext(dispatcher) {
            queries.insert(
                title = title,
                notes = notes,
                isDone = false,
                createdAt = now(),
                dueAt = null,
                priority = Priority.None.storedValue.toLong(),
            )
        }
    }

    override suspend fun updateDetails(
        id: Long,
        title: String,
        notes: String,
        dueAtEpochMillis: Long?,
        priority: Priority,
        tags: List<String>,
    ) {
        withContext(dispatcher) {
            queries.transaction {
                queries.updateDetails(
                    title = title,
                    notes = notes,
                    dueAt = dueAtEpochMillis,
                    priority = priority.storedValue.toLong(),
                    id = id,
                )
                queries.replaceTagsOf(id, tags)
                queries.deleteUnusedTags()
            }
        }
    }

    override suspend fun setDone(id: Long, isDone: Boolean) {
        withContext(dispatcher) {
            queries.setDone(isDone = isDone, id = id)
        }
    }

    override suspend fun delete(id: Long) {
        withContext(dispatcher) {
            queries.transaction {
                queries.deleteTagLinksByTodo(id)
                queries.deleteById(id)
                queries.deleteUnusedTags()
            }
        }
    }

    override suspend fun clearCompleted() {
        withContext(dispatcher) {
            queries.transaction {
                queries.deleteTagLinksByTodos(queries.selectCompletedIds().executeAsList())
                queries.deleteCompleted(done = true)
                queries.deleteUnusedTags()
            }
        }
    }
}

/**
 * ある Todo のタグを [tags] の内容で置き換える。
 * 呼び出し側でトランザクションを張っていることを前提とする。
 */
private fun TodoQueries.replaceTagsOf(todoId: Long, tags: List<String>) {
    deleteTagLinksByTodo(todoId)
    tags.forEach { name ->
        // 既にあれば無視される。そのうえで id を引き直して対応表に入れる。
        insertTag(name)
        val tagId = selectTagIdByName(name).executeAsOneOrNull() ?: return@forEach
        insertTagLink(todoId = todoId, tagId = tagId)
    }
}

private fun TodoEntity.toDomain(tags: List<String>): Todo = Todo(
    id = id,
    title = title,
    notes = notes,
    isDone = isDone,
    createdAtEpochMillis = createdAt,
    dueAtEpochMillis = dueAt,
    priority = Priority.fromStored(priority.toInt()),
    tags = tags,
)

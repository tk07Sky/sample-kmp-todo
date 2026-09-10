package com.example.todo.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import com.example.todo.db.TodoDatabase
import com.example.todo.db.TodoEntity
import com.example.todo.domain.Todo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    override fun observeAll(): Flow<List<Todo>> =
        queries.selectAll()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(title: String, notes: String) {
        withContext(dispatcher) {
            queries.insert(title = title, notes = notes, isDone = false, createdAt = now())
        }
    }

    override suspend fun updateContent(id: Long, title: String, notes: String) {
        withContext(dispatcher) {
            queries.updateContent(title = title, notes = notes, id = id)
        }
    }

    override suspend fun setDone(id: Long, isDone: Boolean) {
        withContext(dispatcher) {
            queries.setDone(isDone = isDone, id = id)
        }
    }

    override suspend fun delete(id: Long) {
        withContext(dispatcher) {
            queries.deleteById(id)
        }
    }

    override suspend fun clearCompleted() {
        withContext(dispatcher) {
            queries.deleteCompleted(done = true)
        }
    }
}

private fun TodoEntity.toDomain(): Todo = Todo(
    id = id,
    title = title,
    notes = notes,
    isDone = isDone,
    createdAtEpochMillis = createdAt,
)

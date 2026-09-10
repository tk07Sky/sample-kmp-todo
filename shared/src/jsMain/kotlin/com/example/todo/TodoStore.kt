package com.example.todo

import com.example.todo.data.LocalStorageTodoRepository
import com.example.todo.data.TodoRepository
import com.example.todo.domain.Todo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlin.js.ExperimentalJsExport
import kotlin.js.Promise

/**
 * TypeScript 側から使うための facade。
 *
 * `@JsExport` は suspend 関数・Flow・Long を扱えないため、ここで変換する。
 *  - 購読    : Flow -> コールバック（戻り値の関数で解除する）
 *  - 書き込み: suspend -> Promise
 *  - ID      : Long -> String（JS の number では 53bit を超える値を表現できないため）
 *
 * このクラスと [TodoDto] だけが JS との境界で、
 * ドメイン・リポジトリの実装は Android / iOS と共通のものをそのまま使う。
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
class TodoStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val repository: TodoRepository = LocalStorageTodoRepository()

    /**
     * 一覧の変化を受け取る。呼ばれた時点の内容が即座に一度通知される。
     * 戻り値の関数を呼ぶと購読を解除する（React の useEffect のクリーンアップに渡せる）。
     */
    fun subscribe(onChange: (Array<TodoDto>) -> Unit): () -> Unit {
        val job = scope.launch {
            repository.observeAll().collect { todos ->
                onChange(todos.map { it.toDto() }.toTypedArray())
            }
        }
        return { job.cancel() }
    }

    /** タイトルが空白のみの場合は追加しない。追加できたかどうかを返す。 */
    fun add(title: String, notes: String): Promise<Boolean> = scope.promise {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return@promise false
        repository.add(trimmed, notes.trim())
        true
    }

    fun updateContent(id: String, title: String, notes: String): Promise<Boolean> = scope.promise {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return@promise false
        repository.updateContent(id.toLong(), trimmed, notes.trim())
        true
    }

    fun setDone(id: String, isDone: Boolean): Promise<Unit> = scope.promise {
        repository.setDone(id.toLong(), isDone)
    }

    fun remove(id: String): Promise<Unit> = scope.promise {
        repository.delete(id.toLong())
    }

    fun clearCompleted(): Promise<Unit> = scope.promise {
        repository.clearCompleted()
    }
}

/** JS へ渡す Todo の表現。 */
@OptIn(ExperimentalJsExport::class)
@JsExport
class TodoDto internal constructor(
    val id: String,
    val title: String,
    val notes: String,
    val isDone: Boolean,
    /** エポックミリ秒。JS 側では new Date(createdAt) で扱える。 */
    val createdAt: Double,
)

private fun Todo.toDto(): TodoDto = TodoDto(
    id = id.toString(),
    title = title,
    notes = notes,
    isDone = isDone,
    createdAt = createdAtEpochMillis.toDouble(),
)

package com.example.todo

import com.example.todo.data.LocalStorageTodoRepository
import com.example.todo.data.TodoRepository
import com.example.todo.domain.Priority
import com.example.todo.domain.Todo
import com.example.todo.domain.normalizeTags
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
 *  - 一覧    : List -> Array
 *  - 優先度  : enum -> "high" / "medium" / "low" / "none"（TS 側で読める形にする）
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

    /** 使われているタグ名の一覧を受け取る。入力補完に使う。 */
    fun subscribeTags(onChange: (Array<String>) -> Unit): () -> Unit {
        val job = scope.launch {
            repository.observeAllTags().collect { tags -> onChange(tags.toTypedArray()) }
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

    /**
     * 詳細画面の編集内容を保存する。
     * [dueAt] は null で期限なし、[priority] は "high" / "medium" / "low" / "none"。
     * タグはここで正規化するので、TS 側は入力されたままの配列を渡してよい。
     */
    fun updateDetails(
        id: String,
        title: String,
        notes: String,
        dueAt: Double?,
        priority: String,
        tags: Array<String>,
    ): Promise<Boolean> = scope.promise {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return@promise false
        repository.updateDetails(
            id = id.toLong(),
            title = trimmed,
            notes = notes.trim(),
            dueAtEpochMillis = dueAt?.toLong(),
            priority = priorityFromJs(priority),
            tags = normalizeTags(tags.toList()),
        )
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
    /** 期限のエポックミリ秒。未設定なら null。 */
    val dueAt: Double?,
    /** "high" / "medium" / "low" / "none"。 */
    val priority: String,
    val tags: Array<String>,
)

private fun Todo.toDto(): TodoDto = TodoDto(
    id = id.toString(),
    title = title,
    notes = notes,
    isDone = isDone,
    createdAt = createdAtEpochMillis.toDouble(),
    dueAt = dueAtEpochMillis?.toDouble(),
    priority = priority.toJs(),
    tags = tags.toTypedArray(),
)

private fun Priority.toJs(): String = when (this) {
    Priority.High -> "high"
    Priority.Medium -> "medium"
    Priority.Low -> "low"
    Priority.None -> "none"
}

/** 知らない値が来ても落とさず「未設定」に寄せる。 */
private fun priorityFromJs(value: String): Priority = when (value) {
    "high" -> Priority.High
    "medium" -> Priority.Medium
    "low" -> Priority.Low
    else -> Priority.None
}

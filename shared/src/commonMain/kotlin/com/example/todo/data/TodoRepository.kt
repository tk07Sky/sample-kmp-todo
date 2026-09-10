package com.example.todo.data

import com.example.todo.domain.Priority
import com.example.todo.domain.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Todo の永続化を抽象化する。
 *
 * 実装はプラットフォームごとに差し替える:
 *  - Android / iOS : [SqlDelightTodoRepository]（SQLite）
 *  - Web (wasmJs)  : LocalStorageTodoRepository（ブラウザの localStorage）
 *
 * 呼び出し側（ViewModel / UI）はこのインターフェースだけを見るため、
 * 実装の違いは共通コードに影響しない。
 */
interface TodoRepository {

    /** 全件を監視する。並び順は「未完了が先、その中では新しい順」。 */
    fun observeAll(): Flow<List<Todo>>

    /**
     * 1 件を監視する。詳細画面が使う。存在しない（削除された）場合は null を流す。
     *
     * 件数が多くない前提で [observeAll] から取り出す既定実装を置いている。
     * 実装側で専用のクエリを使いたくなったら上書きすればよい。
     */
    fun observeById(id: Long): Flow<Todo?> =
        observeAll().map { todos -> todos.find { it.id == id } }

    /** 使われているタグ名の一覧を監視する。入力補完に使う。 */
    fun observeAllTags(): Flow<List<String>>

    /** 新規追加する。title は呼び出し側で trim・空判定済みであることを前提とする。 */
    suspend fun add(title: String, notes: String)

    /**
     * 詳細画面で編集できる項目をまとめて更新する。
     * title と tags は呼び出し側で正規化済み（[com.example.todo.domain.normalizeTags]）であることを前提とする。
     */
    suspend fun updateDetails(
        id: Long,
        title: String,
        notes: String,
        dueAtEpochMillis: Long?,
        priority: Priority,
        tags: List<String>,
    )

    /** 完了・未完了を切り替える。 */
    suspend fun setDone(id: Long, isDone: Boolean)

    /** 1 件削除する。 */
    suspend fun delete(id: Long)

    /** 完了済みをまとめて削除する。 */
    suspend fun clearCompleted()
}

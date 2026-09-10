package com.example.todo.data

import com.example.todo.domain.Todo
import kotlinx.coroutines.flow.Flow

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

    /** 新規追加する。title は呼び出し側で trim・空判定済みであることを前提とする。 */
    suspend fun add(title: String, notes: String)

    /** タイトルと本文を更新する。 */
    suspend fun updateContent(id: Long, title: String, notes: String)

    /** 完了・未完了を切り替える。 */
    suspend fun setDone(id: Long, isDone: Boolean)

    /** 1 件削除する。 */
    suspend fun delete(id: Long)

    /** 完了済みをまとめて削除する。 */
    suspend fun clearCompleted()
}

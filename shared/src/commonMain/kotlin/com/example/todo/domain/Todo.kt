package com.example.todo.domain

/**
 * Todo 1 件を表すドメインモデル。
 * 永続化方式（SQLDelight / localStorage）に依存しない形にしておき、
 * プラットフォームごとの実装差を UI 層に漏らさないようにする。
 */
data class Todo(
    val id: Long,
    val title: String,
    val notes: String,
    val isDone: Boolean,
    val createdAtEpochMillis: Long,
)

/** 一覧の絞り込み条件。 */
enum class TodoFilter {
    All,
    Active,
    Completed;

    fun matches(todo: Todo): Boolean = when (this) {
        All -> true
        Active -> !todo.isDone
        Completed -> todo.isDone
    }
}

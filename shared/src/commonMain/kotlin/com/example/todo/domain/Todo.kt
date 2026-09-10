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
    /** 期限。未設定なら null。日付だけでなく時刻まで持つ。 */
    val dueAtEpochMillis: Long? = null,
    val priority: Priority = Priority.None,
    /** 付与されたタグ。表示順を安定させるため文字コードの昇順で保持する。 */
    val tags: List<String> = emptyList(),
)

/**
 * 優先度。
 *
 * [storedValue] は DB と localStorage に書く値で、大きいほど優先度が高い。
 * enum の並び順（ordinal）に依存させると、あとで要素を足したときに
 * 保存済みのデータの意味が変わってしまうため、明示的な値を持たせている。
 */
enum class Priority(val storedValue: Int) {
    High(3),
    Medium(2),
    Low(1),
    None(0);

    companion object {
        /** 未知の値は未設定として扱い、読み込みで落ちないようにする。 */
        fun fromStored(value: Int): Priority = entries.find { it.storedValue == value } ?: None
    }
}

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

/** 一覧の並び順。 */
enum class TodoSort {
    /** 作成が新しい順。既定。 */
    CreatedDesc,

    /** 期限が近い順。未設定は最後に置く。 */
    DueAsc,

    /** 優先度が高い順。未設定は最後に置く。 */
    PriorityDesc,
}

/**
 * 一覧の並び順を適用する。
 *
 * どの並び順でも「未完了が先」は共通で、完了済みが未完了より上に来ることはない。
 * 決着がつかない場合は作成が新しい順にして、並びが実行ごとに変わらないようにする。
 *
 * TypeScript 版にも同じ規則の実装がある（web/src/todoStore.ts の sortTodos）。
 * 片方だけ変えると Web の 2 実装で並びがずれるため、変更するときは両方を直すこと。
 */
fun List<Todo>.sortedBy(sort: TodoSort): List<Todo> {
    val comparator = when (sort) {
        TodoSort.CreatedDesc ->
            compareBy<Todo> { it.isDone }

        TodoSort.DueAsc ->
            // null（期限なし）を最後に送るため、まず有無で分けてから期限で比べる
            compareBy<Todo> { it.isDone }
                .thenBy { it.dueAtEpochMillis == null }
                .thenBy { it.dueAtEpochMillis ?: Long.MAX_VALUE }

        TodoSort.PriorityDesc ->
            compareBy<Todo> { it.isDone }
                .thenByDescending { it.priority.storedValue }
    }
    return sortedWith(comparator.thenByDescending { it.createdAtEpochMillis })
}

/**
 * 入力されたタグを保存できる形に整える。
 *
 * 前後の空白を落とし、空文字と重複を除いてから並べ替える。
 * 並びは文字コード（Unicode）順で、日本語の五十音順にはならない。
 * ロケールを見た並べ替えは共通コードで揃えるのが難しく、
 * TypeScript 版と結果がずれるほうが困るため、単純な比較にしている。
 * 大文字小文字は区別する（DB 側の UNIQUE 制約と揃えている）。
 *
 * TypeScript 版にも同じ規則の実装がある（web/src/todoStore.ts の normalizeTags）。
 */
fun normalizeTags(raw: List<String>): List<String> =
    raw.map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .sorted()

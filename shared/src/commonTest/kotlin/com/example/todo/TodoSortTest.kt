package com.example.todo

import com.example.todo.domain.Priority
import com.example.todo.domain.Todo
import com.example.todo.domain.TodoSort
import com.example.todo.domain.normalizeTags
import com.example.todo.domain.sortedBy
import kotlin.test.Test
import kotlin.test.assertEquals

class TodoSortTest {

    private fun todo(
        id: Long,
        createdAt: Long = id,
        isDone: Boolean = false,
        dueAt: Long? = null,
        priority: Priority = Priority.None,
    ) = Todo(
        id = id,
        title = "todo $id",
        notes = "",
        isDone = isDone,
        createdAtEpochMillis = createdAt,
        dueAtEpochMillis = dueAt,
        priority = priority,
    )

    @Test
    fun created_desc_puts_newest_first() {
        val todos = listOf(todo(id = 1, createdAt = 10), todo(id = 2, createdAt = 30), todo(id = 3, createdAt = 20))

        assertEquals(listOf(2L, 3L, 1L), todos.sortedBy(TodoSort.CreatedDesc).map { it.id })
    }

    @Test
    fun due_asc_puts_nearest_first_and_undated_last() {
        val todos = listOf(
            todo(id = 1, dueAt = null),
            todo(id = 2, dueAt = 300),
            todo(id = 3, dueAt = 100),
        )

        assertEquals(listOf(3L, 2L, 1L), todos.sortedBy(TodoSort.DueAsc).map { it.id })
    }

    @Test
    fun priority_desc_puts_high_first_and_none_last() {
        val todos = listOf(
            todo(id = 1, priority = Priority.None),
            todo(id = 2, priority = Priority.Low),
            todo(id = 3, priority = Priority.High),
            todo(id = 4, priority = Priority.Medium),
        )

        assertEquals(listOf(3L, 4L, 2L, 1L), todos.sortedBy(TodoSort.PriorityDesc).map { it.id })
    }

    @Test
    fun completed_never_comes_before_active() {
        // 完了済みに一番近い期限を持たせても、未完了より上には来ない
        val todos = listOf(
            todo(id = 1, isDone = true, dueAt = 1, priority = Priority.High),
            todo(id = 2, isDone = false, dueAt = 999, priority = Priority.None),
        )

        TodoSort.entries.forEach { sort ->
            assertEquals(listOf(2L, 1L), todos.sortedBy(sort).map { it.id }, "並び順 $sort")
        }
    }

    @Test
    fun tags_are_trimmed_deduplicated_and_sorted() {
        assertEquals(
            listOf("あとで", "仕事", "買い物"),
            normalizeTags(listOf(" 仕事 ", "買い物", "仕事", "", "   ", "あとで")),
        )
    }
}

package com.example.todo

import com.example.todo.domain.Todo
import com.example.todo.domain.TodoFilter
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TodoFilterTest {

    private val active = Todo(1, "買い物", "", isDone = false, createdAtEpochMillis = 1)
    private val done = Todo(2, "掃除", "", isDone = true, createdAtEpochMillis = 2)

    @Test
    fun all_matches_everything() {
        assertTrue(TodoFilter.All.matches(active))
        assertTrue(TodoFilter.All.matches(done))
    }

    @Test
    fun active_matches_only_incomplete() {
        assertTrue(TodoFilter.Active.matches(active))
        assertFalse(TodoFilter.Active.matches(done))
    }

    @Test
    fun completed_matches_only_complete() {
        assertFalse(TodoFilter.Completed.matches(active))
        assertTrue(TodoFilter.Completed.matches(done))
    }
}

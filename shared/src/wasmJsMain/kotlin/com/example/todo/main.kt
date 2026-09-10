package com.example.todo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.example.todo.data.LocalStorageTodoRepository
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val repository = LocalStorageTodoRepository()
    ComposeViewport(document.body!!) {
        App(repository)
    }
}

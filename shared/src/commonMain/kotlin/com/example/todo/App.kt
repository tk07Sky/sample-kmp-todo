package com.example.todo

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todo.data.TodoRepository
import com.example.todo.ui.AppTheme
import com.example.todo.ui.TodoScreen
import com.example.todo.ui.TodoViewModel

/**
 * アプリのルート。各プラットフォームのエントリポイントから呼び出す。
 *
 * [repository] はプラットフォーム側で組み立てて渡す（手動 DI）。
 * こうすることで Android の Context 依存などを共通コードに持ち込まずに済む。
 */
@Composable
fun App(repository: TodoRepository) {
    AppTheme {
        val viewModel = viewModel { TodoViewModel(repository) }
        TodoScreen(viewModel)
    }
}

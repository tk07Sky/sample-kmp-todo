package com.example.todo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todo.data.TodoRepository
import com.example.todo.ui.AppTheme
import com.example.todo.ui.TodoDetailScreen
import com.example.todo.ui.TodoListScreen
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

        // 画面は一覧と詳細の 2 枚だけなので、ナビゲーションライブラリは入れず
        // 「今どちらを出しているか」を状態として持つだけにしている。
        // rememberSaveable なので、Android の画面回転や再生成をまたいでも開いたままになる。
        var openedTodoId by rememberSaveable { mutableStateOf<Long?>(null) }

        val id = openedTodoId
        if (id == null) {
            TodoListScreen(
                viewModel = viewModel,
                onOpenDetail = { openedTodoId = it },
            )
        } else {
            TodoDetailScreen(
                viewModel = viewModel,
                todoId = id,
                onBack = { openedTodoId = null },
            )
        }
    }
}

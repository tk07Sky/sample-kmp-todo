package com.example.todo

import androidx.compose.ui.window.ComposeUIViewController
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.todo.data.SqlDelightTodoRepository
import com.example.todo.data.TodoRepository
import com.example.todo.db.TodoDatabase
import platform.UIKit.UIViewController

/**
 * SQLite 接続はアプリで 1 つあれば足りるので、遅延初期化して使い回す。
 */
private val repository: TodoRepository by lazy {
    SqlDelightTodoRepository(
        NativeSqliteDriver(
            schema = TodoDatabase.Schema,
            name = "todo.db",
        )
    )
}

/** Swift 側（ContentView）から呼び出す Compose のエントリポイント。 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    App(repository)
}

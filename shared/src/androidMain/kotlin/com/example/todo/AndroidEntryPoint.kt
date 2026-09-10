package com.example.todo

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.todo.data.SqlDelightTodoRepository
import com.example.todo.data.TodoRepository
import com.example.todo.db.TodoDatabase

/**
 * Android 用のリポジトリを組み立てる。
 * SQLite のファイル名やドライバの選択を shared 側に閉じ込め、アプリモジュールからは隠す。
 */
fun createTodoRepository(context: Context): TodoRepository =
    SqlDelightTodoRepository(
        AndroidSqliteDriver(
            schema = TodoDatabase.Schema,
            context = context.applicationContext,
            name = "todo.db",
        )
    )

/**
 * Activity に Todo 画面を設定する。
 *
 * Compose の記述をここに置くことで、androidApp モジュールは Compose コンパイラを
 * 適用せずに済み、単なるアプリの器として保てる。
 */
fun ComponentActivity.setTodoContent(repository: TodoRepository) {
    setContent {
        App(repository)
    }
}

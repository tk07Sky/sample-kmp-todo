package com.example.todo

import android.app.Application
import com.example.todo.data.TodoRepository

/**
 * リポジトリの生存期間をプロセスに合わせるための Application。
 *
 * Activity 側で作ると画面回転のたびに SQLite 接続を開き直すことになるため、
 * ここで 1 つだけ生成して使い回す。
 */
class TodoApplication : Application() {

    val repository: TodoRepository by lazy { createTodoRepository(this) }
}

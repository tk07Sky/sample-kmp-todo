// Kotlin/JS が出力したライブラリ。ES モジュールとして型付きで import できる。
// TodoStore / TodoDto は shared/src/jsMain の @JsExport から生成されたもの。
import { TodoStore, type TodoDto } from 'todo-shared'

export type Todo = TodoDto

/** アプリ全体で 1 つ。内部で localStorage を読み書きする。 */
export const todoStore = new TodoStore()

export type Filter = 'all' | 'active' | 'completed'

export function applyFilter(todos: readonly Todo[], filter: Filter): Todo[] {
  switch (filter) {
    case 'active':
      return todos.filter((t) => !t.isDone)
    case 'completed':
      return todos.filter((t) => t.isDone)
    default:
      return [...todos]
  }
}

export function formatCreatedAt(epochMillis: number): string {
  const d = new Date(epochMillis)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

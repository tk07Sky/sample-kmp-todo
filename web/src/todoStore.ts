// Kotlin/JS が出力したライブラリ。ES モジュールとして型付きで import できる。
// TodoStore / TodoDto は shared/src/jsMain の @JsExport から生成されたもの。
import { TodoStore, type TodoDto } from 'todo-shared'

export type Todo = TodoDto

/**
 * Kotlin/JS が出力する nullable な値の型は `T | null | undefined` になっている。
 * Kotlin 側が入れるのは null だけだが、型のうえでは undefined も来うるので、
 * 期限の有無はどちらもまとめて弾ける `!= null` で判定する。
 */
export function hasDue(todo: Todo): todo is Todo & { dueAt: number } {
  return todo.dueAt != null
}

/** アプリ全体で 1 つ。内部で localStorage を読み書きする。 */
export const todoStore = new TodoStore()

export type Filter = 'all' | 'active' | 'completed'
export type Sort = 'created' | 'due' | 'priority'

/**
 * 優先度。Kotlin 側（Priority）とは文字列でやり取りしている。
 * 数値が大きいほど優先度が高く、並べ替えに使う。
 */
export type Priority = 'high' | 'medium' | 'low' | 'none'

const PRIORITY_ORDER: Record<Priority, number> = {
  high: 3,
  medium: 2,
  low: 1,
  none: 0,
}

export const PRIORITIES: { value: Priority; label: string }[] = [
  { value: 'high', label: '高' },
  { value: 'medium', label: '中' },
  { value: 'low', label: '低' },
  { value: 'none', label: 'なし' },
]

/** Kotlin から来る priority は string なので、既知の値だけ通す。 */
export function asPriority(value: string): Priority {
  return value === 'high' || value === 'medium' || value === 'low' ? value : 'none'
}

export function priorityLabel(value: Priority): string {
  return PRIORITIES.find((p) => p.value === value)?.label ?? 'なし'
}

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

/**
 * 一覧の並び順を適用する。
 *
 * Kotlin 側の sortedBy（shared/src/commonMain の domain/Todo.kt）と同じ規則:
 * どの並び順でも未完了が先で、決着がつかなければ作成が新しい順。
 * 片方だけ変えると Compose 版と並びがずれるため、変更するときは両方を直すこと。
 */
export function sortTodos(todos: readonly Todo[], sort: Sort): Todo[] {
  const byCreatedDesc = (a: Todo, b: Todo) => b.createdAt - a.createdAt
  const byDone = (a: Todo, b: Todo) => Number(a.isDone) - Number(b.isDone)

  const compare = (a: Todo, b: Todo): number => {
    const done = byDone(a, b)
    if (done !== 0) return done

    if (sort === 'due') {
      // 期限なしは最後に送る
      const aHas = hasDue(a)
      const bHas = hasDue(b)
      if (aHas !== bHas) return aHas ? -1 : 1
      if (aHas && bHas && a.dueAt !== b.dueAt) return a.dueAt - b.dueAt
    }

    if (sort === 'priority') {
      const diff = PRIORITY_ORDER[asPriority(b.priority)] - PRIORITY_ORDER[asPriority(a.priority)]
      if (diff !== 0) return diff
    }

    return byCreatedDesc(a, b)
  }

  return [...todos].sort(compare)
}

/**
 * 入力されたタグを保存できる形に整える。
 * Kotlin 側の normalizeTags と同じく、空白を落として重複を除き、文字コード順に並べる。
 */
export function normalizeTags(raw: readonly string[]): string[] {
  const trimmed = raw.map((t) => t.trim()).filter((t) => t !== '')
  return [...new Set(trimmed)].sort()
}

export function formatCreatedAt(epochMillis: number): string {
  const d = new Date(epochMillis)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/**
 * <input type="datetime-local"> が読み書きする "2026-09-30T18:00" 形式にする。
 * この入力欄はローカル時刻として扱われるため、UTC ではなくローカルの値を組み立てる。
 */
export function toDateTimeLocalValue(epochMillis: number): string {
  const d = new Date(epochMillis)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** datetime-local の値をエポックミリ秒にする。空文字と解釈できない値は null（期限なし）。 */
export function fromDateTimeLocalValue(value: string): number | null {
  if (value === '') return null
  const millis = new Date(value).getTime()
  return Number.isNaN(millis) ? null : millis
}

import { useEffect, useMemo, useState } from 'react'
import {
  applyFilter,
  formatCreatedAt,
  todoStore,
  type Filter,
  type Todo,
} from './todoStore'

const FILTERS: { value: Filter; label: string }[] = [
  { value: 'all', label: 'すべて' },
  { value: 'active', label: '未完了' },
  { value: 'completed', label: '完了' },
]

export function App() {
  const [todos, setTodos] = useState<readonly Todo[]>([])
  const [filter, setFilter] = useState<Filter>('all')
  const [draft, setDraft] = useState('')

  // Kotlin 側の Flow を購読する。戻り値が解除関数なので、そのまま cleanup に渡せる。
  useEffect(() => todoStore.subscribe(setTodos), [])

  const visible = useMemo(() => applyFilter(todos, filter), [todos, filter])
  const activeCount = todos.filter((t) => !t.isDone).length
  const completedCount = todos.length - activeCount

  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    const added = await todoStore.add(draft, '')
    if (added) setDraft('')
  }

  return (
    <div className="app">
      <header className="app-header">
        <div>
          <h1>Todo</h1>
          <p className="counts">
            未完了 {activeCount} 件 / 全 {todos.length} 件
          </p>
        </div>
        <button
          type="button"
          className="text-button"
          disabled={completedCount === 0}
          onClick={() => todoStore.clearCompleted()}
        >
          完了を削除
        </button>
      </header>

      <nav className="filters">
        {FILTERS.map(({ value, label }) => (
          <button
            key={value}
            type="button"
            className={value === filter ? 'chip chip-selected' : 'chip'}
            aria-pressed={value === filter}
            onClick={() => setFilter(value)}
          >
            {label}
          </button>
        ))}
      </nav>

      <main className="list-area">
        {visible.length === 0 ? (
          <p className="empty">{emptyMessage(filter)}</p>
        ) : (
          <ul className="todo-list">
            {visible.map((todo) => (
              <TodoRow key={todo.id} todo={todo} />
            ))}
          </ul>
        )}
      </main>

      <form className="add-form" onSubmit={submit}>
        <input
          type="text"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder="やることを入力"
          aria-label="やることを入力"
        />
        <button type="submit" className="add-button" disabled={draft.trim() === ''}>
          追加
        </button>
      </form>
    </div>
  )
}

function TodoRow({ todo }: { todo: Todo }) {
  return (
    <li className={todo.isDone ? 'todo todo-done' : 'todo'}>
      <input
        type="checkbox"
        checked={todo.isDone}
        onChange={() => todoStore.setDone(todo.id, !todo.isDone)}
        aria-label={`${todo.title} を完了にする`}
      />
      <div className="todo-body">
        <span className="todo-title">{todo.title}</span>
        {todo.notes !== '' && <span className="todo-notes">{todo.notes}</span>}
        <time className="todo-date">{formatCreatedAt(todo.createdAt)}</time>
      </div>
      <button
        type="button"
        className="icon-button"
        onClick={() => todoStore.remove(todo.id)}
        aria-label={`${todo.title} を削除`}
      >
        ✕
      </button>
    </li>
  )
}

function emptyMessage(filter: Filter): string {
  switch (filter) {
    case 'completed':
      return '完了した Todo はまだありません'
    case 'active':
      return '未完了の Todo はありません。お疲れさまでした'
    default:
      return '下の入力欄から最初の Todo を追加してください'
  }
}

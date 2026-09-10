import { useEffect, useMemo, useRef, useState } from 'react'
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
  // 編集ダイアログの対象。null なら閉じている。
  const [editing, setEditing] = useState<Todo | null>(null)

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
              <TodoRow key={todo.id} todo={todo} onEdit={() => setEditing(todo)} />
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

      {editing && (
        <EditTodoDialog
          key={editing.id}
          todo={editing}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  )
}

function TodoRow({ todo, onEdit }: { todo: Todo; onEdit: () => void }) {
  return (
    <li className={todo.isDone ? 'todo todo-done' : 'todo'}>
      <input
        type="checkbox"
        checked={todo.isDone}
        onChange={() => todoStore.setDone(todo.id, !todo.isDone)}
        aria-label={`${todo.title} を完了にする`}
      />
      {/* 本文は button で包まない。ここを押せるようにするとテキスト選択が効かなくなるため、
          編集は右側の独立したボタンから開く。 */}
      <div className="todo-body">
        <span className="todo-title">{todo.title}</span>
        {todo.notes !== '' && <span className="todo-notes">{todo.notes}</span>}
        <time className="todo-date">{formatCreatedAt(todo.createdAt)}</time>
      </div>
      <button
        type="button"
        className="icon-button"
        onClick={onEdit}
        aria-label={`${todo.title} を編集`}
      >
        ✎
      </button>
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

/**
 * タイトルとメモを編集するモーダル。
 * Escape で閉じる・背景を操作させない・フォーカスを閉じ込める、は <dialog> に任せている。
 */
function EditTodoDialog({ todo, onClose }: { todo: Todo; onClose: () => void }) {
  const ref = useRef<HTMLDialogElement>(null)
  const [title, setTitle] = useState(todo.title)
  const [notes, setNotes] = useState(todo.notes)
  const canSave = title.trim() !== ''

  // StrictMode では effect が二度走る。開いている dialog に showModal() を呼ぶと
  // 例外になるので、開いていないときだけ呼ぶ。
  useEffect(() => {
    if (!ref.current?.open) ref.current?.showModal()
  }, [])

  const save = async (event: React.FormEvent) => {
    event.preventDefault()
    const updated = await todoStore.updateContent(todo.id, title, notes)
    if (updated) ref.current?.close()
  }

  return (
    <dialog ref={ref} className="edit-dialog" onClose={onClose}>
      <form className="edit-form" onSubmit={save}>
        <h2>Todo を編集</h2>

        <label className="field">
          <span className="field-label">タイトル</span>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            aria-invalid={!canSave}
            autoFocus
          />
        </label>

        <label className="field">
          <span className="field-label">メモ（任意）</span>
          <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={3} />
        </label>

        <div className="dialog-actions">
          <button type="button" className="text-button" onClick={() => ref.current?.close()}>
            キャンセル
          </button>
          <button type="submit" className="text-button" disabled={!canSave}>
            保存
          </button>
        </div>
      </form>
    </dialog>
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

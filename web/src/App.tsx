import { useEffect, useMemo, useState } from 'react'
import {
  applyFilter,
  formatCreatedAt,
  sortTodos,
  todoStore,
  type Filter,
  type Sort,
  type Todo,
} from './todoStore'
import { TodoDetail } from './TodoDetail'
import { TodoBadges } from './TodoBadges'
import { useRoute } from './useRoute'

const FILTERS: { value: Filter; label: string }[] = [
  { value: 'all', label: 'すべて' },
  { value: 'active', label: '未完了' },
  { value: 'completed', label: '完了' },
]

const SORTS: { value: Sort; label: string }[] = [
  { value: 'created', label: '作成順' },
  { value: 'due', label: '期限順' },
  { value: 'priority', label: '優先度順' },
]

export function App() {
  const [todos, setTodos] = useState<readonly Todo[]>([])
  // 件数では「まだ届いていない」と「本当に 0 件」を区別できないので、received で持つ。
  // これがないと、存在しない id の URL を直接開いたときに「読み込み中…」から進まない。
  const [received, setReceived] = useState(false)
  const { route, openDetail, backToList } = useRoute()

  // Kotlin 側の Flow を購読する。戻り値が解除関数なので、そのまま cleanup に渡せる。
  useEffect(
    () =>
      todoStore.subscribe((next) => {
        setTodos(next)
        setReceived(true)
      }),
    [],
  )

  if (route.name === 'detail') {
    return (
      <TodoDetail
        todo={todos.find((t) => t.id === route.id) ?? null}
        isLoading={!received}
        onBack={backToList}
      />
    )
  }

  return <TodoList todos={todos} onOpenDetail={openDetail} />
}

function TodoList({
  todos,
  onOpenDetail,
}: {
  todos: readonly Todo[]
  onOpenDetail: (id: string) => void
}) {
  const [filter, setFilter] = useState<Filter>('all')
  const [sort, setSort] = useState<Sort>('created')
  const [draft, setDraft] = useState('')

  const visible = useMemo(() => sortTodos(applyFilter(todos, filter), sort), [todos, filter, sort])
  const activeCount = todos.filter((t) => !t.isDone).length
  const completedCount = todos.length - activeCount

  // 「期限切れ」の判定に使う。描画のたびに変えず、一覧を開いているあいだは固定する。
  const now = useMemo(() => Date.now(), [])

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

      <nav className="filters" aria-label="絞り込み">
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

      <nav className="sorts" aria-label="並び順">
        <span className="sorts-label">並び順</span>
        {SORTS.map(({ value, label }) => (
          <button
            key={value}
            type="button"
            className={value === sort ? 'chip chip-selected' : 'chip'}
            aria-pressed={value === sort}
            onClick={() => setSort(value)}
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
              <TodoRow
                key={todo.id}
                todo={todo}
                now={now}
                onOpen={() => onOpenDetail(todo.id)}
              />
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

function TodoRow({ todo, now, onOpen }: { todo: Todo; now: number; onOpen: () => void }) {
  return (
    <li className={todo.isDone ? 'todo todo-done' : 'todo'}>
      <input
        type="checkbox"
        checked={todo.isDone}
        onChange={() => todoStore.setDone(todo.id, !todo.isDone)}
        aria-label={`${todo.title} を完了にする`}
      />
      {/* 本文は button で包まない。ここを押せるようにするとテキスト選択が効かなくなるため、
          詳細は右側の独立したボタンから開く。 */}
      <div className="todo-body">
        <span className="todo-title">{todo.title}</span>
        {todo.notes !== '' && <span className="todo-notes">{todo.notes}</span>}
        <TodoBadges todo={todo} now={now} />
        <time className="todo-date">{formatCreatedAt(todo.createdAt)}</time>
      </div>
      <button
        type="button"
        className="icon-button"
        onClick={onOpen}
        aria-label={`${todo.title} の詳細を開く`}
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

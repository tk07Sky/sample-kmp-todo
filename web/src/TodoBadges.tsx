import { asPriority, formatCreatedAt, hasDue, priorityLabel, type Todo } from './todoStore'

/**
 * 期限・優先度・タグを 1 行にまとめて出す。どれも無ければ何も描かない。
 * [now] は期限切れの判定に使う。
 */
export function TodoBadges({ todo, now }: { todo: Todo; now: number }) {
  const priority = asPriority(todo.priority)
  const dueAt = hasDue(todo) ? todo.dueAt : null
  const hasBadge = dueAt !== null || priority !== 'none' || todo.tags.length > 0
  if (!hasBadge) return null

  // 期限切れは色を変えて気づけるようにする。完了済みは急かす必要がないので通常色。
  const overdue = !todo.isDone && dueAt !== null && dueAt < now

  return (
    <span className="badges">
      {dueAt !== null && (
        <span className={overdue ? 'badge badge-overdue' : 'badge badge-due'}>
          期限 {formatCreatedAt(dueAt)}
          {overdue && <span className="visually-hidden">（期限切れ）</span>}
        </span>
      )}
      {priority !== 'none' && (
        <span className="badge badge-priority">優先度 {priorityLabel(priority)}</span>
      )}
      {todo.tags.map((tag) => (
        <span key={tag} className="badge badge-tag">
          # {tag}
        </span>
      ))}
    </span>
  )
}

import { useEffect, useMemo, useState } from 'react'
import {
  PRIORITIES,
  asPriority,
  formatCreatedAt,
  fromDateTimeLocalValue,
  hasDue,
  normalizeTags,
  toDateTimeLocalValue,
  todoStore,
  type Priority,
  type Todo,
} from './todoStore'

/**
 * Todo 1 件の詳細ページ。タイトル・メモに加えて期限・優先度・タグを編集する。
 *
 * 対象は一覧の購読結果から引く。まだ一覧が届いていないだけの場合と、
 * 本当に存在しない（削除された・URL を直接開いた）場合を [isLoading] で分ける。
 */
export function TodoDetail({
  todo,
  isLoading,
  onBack,
}: {
  todo: Todo | null
  isLoading: boolean
  onBack: () => void
}) {
  if (todo === null) {
    return (
      <div className="app">
        <DetailHeader onBack={onBack} />
        <p className="empty">{isLoading ? '読み込み中…' : 'この Todo は見つかりませんでした'}</p>
      </div>
    )
  }

  // id が変わったら入力欄を作り直す
  return <DetailForm key={todo.id} todo={todo} onBack={onBack} />
}

function DetailHeader({ onBack }: { onBack: () => void }) {
  return (
    <header className="app-header">
      <div className="detail-heading">
        <button type="button" className="icon-button" onClick={onBack} aria-label="一覧へ戻る">
          ←
        </button>
        <h1>Todo の詳細</h1>
      </div>
    </header>
  )
}

function DetailForm({ todo, onBack }: { todo: Todo; onBack: () => void }) {
  // 編集中の値はページのローカルに持ち、保存を押したときだけ Kotlin 側へ書く
  const [title, setTitle] = useState(todo.title)
  const [notes, setNotes] = useState(todo.notes)
  const [due, setDue] = useState(hasDue(todo) ? toDateTimeLocalValue(todo.dueAt) : '')
  const [priority, setPriority] = useState<Priority>(asPriority(todo.priority))
  const [tags, setTags] = useState<string[]>([...todo.tags])
  const [tagDraft, setTagDraft] = useState('')
  const [knownTags, setKnownTags] = useState<readonly string[]>([])

  useEffect(() => todoStore.subscribeTags(setKnownTags), [])

  const canSave = title.trim() !== ''
  // 他の Todo で使われているタグを候補に出す。まだ付いていないものだけ。
  const suggestions = useMemo(
    () => knownTags.filter((t) => !tags.includes(t)),
    [knownTags, tags],
  )

  const addTag = (raw: string) => {
    setTags((current) => normalizeTags([...current, raw]))
    setTagDraft('')
  }

  const save = async (event: React.FormEvent) => {
    event.preventDefault()
    const saved = await todoStore.updateDetails(
      todo.id,
      title,
      notes,
      fromDateTimeLocalValue(due),
      priority,
      tags,
    )
    if (saved) onBack()
  }

  const remove = async () => {
    await todoStore.remove(todo.id)
    onBack()
  }

  return (
    <div className="app">
      <DetailHeader onBack={onBack} />

      <form className="detail-form" onSubmit={save}>
        <label className="field">
          <span className="field-label">タイトル</span>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            aria-invalid={!canSave}
          />
        </label>

        <label className="field">
          <span className="field-label">メモ（任意）</span>
          <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={4} />
        </label>

        <label className="field">
          <span className="field-label">期限（任意）</span>
          <input type="datetime-local" value={due} onChange={(e) => setDue(e.target.value)} />
          <span className="field-hint">空にすると期限なしになります</span>
        </label>

        <fieldset className="field">
          <legend className="field-label">優先度</legend>
          <div className="chip-row">
            {PRIORITIES.map(({ value, label }) => (
              <button
                key={value}
                type="button"
                className={value === priority ? 'chip chip-selected' : 'chip'}
                aria-pressed={value === priority}
                onClick={() => setPriority(value)}
              >
                {label}
              </button>
            ))}
          </div>
        </fieldset>

        <fieldset className="field">
          <legend className="field-label">タグ</legend>

          {tags.length > 0 && (
            <ul className="tag-list">
              {tags.map((tag) => (
                <li key={tag} className="badge badge-tag">
                  {tag}
                  <button
                    type="button"
                    className="tag-remove"
                    onClick={() => setTags((current) => current.filter((t) => t !== tag))}
                    aria-label={`タグ「${tag}」を外す`}
                  >
                    ✕
                  </button>
                </li>
              ))}
            </ul>
          )}

          <div className="tag-input">
            <input
              type="text"
              value={tagDraft}
              onChange={(e) => setTagDraft(e.target.value)}
              aria-label="タグを追加"
              placeholder="タグを追加"
              onKeyDown={(e) => {
                // Enter でフォーム全体が送信されないようにしてから、タグとして足す
                if (e.key === 'Enter') {
                  e.preventDefault()
                  if (tagDraft.trim() !== '') addTag(tagDraft)
                }
              }}
            />
            <button
              type="button"
              className="text-button"
              onClick={() => addTag(tagDraft)}
              disabled={tagDraft.trim() === ''}
            >
              追加
            </button>
          </div>

          {suggestions.length > 0 && (
            <>
              <span className="field-hint">使ったことのあるタグ</span>
              <div className="chip-row">
                {suggestions.map((tag) => (
                  <button key={tag} type="button" className="chip" onClick={() => addTag(tag)}>
                    {tag}
                  </button>
                ))}
              </div>
            </>
          )}
        </fieldset>

        <p className="field-hint">作成 {formatCreatedAt(todo.createdAt)}</p>

        <div className="detail-actions">
          <button type="button" className="text-button" onClick={remove}>
            削除
          </button>
          <button type="button" className="text-button" onClick={onBack}>
            キャンセル
          </button>
          <button type="submit" className="add-button" disabled={!canSave}>
            保存
          </button>
        </div>
      </form>
    </div>
  )
}

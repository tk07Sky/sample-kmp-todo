import { useCallback, useEffect, useState } from 'react'

/**
 * 一覧と詳細の 2 画面だけなので、ルーターは入れずに History API を直接使う。
 *
 *   /          一覧
 *   /todo/<id> 詳細
 *
 * DOM で描いている利点を活かして、URL・ブラウザの戻る・リロードをそのまま使えるようにしている
 * （Compose 版は canvas なので画面の状態を Kotlin 側に持っている）。
 */
export type Route = { name: 'list' } | { name: 'detail'; id: string }

export function parseRoute(pathname: string): Route {
  const match = /^\/todo\/([^/]+)\/?$/.exec(pathname)
  return match ? { name: 'detail', id: decodeURIComponent(match[1]) } : { name: 'list' }
}

export function useRoute(): {
  route: Route
  openDetail: (id: string) => void
  backToList: () => void
} {
  const [route, setRoute] = useState<Route>(() => parseRoute(window.location.pathname))

  // 戻る・進むで URL が変わったら状態も追従させる
  useEffect(() => {
    const onPopState = () => setRoute(parseRoute(window.location.pathname))
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  const openDetail = useCallback((id: string) => {
    window.history.pushState(null, '', `/todo/${encodeURIComponent(id)}`)
    setRoute({ name: 'detail', id })
  }, [])

  const backToList = useCallback(() => {
    window.history.pushState(null, '', '/')
    setRoute({ name: 'list' })
  }, [])

  return { route, openDetail, backToList }
}

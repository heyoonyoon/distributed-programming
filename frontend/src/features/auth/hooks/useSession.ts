import { useState } from 'react'
import { readSession, tokenKey } from '../../../lib/session'
import type { AuthSession } from '../../../lib/types'
import { authApi } from '../api'

/** 로그인 세션 상태와 로그아웃/만료 처리를 한 곳에서 관리한다. */
export function useSession() {
  const [session, setSession] = useState<AuthSession | null>(() => readSession())

  function logout() {
    void authApi.logout().catch(() => undefined)
    window.localStorage.removeItem(tokenKey)
    setSession(null)
  }

  function handleUnauthorized() {
    window.localStorage.removeItem(tokenKey)
    setSession(null)
  }

  return { session, setSession, logout, handleUnauthorized }
}

import { decodeUserType } from './api/httpClient'
import type { AuthSession } from './types'

export const tokenKey = 'insurance.authToken'

export function readSession(): AuthSession | null {
  const token = window.localStorage.getItem(tokenKey)
  return token ? { token, userType: decodeUserType(token) } : null
}

export function homePath(session: AuthSession) {
  return session.userType === 'EMPLOYEE' ? '/employee/reviews' : '/customer/home'
}

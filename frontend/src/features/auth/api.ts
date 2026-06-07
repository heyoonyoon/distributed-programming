import { request } from '../../lib/api/httpClient'
import type { LoginRequest } from './types'

export const authApi = {
  async login(body: LoginRequest): Promise<{ token: string }> {
    return request('/auth/login', {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  async logout(): Promise<void> {
    return request('/auth/logout', { method: 'POST' })
  },
}

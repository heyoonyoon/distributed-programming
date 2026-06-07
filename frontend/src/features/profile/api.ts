import { request } from '../../lib/api/httpClient'
import type { PolicyholderProfile, UpdateProfileRequest } from './types'

export const profileApi = {
  async getMe(token: string): Promise<PolicyholderProfile> {
    return request('/me', {}, token)
  },

  async updateProfile(
    token: string,
    body: UpdateProfileRequest,
  ): Promise<PolicyholderProfile> {
    return request(
      '/me/profile',
      {
        method: 'PUT',
        body: JSON.stringify(body),
      },
      token,
    )
  },
}

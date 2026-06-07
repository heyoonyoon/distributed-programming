import { useEffect, useState } from 'react'
import { ApiError } from '../../../lib/api/httpClient'
import { profileApi } from '../api'
import type { PolicyholderProfile, UpdateProfileRequest } from '../types'

export function useProfile(token: string, onUnauthorized: () => void) {
  const [profile, setProfile] = useState<PolicyholderProfile | null>(null)
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(true)
  const [isSaving, setSaving] = useState(false)

  useEffect(() => {
    let isMounted = true

    async function loadProfile() {
      setError('')
      setLoading(true)

      try {
        const nextProfile = await profileApi.getMe(token)
        if (isMounted) {
          setProfile(nextProfile)
        }
      } catch (err) {
        if (err instanceof ApiError && err.status === 401) {
          onUnauthorized()
          return
        }

        if (isMounted) {
          setError(err instanceof Error ? err.message : '내 정보 조회에 실패했습니다.')
        }
      } finally {
        if (isMounted) {
          setLoading(false)
        }
      }
    }

    loadProfile()

    return () => {
      isMounted = false
    }
  }, [onUnauthorized, token])

  async function saveProfile(body: UpdateProfileRequest) {
    setStatus('')
    setError('')
    setSaving(true)

    try {
      const nextProfile = await profileApi.updateProfile(token, body)
      setProfile(nextProfile)
      setStatus('개인정보가 성공적으로 변경되었습니다.')
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }

      setError(err instanceof Error ? err.message : '개인정보 수정에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  return { profile, status, error, isLoading, isSaving, saveProfile }
}

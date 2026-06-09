import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../../../lib/api/httpClient'
import type { ReviewResult } from '../../../lib/types'
import { underwritingApi } from '../api'
import type { ConfirmReviewResponse, PendingReview, ReviewApplicationDetail } from '../types'

export function useUnderwritingReviews(token: string, onUnauthorized: () => void) {
  const [pendingReviews, setPendingReviews] = useState<PendingReview[]>([])
  const [selectedReview, setSelectedReview] = useState<ReviewApplicationDetail | null>(null)
  const [reviewResult, setReviewResult] = useState<ReviewResult>('APPROVED')
  const [comment, setComment] = useState('이상 없음')
  const [surchargeRate, setSurchargeRate] = useState('0.2')
  const [decision, setDecision] = useState<ConfirmReviewResponse | null>(null)
  const [error, setError] = useState('')

  async function loadPendingReviews() {
    try {
      const list = await underwritingApi.getPendingReviews(token)
      setPendingReviews(list)
      if (list[0]) {
        setSelectedReview(await underwritingApi.getReviewApplication(token, list[0].applicationId))
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '심사 대기 목록 조회에 실패했습니다.')
    }
  }

  useEffect(() => {
    void Promise.resolve().then(() => {
      loadPendingReviews()
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function selectReview(id: number) {
    setError('')
    setDecision(null)
    try {
      setSelectedReview(await underwritingApi.getReviewApplication(token, id))
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '심사 상세 조회에 실패했습니다.')
    }
  }

  async function confirmReview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedReview) {
      return
    }

    try {
      const response = await underwritingApi.confirmReview(token, selectedReview.applicationId, {
        result: reviewResult,
        comment,
        ...(reviewResult === 'CONDITIONAL' ? { surchargeRate: Number(surchargeRate) } : {}),
      })
      setDecision(response)
      await loadPendingReviews()
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '심사 확정에 실패했습니다.')
    }
  }

  return {
    pendingReviews,
    selectedReview,
    reviewResult,
    setReviewResult,
    comment,
    setComment,
    surchargeRate,
    setSurchargeRate,
    decision,
    error,
    selectReview,
    confirmReview,
  }
}

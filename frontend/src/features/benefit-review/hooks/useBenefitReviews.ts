import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '../../../lib/api/httpClient'
import type { BenefitReviewResult } from '../../../lib/types'
import { formatContractStatus } from '../../../utils/format'
import { benefitReviewApi } from '../api'
import type { BenefitReviewDetail, BenefitReviewSummary, ConfirmBenefitReviewResponse } from '../types'
import { describeBenefitReviewResult } from '../utils'

export function useBenefitReviews(token: string, onUnauthorized: () => void) {
  const [reviews, setReviews] = useState<BenefitReviewSummary[]>([])
  const [selectedReview, setSelectedReview] = useState<BenefitReviewDetail | null>(null)
  const [reviewResult, setReviewResult] = useState<BenefitReviewResult>('APPROVED')
  const [comment, setComment] = useState('정상 청구')
  const [assignClaimId, setAssignClaimId] = useState('')
  const [assignEmployeeId, setAssignEmployeeId] = useState('')
  const [decision, setDecision] = useState<ConfirmBenefitReviewResponse | null>(null)
  const [statusMessage, setStatusMessage] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(true)
  const [isSubmitting, setSubmitting] = useState(false)
  const [isRetrying, setRetrying] = useState(false)
  const [isAssigning, setAssigning] = useState(false)
  const canRetry = selectedReview?.claimStatus === 'FAILED' || decision?.claimStatus === 'FAILED'

  async function refreshBenefitReviews() {
    setReviews(await benefitReviewApi.getBenefitReviews(token))
  }

  async function loadBenefitReviews() {
    setError('')
    setLoading(true)

    try {
      const list = await benefitReviewApi.getBenefitReviews(token)
      setReviews(list)
      if (list[0]) {
        await selectBenefitReview(list[0].claimId)
      } else {
        setSelectedReview(null)
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '보험금 심사 목록 조회에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void Promise.resolve().then(() => {
      loadBenefitReviews()
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function selectBenefitReview(claimId: number) {
    setError('')
    setDecision(null)
    setStatusMessage('')

    try {
      const detail = await benefitReviewApi.getBenefitReview(token, claimId)
      setSelectedReview(detail)
      setAssignClaimId(String(detail.claimId))
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      if (err instanceof ApiError && err.status === 409) {
        setError('다른 담당자 처리 중입니다.')
        return
      }
      setError(err instanceof Error ? err.message : '보험금 심사 상세 조회에 실패했습니다.')
    }
  }

  async function confirmBenefitReview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedReview) {
      return
    }

    setError('')
    setStatusMessage('')
    setSubmitting(true)

    try {
      const response = await benefitReviewApi.confirmBenefitReview(token, selectedReview.claimId, {
        result: reviewResult,
        comment,
      })
      setDecision(response)
      setStatusMessage(describeBenefitReviewResult(response))
      await refreshBenefitReviews()
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '보험금 심사 확정에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  async function retryPayout() {
    if (!selectedReview) {
      return
    }

    setError('')
    setStatusMessage('')
    setRetrying(true)

    try {
      const response = await benefitReviewApi.retryBenefitPayout(token, selectedReview.claimId)
      const claimStatus = response?.claimStatus ?? 'COMPLETED'
      setStatusMessage(
        claimStatus === 'COMPLETED'
          ? '지급 재시도 완료'
          : `지급 재시도 결과 ${formatContractStatus(claimStatus)}`,
      )
      await refreshBenefitReviews()
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '지급 재시도에 실패했습니다.')
    } finally {
      setRetrying(false)
    }
  }

  async function assignClaim(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const claimId = Number(assignClaimId)
    const employeeId = Number(assignEmployeeId)

    if (!claimId || !employeeId) {
      setError('청구번호와 직원번호를 입력해 주세요.')
      return
    }

    setError('')
    setStatusMessage('')
    setAssigning(true)

    try {
      await benefitReviewApi.assignClaim(token, claimId, { employeeId })
      setStatusMessage(`청구 ${claimId}번을 직원 ${employeeId}에게 재배정했습니다.`)
      await refreshBenefitReviews()
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      setError(err instanceof Error ? err.message : '담당자 재배정에 실패했습니다.')
    } finally {
      setAssigning(false)
    }
  }

  return {
    reviews,
    selectedReview,
    reviewResult,
    setReviewResult,
    comment,
    setComment,
    assignClaimId,
    setAssignClaimId,
    assignEmployeeId,
    setAssignEmployeeId,
    decision,
    statusMessage,
    error,
    isLoading,
    isSubmitting,
    isRetrying,
    isAssigning,
    canRetry,
    selectBenefitReview,
    confirmBenefitReview,
    retryPayout,
    assignClaim,
  }
}

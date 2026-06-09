import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, employeeIdFromToken } from '../../../lib/api/httpClient'
import type { BenefitReviewResult } from '../../../lib/types'
import { formatContractStatus } from '../../../utils/format'
import { benefitReviewApi } from '../api'
import type {
  BenefitReviewDetail,
  BenefitReviewSummary,
  ConfirmBenefitReviewResponse,
  UnassignedBenefitReview,
} from '../types'
import { describeBenefitReviewResult } from '../utils'

export function useBenefitReviews(token: string, onUnauthorized: () => void) {
  const [reviews, setReviews] = useState<BenefitReviewSummary[]>([])
  const [unassigned, setUnassigned] = useState<UnassignedBenefitReview[]>([])
  const [selectedReview, setSelectedReview] = useState<BenefitReviewDetail | null>(null)
  const [reviewResult, setReviewResult] = useState<BenefitReviewResult>('APPROVED')
  const [comment, setComment] = useState('정상 청구')
  const [payoutAmount, setPayoutAmount] = useState('')
  const [decision, setDecision] = useState<ConfirmBenefitReviewResponse | null>(null)
  const [statusMessage, setStatusMessage] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setLoading] = useState(true)
  const [isSubmitting, setSubmitting] = useState(false)
  const [isRetrying, setRetrying] = useState(false)
  const [assigningClaimId, setAssigningClaimId] = useState<number | null>(null)
  const canRetry = selectedReview?.claimStatus === 'FAILED' || decision?.claimStatus === 'FAILED'

  // 미배정 목록은 실패해도 화면을 막지 않는다(401만 로그인 만료 처리).
  async function loadUnassigned() {
    try {
      setUnassigned(await benefitReviewApi.getUnassignedBenefitReviews(token))
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
      }
    }
  }

  async function refreshBenefitReviews() {
    setReviews(await benefitReviewApi.getBenefitReviews(token))
    await loadUnassigned()
  }

  async function loadBenefitReviews() {
    setError('')
    setLoading(true)

    try {
      const list = await benefitReviewApi.getBenefitReviews(token)
      setReviews(list)
      await loadUnassigned()
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
      setPayoutAmount(String(detail.requestAmount))
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

    const parsedPayout = Number(payoutAmount)
    if (reviewResult === 'APPROVED' && (!payoutAmount || Number.isNaN(parsedPayout) || parsedPayout <= 0)) {
      setError('승인 시 지급금액을 0보다 큰 값으로 입력해 주세요.')
      return
    }

    setError('')
    setStatusMessage('')
    setSubmitting(true)

    try {
      const response = await benefitReviewApi.confirmBenefitReview(token, selectedReview.claimId, {
        result: reviewResult,
        comment,
        ...(reviewResult === 'APPROVED' ? { payoutAmount: parsedPayout } : {}),
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

  // 미배정 사고접수 건을 로그인한 직원(JWT sub)에게 배정하고 바로 선택한다.
  async function assignToMe(claimId: number) {
    const employeeId = employeeIdFromToken(token)
    if (!employeeId) {
      setError('토큰에서 직원 정보를 확인할 수 없습니다. 다시 로그인해 주세요.')
      return
    }

    setError('')
    setStatusMessage('')
    setAssigningClaimId(claimId)

    try {
      await benefitReviewApi.assignClaim(token, claimId, { employeeId })
      setStatusMessage(`청구 ${claimId}번을 내 심사 큐로 배정했습니다.`)
      await refreshBenefitReviews()
      await selectBenefitReview(claimId)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onUnauthorized()
        return
      }
      if (err instanceof ApiError && err.status === 409) {
        setError('이미 다른 담당자에게 배정된 건입니다.')
        await refreshBenefitReviews()
        return
      }
      setError(err instanceof Error ? err.message : '배정에 실패했습니다.')
    } finally {
      setAssigningClaimId(null)
    }
  }

  return {
    reviews,
    unassigned,
    selectedReview,
    reviewResult,
    setReviewResult,
    comment,
    setComment,
    payoutAmount,
    setPayoutAmount,
    decision,
    statusMessage,
    error,
    isLoading,
    isSubmitting,
    isRetrying,
    assigningClaimId,
    canRetry,
    selectBenefitReview,
    confirmBenefitReview,
    retryPayout,
    assignToMe,
  }
}

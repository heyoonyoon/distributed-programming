import type { ConfirmBenefitReviewResponse } from './types'

export function describeBenefitReviewResult(result: ConfirmBenefitReviewResponse) {
  if (result.claimStatus === 'COMPLETED') {
    return '지급 완료'
  }

  if (result.claimStatus === 'FAILED') {
    return '지급 실패, 계좌 확인 후 재시도'
  }

  if (result.claimStatus === 'REJECTED') {
    return '반려 완료'
  }

  return `청구 상태 ${result.claimStatus}`
}

import type { BenefitReviewResult, ClaimStatus, ClaimType } from '../../lib/types'

export type BenefitReviewSummary = {
  claimId: number
  requestAmount: number
  hospitalName: string
  claimStatus: ClaimStatus
}

export type BenefitReviewDetail = BenefitReviewSummary & {
  diagnosisCode: string
  assignedStaffId: number
}

// GET /staff/benefit-reviews/unassigned — 미배정(assignedStaffId=null && IN_REVIEW) 보상심사 요약.
export type UnassignedBenefitReview = {
  claimId: number
  claimType: ClaimType
  requestAmount: number
  hospitalName: string | null
  accidentType: string | null
  claimStatus: ClaimStatus
}

export type ConfirmBenefitReviewRequest = {
  result: BenefitReviewResult
  comment: string
  // 승인(APPROVED) 시 백엔드 필수. 거절 시 생략 가능.
  payoutAmount?: number
}

export type ConfirmBenefitReviewResponse = {
  claimId: number
  result: BenefitReviewResult
  claimStatus: ClaimStatus
}

export type RetryBenefitPayoutResponse = {
  claimId: number
  claimStatus: ClaimStatus
}

export type AssignClaimRequest = {
  employeeId: number
}

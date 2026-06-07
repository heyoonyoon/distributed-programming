import type { BenefitReviewResult, ClaimStatus } from '../../lib/types'

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

export type ConfirmBenefitReviewRequest = {
  result: BenefitReviewResult
  comment: string
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

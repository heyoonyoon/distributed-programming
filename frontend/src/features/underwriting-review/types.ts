import type { MedicalHistory, ReviewResult, VehicleInfo } from '../../lib/types'

export type PendingReview = {
  applicationId: number
  appliedAt: string
  applicantName: string
  productName: string
  basePremium: number
}

export type AccidentHistory = {
  accidentCount: number
  totalPaidAmount: number
  licenseStatus: string
}

export type ReviewApplicationDetail = {
  applicationId: number
  applicantName: string
  birthDate: string
  ssn: string
  productName: string
  basePremium: number
  vehicleInfo: VehicleInfo | null
  medicalHistory: MedicalHistory | null
  accidentHistory: AccidentHistory | null
}

export type ConfirmReviewRequest = {
  result: ReviewResult
  comment: string
  surchargeRate?: number
}

export type ConfirmReviewResponse = {
  reviewId: number
  result: ReviewResult
  surchargeRate: number
  adjustedPremium: number
}

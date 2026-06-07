import type { ClaimComplexity, ClaimStatus, ClaimType } from '../../lib/types'

export type CarAccidentReportRequest = {
  contractId: number
  accidentDate: string
  accidentLocation: string
  accidentType: string
  vehicleNumber: string
  hasInjury: boolean
  injuredCount: number
  attachments: File[]
}

export type CarAccidentReportResponse = {
  reportId: number
  status: 'PENDING'
}

export type HealthClaimRequest = {
  contractId: number
  hospitalName: string
  diagnosisCode: string
  treatmentDate: string
  requestAmount: number
  receiptAmount: number
  attachments: File[]
}

export type HealthClaimResponse = {
  claimId: number
  status: ClaimStatus
  complexity: ClaimComplexity
}

export type ClaimListItem = {
  claimId: number
  claimType: ClaimType
  claimDate: string
  requestAmount: number
  paidAmount: number
  status: ClaimStatus
}

export type BenefitAnalysis = {
  totalPaidPremium: number
  totalReceivedBenefit: number
  profit: number
  profitRate: number
}

export type UserType = 'POLICYHOLDER' | 'EMPLOYEE'
export type ProductType = 'HEALTH' | 'CAR'
export type ApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export type ReviewResult = 'APPROVED' | 'CONDITIONAL' | 'REJECTED'
export type ContractStatus = 'ACTIVE' | 'SUSPENDED' | 'TERMINATED'
export type PaymentMethod = 'CARD' | 'TRANSFER' | 'AUTO_DEBIT'
export type PaymentStatus = 'SUCCESS' | 'FAILED'
export type ClaimStatus =
  | 'PENDING'
  | 'IN_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'COMPLETED'
  | 'FAILED'
export type BenefitReviewResult = 'APPROVED' | 'REJECTED'
export type ClaimComplexity = 'SIMPLE' | 'COMPLEX'

export type AuthSession = {
  token: string
  userType: UserType | null
}

export type PolicyholderProfile = {
  name: string
  email: string
  phone: string
  address: string
  bankAccount: string
}

export type LoginRequest = {
  email: string
  password: string
}

export type UpdateProfileRequest = {
  email: string
  phone: string
  address: string
  bankAccount: string
}

export type ProductSummary = {
  id: number
  productName: string
  coverageSummary: string
  monthlyPremium: number
  productType: ProductType
}

export type CoverageItem = {
  itemName: string
  coverageLimit: number
  deductible: number
}

export type ProductDetail = {
  id: number
  productName: string
  productType: ProductType
  description: string
  monthlyPremium: number
  coverageItems: CoverageItem[]
}

export type MedicalHistory = {
  currentConditions: string
  pastHospitalization: string
  medications: string
}

export type VehicleInfo = {
  plateNumber: string
  vehicleType: string
  modelYear: number
  drivingExperienceYears: number
}

export type CreateApplicationRequest = {
  productId: number
  medicalHistory?: MedicalHistory
  vehicleInfo?: VehicleInfo
}

export type ApplicationCreated = {
  applicationId: number
  status: ApplicationStatus
  appliedAt: string
}

export type MyApplication = {
  applicationId: number
  status: ApplicationStatus
  appliedAt: string
  productId: number
  productName: string
}

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

export type ContractSummary = {
  contractId: number
  productName: string
  productType: ProductType
  startDate: string
  endDate: string
  monthlyPremium: number
  status: ContractStatus
}

export type ContractDetail = ContractSummary & {
  paymentMethod: PaymentMethod | '미등록'
  coverageItems: CoverageItem[]
}

export type UnpaidContract = {
  contractId: number
  productName: string
  dueDate: string
  unpaidPrincipal: number
  overdueDays: number
  overdueInterest: number
}

export type PayableContract = {
  contractId: number
  productName: string
  dueDate: string
  amount: number
}

export type PaymentRequest = {
  method: PaymentMethod
  paymentInfo: string
}

export type PaymentResponse = {
  paymentId: number
  status: PaymentStatus
  amount: number
  reason: string | null
}

export type AutoDebitRequest = {
  account: string
  withdrawalDay: number
}

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

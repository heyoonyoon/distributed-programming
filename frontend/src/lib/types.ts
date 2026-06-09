// 여러 feature가 공유하는 기본 타입/enum 및 공용 값 객체.

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
export type ClaimType = 'HEALTH' | 'CAR' | 'CAR_ACCIDENT'

export type AuthSession = {
  token: string
  userType: UserType | null
}

// 가입 신청과 가입 심사가 함께 쓰는 위험 평가용 값 객체.
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

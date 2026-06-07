import type {
  ApplicationStatus,
  ContractStatus,
  MedicalHistory,
  PaymentMethod,
  PaymentStatus,
  ProductType,
  VehicleInfo,
} from '../../lib/types'

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

import { request, requestForm } from '../../lib/api/httpClient'
import type {
  BenefitAnalysis,
  CarAccidentReportRequest,
  CarAccidentReportResponse,
  ClaimListItem,
  HealthClaimRequest,
  HealthClaimResponse,
} from './types'

function buildCarAccidentForm(body: CarAccidentReportRequest) {
  const formData = new FormData()
  formData.append('contractId', String(body.contractId))
  formData.append('accidentDate', body.accidentDate)
  formData.append('accidentLocation', body.accidentLocation)
  formData.append('accidentType', body.accidentType)
  formData.append('vehicleNumber', body.vehicleNumber)
  formData.append('hasInjury', String(body.hasInjury))
  formData.append('injuredCount', String(body.injuredCount))
  body.attachments.forEach((file) => formData.append('attachments', file))

  return formData
}

function buildHealthClaimForm(body: HealthClaimRequest) {
  const formData = new FormData()
  formData.append('contractId', String(body.contractId))
  formData.append('hospitalName', body.hospitalName)
  formData.append('diagnosisCode', body.diagnosisCode)
  formData.append('treatmentDate', body.treatmentDate)
  formData.append('requestAmount', String(body.requestAmount))
  formData.append('receiptAmount', String(body.receiptAmount))
  body.attachments.forEach((file) => formData.append('attachments', file))

  return formData
}

export const claimsApi = {
  async submitCarAccidentReport(
    token: string,
    body: CarAccidentReportRequest,
  ): Promise<CarAccidentReportResponse> {
    return requestForm('/claims/car-accidents', buildCarAccidentForm(body), token)
  },

  async submitHealthClaim(
    token: string,
    body: HealthClaimRequest,
  ): Promise<HealthClaimResponse> {
    return requestForm('/claims/health', buildHealthClaimForm(body), token)
  },

  async getClaimStatus(token: string): Promise<ClaimListItem[]> {
    return request('/claims/status', {}, token)
  },

  async getClaimHistory(
    token: string,
    params: { from?: string; to?: string } = {},
  ): Promise<ClaimListItem[]> {
    const query = new URLSearchParams()

    if (params.from) {
      query.set('from', params.from)
    }
    if (params.to) {
      query.set('to', params.to)
    }

    const suffix = query.toString() ? `?${query.toString()}` : ''
    return request(`/claims/history${suffix}`, {}, token)
  },

  async getBenefitAnalysis(
    token: string,
    contractId: number,
  ): Promise<BenefitAnalysis> {
    const query = new URLSearchParams({ contractId: String(contractId) })
    return request(`/claims/benefit-analysis?${query.toString()}`, {}, token)
  },
}

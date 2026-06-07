import { request, requestBlob } from '../../lib/api/httpClient'
import type { ProductType } from '../../lib/types'
import type {
  ApplicationCreated,
  AutoDebitRequest,
  ContractDetail,
  ContractSummary,
  CreateApplicationRequest,
  MyApplication,
  PayableContract,
  PaymentRequest,
  PaymentResponse,
  ProductDetail,
  ProductSummary,
  UnpaidContract,
} from './types'

export const contractsApi = {
  async getProducts(params: {
    type: ProductType
    minPremium?: string
    maxPremium?: string
    keyword?: string
  }): Promise<ProductSummary[]> {
    const query = new URLSearchParams({ type: params.type })

    if (params.minPremium) {
      query.set('minPremium', params.minPremium)
    }
    if (params.maxPremium) {
      query.set('maxPremium', params.maxPremium)
    }
    if (params.keyword) {
      query.set('keyword', params.keyword)
    }

    return request(`/products?${query.toString()}`)
  },

  async getProduct(id: number): Promise<ProductDetail> {
    return request(`/products/${id}`)
  },

  async createApplication(
    token: string,
    body: CreateApplicationRequest,
  ): Promise<ApplicationCreated> {
    return request(
      '/applications',
      {
        method: 'POST',
        body: JSON.stringify(body),
      },
      token,
    )
  },

  async getMyApplications(token: string): Promise<MyApplication[]> {
    return request('/applications/me', {}, token)
  },

  async cancelApplication(token: string, id: number): Promise<void> {
    return request(`/applications/${id}/cancel`, { method: 'POST' }, token)
  },

  async getContracts(token: string): Promise<ContractSummary[]> {
    return request('/contracts', {}, token)
  },

  async getContract(token: string, id: number): Promise<ContractDetail> {
    return request(`/contracts/${id}`, {}, token)
  },

  async downloadContract(
    token: string,
    id: number,
  ): Promise<{ blob: Blob; filename: string }> {
    return requestBlob(`/contracts/${id}/pdf`, token)
  },

  async getUnpaidContracts(token: string): Promise<UnpaidContract[]> {
    return request('/contracts/unpaid', {}, token)
  },

  async getUnpaidContract(token: string, id: number): Promise<UnpaidContract> {
    return request(`/contracts/${id}/unpaid`, {}, token)
  },

  async getPayableContracts(token: string): Promise<PayableContract[]> {
    return request('/contracts/payable', {}, token)
  },

  async payPremium(
    token: string,
    id: number,
    body: PaymentRequest,
  ): Promise<PaymentResponse> {
    return request(
      `/contracts/${id}/payments`,
      {
        method: 'POST',
        body: JSON.stringify(body),
      },
      token,
    )
  },

  async registerAutoDebit(
    token: string,
    id: number,
    body: AutoDebitRequest,
  ): Promise<void> {
    return request(
      `/contracts/${id}/auto-debit`,
      {
        method: 'POST',
        body: JSON.stringify(body),
      },
      token,
    )
  },
}

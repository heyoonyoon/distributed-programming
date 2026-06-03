import type {
  ConfirmReviewRequest,
  ConfirmReviewResponse,
  CreateApplicationRequest,
  ApplicationCreated,
  LoginRequest,
  MyApplication,
  PendingReview,
  PolicyholderProfile,
  ProductDetail,
  ProductSummary,
  ProductType,
  ReviewApplicationDetail,
  UpdateProfileRequest,
  UserType,
} from '../types'

const baseUrl = 'http://localhost:8080'

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

type TokenPayload = {
  userType?: UserType
}

function decodeBase64Url(value: string) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(
    normalized.length + ((4 - (normalized.length % 4)) % 4),
    '=',
  )

  return window.atob(padded)
}

export function decodeUserType(token: string): UserType | null {
  try {
    const payload = JSON.parse(decodeBase64Url(token.split('.')[1])) as TokenPayload
    return payload.userType === 'POLICYHOLDER' || payload.userType === 'EMPLOYEE'
      ? payload.userType
      : null
  } catch {
    return null
  }
}

async function readError(response: Response) {
  const contentType = response.headers.get('content-type') ?? ''

  if (contentType.includes('application/json')) {
    const body = await response.json()
    if (typeof body === 'string') {
      return body
    }
    if (body && typeof body.message === 'string') {
      return body.message
    }
    if (body && typeof body.error === 'string') {
      return body.error
    }
  }

  const text = await response.text()
  return text || `요청 처리에 실패했습니다. (${response.status})`
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  token?: string,
): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })

  if (!response.ok) {
    throw new ApiError(response.status, await readError(response))
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

export const apiClient = {
  async login(body: LoginRequest): Promise<{ token: string }> {
    return request('/auth/login', {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  async logout(): Promise<void> {
    return request('/auth/logout', { method: 'POST' })
  },

  async getMe(token: string): Promise<PolicyholderProfile> {
    return request('/me', {}, token)
  },

  async updateProfile(
    token: string,
    body: UpdateProfileRequest,
  ): Promise<PolicyholderProfile> {
    return request(
      '/me/profile',
      {
        method: 'PUT',
        body: JSON.stringify(body),
      },
      token,
    )
  },

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

  async getPendingReviews(token: string): Promise<PendingReview[]> {
    return request('/reviews/pending', {}, token)
  },

  async getReviewApplication(
    token: string,
    id: number,
  ): Promise<ReviewApplicationDetail> {
    return request(`/reviews/applications/${id}`, {}, token)
  },

  async confirmReview(
    token: string,
    id: number,
    body: ConfirmReviewRequest,
  ): Promise<ConfirmReviewResponse> {
    return request(
      `/reviews/applications/${id}/confirm`,
      {
        method: 'POST',
        body: JSON.stringify(body),
      },
      token,
    )
  },
}

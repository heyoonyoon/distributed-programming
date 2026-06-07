import { request } from '../../lib/api/httpClient'
import type {
  AssignClaimRequest,
  BenefitReviewDetail,
  BenefitReviewSummary,
  ConfirmBenefitReviewRequest,
  ConfirmBenefitReviewResponse,
  RetryBenefitPayoutResponse,
} from './types'

export const benefitReviewApi = {
  async getBenefitReviews(token: string): Promise<BenefitReviewSummary[]> {
    return request('/staff/benefit-reviews', {}, token)
  },

  async getBenefitReview(
    token: string,
    claimId: number,
  ): Promise<BenefitReviewDetail> {
    return request(`/staff/benefit-reviews/${claimId}`, {}, token)
  },

  async confirmBenefitReview(
    token: string,
    claimId: number,
    body: ConfirmBenefitReviewRequest,
  ): Promise<ConfirmBenefitReviewResponse> {
    return request(
      `/staff/benefit-reviews/${claimId}/confirm`,
      {
        method: 'POST',
        body: JSON.stringify(body),
      },
      token,
    )
  },

  async retryBenefitPayout(
    token: string,
    claimId: number,
  ): Promise<RetryBenefitPayoutResponse | undefined> {
    return request(
      `/staff/benefit-reviews/${claimId}/retry`,
      { method: 'POST' },
      token,
    )
  },

  async assignClaim(
    token: string,
    claimId: number,
    body: AssignClaimRequest,
  ): Promise<void> {
    return request(
      `/staff/claims/${claimId}/assign`,
      {
        method: 'POST',
        body: JSON.stringify(body),
      },
      token,
    )
  },
}

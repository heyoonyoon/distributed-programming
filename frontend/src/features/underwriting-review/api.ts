import { request } from '../../lib/api/httpClient'
import type {
  ConfirmReviewRequest,
  ConfirmReviewResponse,
  PendingReview,
  ReviewApplicationDetail,
} from './types'

export const underwritingApi = {
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

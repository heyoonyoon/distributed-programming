package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.review.BenefitPaymentReview;

public record BenefitReviewResultResponse(Long claimId, String result, String claimStatus) {
    public static BenefitReviewResultResponse from(BenefitPaymentReview review) {
        return new BenefitReviewResultResponse(
                review.getClaim().getId(),
                review.getResult() == null ? null : review.getResult().name(),
                review.getClaim().getStatus().name());
    }
}

package com.distribution.insurance.review.dto;

import com.distribution.insurance.review.domain.BenefitPaymentReview;

public record BenefitReviewResultResponse(Long claimId, String result, String claimStatus) {
    public static BenefitReviewResultResponse from(BenefitPaymentReview review) {
        return new BenefitReviewResultResponse(
                review.getClaim().getId(),
                review.getResult() == null ? null : review.getResult().name(),
                review.getClaim().getStatus().name());
    }
}

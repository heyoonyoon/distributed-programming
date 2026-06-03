package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.review.BenefitPaymentReview;

public record BenefitReviewSummaryResponse(Long claimId, int requestAmount, String hospitalName, String claimStatus) {
    public static BenefitReviewSummaryResponse from(BenefitPaymentReview review) {
        var claim = review.getClaim();
        return new BenefitReviewSummaryResponse(
                claim.getId(), claim.getRequestAmount(), claim.getHospitalName(), claim.getStatus().name());
    }
}

package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.review.BenefitPaymentReview;

public record BenefitReviewDetailResponse(Long claimId, int requestAmount, String hospitalName,
                                          String diagnosisCode, String claimStatus, Long assignedStaffId) {
    public static BenefitReviewDetailResponse from(BenefitPaymentReview review) {
        var claim = review.getClaim();
        return new BenefitReviewDetailResponse(
                claim.getId(), claim.getRequestAmount(), claim.getHospitalName(),
                claim.getDiagnosisCode(), claim.getStatus().name(), review.getAssignedStaffId());
    }
}

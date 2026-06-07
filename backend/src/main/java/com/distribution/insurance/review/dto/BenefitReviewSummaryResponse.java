package com.distribution.insurance.review.dto;

import com.distribution.insurance.claim.domain.CarAccidentReport;
import com.distribution.insurance.claim.domain.Claim;
import com.distribution.insurance.claim.domain.HealthInsuranceClaim;
import com.distribution.insurance.review.domain.BenefitPaymentReview;

public record BenefitReviewSummaryResponse(Long claimId, int requestAmount, String claimType,
                                           String hospitalName, String accidentType, String claimStatus) {
    public static BenefitReviewSummaryResponse from(BenefitPaymentReview review) {
        Claim claim = review.getClaim();
        String hospitalName = claim instanceof HealthInsuranceClaim h ? h.getHospitalName() : null;
        String accidentType = claim instanceof CarAccidentReport c ? c.getAccidentType() : null;
        String claimType = claim instanceof CarAccidentReport ? "CAR_ACCIDENT" : "HEALTH";
        return new BenefitReviewSummaryResponse(
                claim.getId(), claim.getRequestAmount(), claimType, hospitalName, accidentType,
                claim.getStatus().name());
    }
}

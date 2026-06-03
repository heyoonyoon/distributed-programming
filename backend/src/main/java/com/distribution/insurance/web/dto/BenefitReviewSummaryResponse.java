package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.claim.CarAccidentReport;
import com.distribution.insurance.domain.claim.Claim;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.review.BenefitPaymentReview;

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

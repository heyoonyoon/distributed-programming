package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.claim.CarAccidentReport;
import com.distribution.insurance.domain.claim.Claim;
import com.distribution.insurance.domain.claim.HealthInsuranceClaim;
import com.distribution.insurance.domain.review.BenefitPaymentReview;

public record BenefitReviewDetailResponse(Long claimId, int requestAmount, String claimType,
                                          String hospitalName, String diagnosisCode,
                                          String accidentType, String accidentLocation, String vehicleNumber,
                                          Boolean hasInjury, String claimStatus, Long assignedStaffId) {
    public static BenefitReviewDetailResponse from(BenefitPaymentReview review) {
        Claim claim = review.getClaim();
        HealthInsuranceClaim h = claim instanceof HealthInsuranceClaim hc ? hc : null;
        CarAccidentReport c = claim instanceof CarAccidentReport cc ? cc : null;
        return new BenefitReviewDetailResponse(
                claim.getId(), claim.getRequestAmount(), c != null ? "CAR_ACCIDENT" : "HEALTH",
                h != null ? h.getHospitalName() : null,
                h != null ? h.getDiagnosisCode() : null,
                c != null ? c.getAccidentType() : null,
                c != null ? c.getAccidentLocation() : null,
                c != null ? c.getVehicleNumber() : null,
                c != null ? c.isHasInjury() : null,
                claim.getStatus().name(), review.getAssignedStaffId());
    }
}

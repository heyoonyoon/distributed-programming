package com.distribution.insurance.review.dto;

import com.distribution.insurance.claim.domain.CarAccidentReport;
import com.distribution.insurance.claim.domain.Claim;
import com.distribution.insurance.claim.domain.HealthInsuranceClaim;
import com.distribution.insurance.review.domain.BenefitPaymentReview;

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

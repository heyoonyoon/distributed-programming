package com.distribution.insurance.review.dto;

import com.distribution.insurance.review.domain.EnrollmentReview;

public record ReviewResultResponse(
        Long reviewId, String result, double surchargeRate, int adjustedPremium) {

    public static ReviewResultResponse from(EnrollmentReview review) {
        return new ReviewResultResponse(
                review.getId(), review.getResult().name(),
                review.getSurchargeRate(), review.getAdjustedPremium());
    }
}

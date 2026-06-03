package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.review.EnrollmentReview;

public record ReviewResultResponse(
        Long reviewId, String result, double surchargeRate, int adjustedPremium) {

    public static ReviewResultResponse from(EnrollmentReview review) {
        return new ReviewResultResponse(
                review.getId(), review.getResult().name(),
                review.getSurchargeRate(), review.getAdjustedPremium());
    }
}

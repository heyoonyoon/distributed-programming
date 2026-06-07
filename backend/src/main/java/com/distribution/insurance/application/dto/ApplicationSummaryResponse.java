package com.distribution.insurance.application.dto;

import com.distribution.insurance.application.domain.InsuranceApplication;

import java.time.LocalDateTime;

public record ApplicationSummaryResponse(
        Long applicationId, String status, LocalDateTime appliedAt,
        Long productId, String productName) {

    public static ApplicationSummaryResponse from(InsuranceApplication app) {
        return new ApplicationSummaryResponse(
                app.getId(), app.getStatus().name(), app.getAppliedAt(),
                app.getProduct().getId(), app.getProduct().getProductName());
    }
}

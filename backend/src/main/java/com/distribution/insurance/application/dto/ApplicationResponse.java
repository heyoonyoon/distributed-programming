package com.distribution.insurance.application.dto;

import com.distribution.insurance.application.domain.InsuranceApplication;

import java.time.LocalDateTime;

public record ApplicationResponse(Long applicationId, String status, LocalDateTime appliedAt) {
    public static ApplicationResponse from(InsuranceApplication app) {
        return new ApplicationResponse(app.getId(), app.getStatus().name(), app.getAppliedAt());
    }
}

package com.distribution.insurance.application.dto;

import com.distribution.insurance.application.domain.InsuranceApplication;

import java.time.LocalDateTime;

/** 심사 대기 목록 항목(UC13 2단계). */
public record PendingApplicationResponse(
        Long applicationId, LocalDateTime appliedAt, String applicantName,
        String productName, int basePremium) {

    public static PendingApplicationResponse from(InsuranceApplication app) {
        return new PendingApplicationResponse(
                app.getId(), app.getAppliedAt(), app.getApplicant().getName(),
                app.getProduct().getProductName(), app.getProduct().getBasePremium());
    }
}

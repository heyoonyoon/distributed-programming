package com.distribution.insurance.claim.dto;

import com.distribution.insurance.claim.domain.CarAccidentReport;

public record CarAccidentResultResponse(Long reportId, String status) {
    public static CarAccidentResultResponse from(CarAccidentReport report) {
        return new CarAccidentResultResponse(report.getId(), report.getStatus().name());
    }
}

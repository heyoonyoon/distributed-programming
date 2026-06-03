package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.claim.CarAccidentReport;

public record CarAccidentResultResponse(Long reportId, String status) {
    public static CarAccidentResultResponse from(CarAccidentReport report) {
        return new CarAccidentResultResponse(report.getId(), report.getStatus().name());
    }
}

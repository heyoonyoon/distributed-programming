package com.distribution.insurance.domain.review;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 금융감독원에서 조회되는 자동차 사고이력(UC15). 텍스트 구현에서는 mock. */
@Embeddable
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AccidentHistory {

    private int accidentCount;     // 최근 3년 사고 건수
    private int totalPaidAmount;   // 보험금 지급 총액
    private String licenseStatus;  // VALID, SUSPENDED, REVOKED
    private LocalDateTime fetchedAt;

    public AccidentHistory(int accidentCount, int totalPaidAmount,
                           String licenseStatus, LocalDateTime fetchedAt) {
        this.accidentCount = accidentCount;
        this.totalPaidAmount = totalPaidAmount;
        this.licenseStatus = licenseStatus;
        this.fetchedAt = fetchedAt;
    }
}

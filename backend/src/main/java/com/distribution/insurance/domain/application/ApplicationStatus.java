package com.distribution.insurance.domain.application;

/** 가입 신청 진행 상태. 조건부 여부는 여기 두지 않는다(ADR 0003 — ReviewResult가 소유). */
public enum ApplicationStatus {
    PENDING, APPROVED, REJECTED, CANCELLED
}

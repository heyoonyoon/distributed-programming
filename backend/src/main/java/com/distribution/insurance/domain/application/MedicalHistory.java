package com.distribution.insurance.domain.application;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 의료보험 가입 시 고지 항목(UC02). */
@Embeddable
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class MedicalHistory {

    @jakarta.persistence.Column(length = 500)
    private String currentConditions;     // 현재 병력
    @jakarta.persistence.Column(length = 500)
    private String pastHospitalization;   // 과거 입원 이력
    @jakarta.persistence.Column(length = 500)
    private String medications;           // 복용 중인 약물

    public MedicalHistory(String currentConditions, String pastHospitalization, String medications) {
        this.currentConditions = currentConditions;
        this.pastHospitalization = pastHospitalization;
        this.medications = medications;
    }
}

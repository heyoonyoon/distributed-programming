package com.distribution.insurance.application.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 자동차보험 가입 시 추가정보(UC02). */
@Embeddable
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class VehicleInfo {

    private String plateNumber;          // 차량번호
    private String vehicleType;          // 차종
    private int modelYear;               // 연식
    private int drivingExperienceYears;  // 운전 경력(년)

    public VehicleInfo(String plateNumber, String vehicleType, int modelYear, int drivingExperienceYears) {
        this.plateNumber = plateNumber;
        this.vehicleType = vehicleType;
        this.modelYear = modelYear;
        this.drivingExperienceYears = drivingExperienceYears;
    }
}

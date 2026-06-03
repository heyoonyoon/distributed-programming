package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.application.MedicalHistory;
import com.distribution.insurance.domain.application.VehicleInfo;
import jakarta.validation.constraints.NotNull;

/** 가입 신청 요청. 개인정보는 인증 주체에서 읽으므로 받지 않는다(ADR 0002). */
public record CreateApplicationRequest(
        @NotNull Long productId,
        VehicleInfoDto vehicleInfo,
        MedicalHistoryDto medicalHistory) {

    public record VehicleInfoDto(String plateNumber, String vehicleType,
                                 int modelYear, int drivingExperienceYears) {}

    public record MedicalHistoryDto(String currentConditions, String pastHospitalization,
                                    String medications) {}

    public VehicleInfo toVehicleInfo() {
        if (vehicleInfo == null) return null;
        return new VehicleInfo(vehicleInfo.plateNumber(), vehicleInfo.vehicleType(),
                vehicleInfo.modelYear(), vehicleInfo.drivingExperienceYears());
    }

    public MedicalHistory toMedicalHistory() {
        if (medicalHistory == null) return null;
        return new MedicalHistory(medicalHistory.currentConditions(),
                medicalHistory.pastHospitalization(), medicalHistory.medications());
    }
}

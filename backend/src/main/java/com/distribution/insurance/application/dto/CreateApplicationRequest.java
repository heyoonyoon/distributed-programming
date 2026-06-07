package com.distribution.insurance.application.dto;

import com.distribution.insurance.application.domain.MedicalHistory;
import com.distribution.insurance.application.domain.VehicleInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 가입 신청 요청. 개인정보는 인증 주체에서 읽으므로 받지 않는다(ADR 0002). */
public record CreateApplicationRequest(
        @NotNull Long productId,
        @Valid VehicleInfoDto vehicleInfo,
        @Valid MedicalHistoryDto medicalHistory) {

    public record VehicleInfoDto(
            @NotBlank String plateNumber,
            @NotBlank String vehicleType,
            @Positive int modelYear,
            @PositiveOrZero int drivingExperienceYears) {}

    public record MedicalHistoryDto(
            @NotBlank @Size(max = 500) String currentConditions,
            @NotBlank @Size(max = 500) String pastHospitalization,
            @NotBlank @Size(max = 500) String medications) {}

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

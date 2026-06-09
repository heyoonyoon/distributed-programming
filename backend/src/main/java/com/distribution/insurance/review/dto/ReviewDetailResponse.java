package com.distribution.insurance.review.dto;

import com.distribution.insurance.application.domain.InsuranceApplication;
import com.distribution.insurance.application.domain.MedicalHistory;
import com.distribution.insurance.application.domain.VehicleInfo;
import com.distribution.insurance.review.domain.AccidentHistory;

import java.time.LocalDate;

/** 심사 상세(UC13 4단계 + 자동차건 UC15 참조정보). */
public record ReviewDetailResponse(
        Long applicationId,
        String applicantName, LocalDate birthDate, String ssn,
        String productName, int basePremium,
        VehicleInfoView vehicleInfo, MedicalHistoryView medicalHistory,
        AccidentHistoryView accidentHistory) {

    public record VehicleInfoView(String plateNumber, String vehicleType,
                                  int modelYear, int drivingExperienceYears) {}

    public record MedicalHistoryView(String currentConditions, String pastHospitalization,
                                     String medications) {}

    public record AccidentHistoryView(int accidentCount, int totalPaidAmount, String licenseStatus) {}

    public static ReviewDetailResponse from(InsuranceApplication app, AccidentHistory accidentHistory) {
        VehicleInfo v = app.getVehicleInfo();
        MedicalHistory m = app.getMedicalHistory();
        return new ReviewDetailResponse(
                app.getId(),
                app.getApplicant().getName(), app.getApplicant().getBirthDate(), app.getApplicant().getSsn(),
                app.getProduct().getProductName(), app.getProduct().getBasePremium(),
                v == null ? null : new VehicleInfoView(v.getPlateNumber(), v.getVehicleType(),
                        v.getModelYear(), v.getDrivingExperienceYears()),
                m == null ? null : new MedicalHistoryView(m.getCurrentConditions(),
                        m.getPastHospitalization(), m.getMedications()),
                accidentHistory == null ? null : new AccidentHistoryView(
                        accidentHistory.getAccidentCount(), accidentHistory.getTotalPaidAmount(),
                        accidentHistory.getLicenseStatus()));
    }
}

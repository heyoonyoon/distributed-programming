package com.distribution.insurance.application.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AdditionalInfoTest {

    @Test
    void 차량정보는_네_필드를_보존한다() {
        VehicleInfo v = new VehicleInfo("12가3456", "승용차", 2020, 5);
        assertThat(v.getPlateNumber()).isEqualTo("12가3456");
        assertThat(v.getVehicleType()).isEqualTo("승용차");
        assertThat(v.getModelYear()).isEqualTo(2020);
        assertThat(v.getDrivingExperienceYears()).isEqualTo(5);
    }

    @Test
    void 의료고지는_세_필드를_보존한다() {
        MedicalHistory m = new MedicalHistory("고혈압", "2019년 입원", "혈압약");
        assertThat(m.getCurrentConditions()).isEqualTo("고혈압");
        assertThat(m.getPastHospitalization()).isEqualTo("2019년 입원");
        assertThat(m.getMedications()).isEqualTo("혈압약");
    }
}

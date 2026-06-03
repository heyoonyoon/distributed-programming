package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.application.MedicalHistory;
import com.distribution.insurance.domain.application.VehicleInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateApplicationRequestTest {

    @Test
    void 차량블록은_VehicleInfo로_변환된다() {
        var req = new CreateApplicationRequest(
                10L,
                new CreateApplicationRequest.VehicleInfoDto("12가3456", "승용차", 2020, 5),
                null);
        VehicleInfo v = req.toVehicleInfo();
        assertThat(v.getPlateNumber()).isEqualTo("12가3456");
        assertThat(req.toMedicalHistory()).isNull();
    }

    @Test
    void 의료블록은_MedicalHistory로_변환된다() {
        var req = new CreateApplicationRequest(
                10L, null,
                new CreateApplicationRequest.MedicalHistoryDto("고혈압", "없음", "혈압약"));
        MedicalHistory m = req.toMedicalHistory();
        assertThat(m.getCurrentConditions()).isEqualTo("고혈압");
        assertThat(req.toVehicleInfo()).isNull();
    }
}

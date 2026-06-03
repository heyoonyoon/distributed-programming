package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.application.InsuranceApplication;
import com.distribution.insurance.domain.application.VehicleInfo;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.review.AccidentHistory;
import com.distribution.insurance.domain.user.Policyholder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewDtoTest {

    @Test
    void 상세응답은_자동차건의_사고이력을_포함한다() {
        Policyholder ph = new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌");
        var product = new CarInsuranceProduct("안심드라이브", "대인대물", 45000, "승용차", "가족한정");
        var app = new InsuranceApplication(ph, product, new VehicleInfo("12가3456", "승용차", 2020, 5), null);
        var history = new AccidentHistory(2, 2_000_000, "VALID", LocalDateTime.now());

        ReviewDetailResponse res = ReviewDetailResponse.from(app, history);

        assertThat(res.applicantName()).isEqualTo("홍길동");
        assertThat(res.vehicleInfo().plateNumber()).isEqualTo("12가3456");
        assertThat(res.medicalHistory()).isNull();
        assertThat(res.accidentHistory().accidentCount()).isEqualTo(2);
    }
}

package com.distribution.insurance.claim.domain;

import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.product.domain.CarInsuranceProduct;
import com.distribution.insurance.user.domain.Policyholder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class CarAccidentReportTest {

    private InsuranceContract carContract() {
        Policyholder ph = new Policyholder("홍", "h@t.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "110-123-456789");
        var product = new CarInsuranceProduct("자동차", "대물", 50000, "SEDAN", "ALL");
        return new InsuranceContract(ph, product, 50000, LocalDate.now());
    }

    @Test
    void 생성하면_PENDING이고_사고정보가_보존된다() {
        CarAccidentReport report = new CarAccidentReport(
                carContract(), LocalDate.now(), "서울 강남", "쌍방", "12가3456", true, 2);

        assertThat(report.getStatus()).isEqualTo(ClaimStatus.PENDING);
        assertThat(report.getAccidentLocation()).isEqualTo("서울 강남");
        assertThat(report.getAccidentType()).isEqualTo("쌍방");
        assertThat(report.getVehicleNumber()).isEqualTo("12가3456");
        assertThat(report.isHasInjury()).isTrue();
        assertThat(report.getInjuredCount()).isEqualTo(2);
        assertThat(report.getRequestAmount()).isEqualTo(0);
    }

    private CarAccidentReport sampleReport() {
        return new CarAccidentReport(null, java.time.LocalDate.now(), "서울", "단독", "12가3456", false, 0);
    }

    @Test
    void assessPayout는_사정금액을_청구금액으로_기록한다() {
        CarAccidentReport report = sampleReport();
        report.assessPayout(3_000_000);
        assertThat(report.getRequestAmount()).isEqualTo(3_000_000);
    }

    @Test
    void assessPayout에_0이하면_400성_예외() {
        CarAccidentReport report = sampleReport();
        assertThatThrownBy(() -> report.assessPayout(0))
                .isInstanceOf(com.distribution.insurance.common.service.InvalidRequestException.class);
    }

    @Test
    void 첨부를_추가하면_컬렉션에_쌓인다() {
        CarAccidentReport report = new CarAccidentReport(
                carContract(), LocalDate.now(), "서울", "단독", "12가3456", false, 0);
        report.addAttachment(new ClaimAttachment("p.jpg", "image/jpeg", 10L, "/p/p.jpg"));
        assertThat(report.getAttachments()).hasSize(1);
    }
}

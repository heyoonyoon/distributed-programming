package com.distribution.insurance.application.domain;

import com.distribution.insurance.product.domain.CarInsuranceProduct;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.common.service.IllegalStateTransitionException;
import com.distribution.insurance.common.service.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsuranceApplicationTest {

    private Policyholder applicant() {
        return new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌");
    }

    private CarInsuranceProduct carProduct() {
        return new CarInsuranceProduct("안심드라이브", "대인대물", 45000, "승용차", "가족한정");
    }

    private HealthInsuranceProduct healthProduct() {
        return new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120);
    }

    private VehicleInfo vehicle() {
        return new VehicleInfo("12가3456", "승용차", 2020, 5);
    }

    private MedicalHistory medical() {
        return new MedicalHistory("없음", "없음", "없음");
    }

    @Test
    void 생성시_상태는_PENDING이고_신청일시가_기록된다() {
        InsuranceApplication app = new InsuranceApplication(applicant(), carProduct(), vehicle(), null);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(app.getAppliedAt()).isNotNull();
    }

    @Test
    void 자동차상품은_차량정보_필수_의료고지_금지() {
        assertThatThrownBy(() -> new InsuranceApplication(applicant(), carProduct(), null, null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> new InsuranceApplication(applicant(), carProduct(), vehicle(), medical()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void 의료상품은_의료고지_필수_차량정보_금지() {
        assertThatThrownBy(() -> new InsuranceApplication(applicant(), healthProduct(), null, null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> new InsuranceApplication(applicant(), healthProduct(), vehicle(), medical()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void PENDING_신청은_취소되어_CANCELLED가_된다() {
        InsuranceApplication app = new InsuranceApplication(applicant(), healthProduct(), null, medical());
        app.cancel();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
    }

    @Test
    void 이미_취소된_신청을_다시_취소하면_예외() {
        InsuranceApplication app = new InsuranceApplication(applicant(), healthProduct(), null, medical());
        app.cancel();
        assertThatThrownBy(app::cancel)
                .isInstanceOf(IllegalStateTransitionException.class);
    }
}

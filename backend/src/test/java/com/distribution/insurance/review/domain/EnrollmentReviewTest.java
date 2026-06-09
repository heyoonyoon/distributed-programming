package com.distribution.insurance.review.domain;

import com.distribution.insurance.application.domain.InsuranceApplication;
import com.distribution.insurance.application.domain.MedicalHistory;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.user.domain.InsuranceEmployee;
import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.common.service.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class EnrollmentReviewTest {

    private InsuranceApplication app() {
        Policyholder ph = new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌");
        var product = new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120);
        return new InsuranceApplication(ph, product, null, new MedicalHistory("없음", "없음", "없음"));
    }

    private InsuranceEmployee reviewer() {
        return new InsuranceEmployee("심사역", "e@test.com", "010", "pw", "심사팀", 0);
    }

    @Test
    void 일반승인은_adjustedPremium이_basePremium과_같다() {
        EnrollmentReview r = new EnrollmentReview(app(), reviewer());
        r.confirm(ReviewResult.APPROVED, "이상 없음", null, 30000);
        assertThat(r.getResult()).isEqualTo(ReviewResult.APPROVED);
        assertThat(r.getAdjustedPremium()).isEqualTo(30000);
        assertThat(r.getReviewedAt()).isNotNull();
    }

    @Test
    void 조건부승인은_할증된_보험료를_계산한다() {
        EnrollmentReview r = new EnrollmentReview(app(), reviewer());
        r.confirm(ReviewResult.CONDITIONAL, "사고이력 다수", 0.2, 30000);
        assertThat(r.getSurchargeRate()).isEqualTo(0.2);
        assertThat(r.getAdjustedPremium()).isEqualTo(36000);
    }

    @Test
    void 조건부승인인데_할증율이_없으면_예외() {
        EnrollmentReview r = new EnrollmentReview(app(), reviewer());
        assertThatThrownBy(() -> r.confirm(ReviewResult.CONDITIONAL, "사유", null, 30000))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> r.confirm(ReviewResult.CONDITIONAL, "사유", 0.0, 30000))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void 일반승인인데_할증율이_있으면_예외() {
        EnrollmentReview r = new EnrollmentReview(app(), reviewer());
        assertThatThrownBy(() -> r.confirm(ReviewResult.APPROVED, "사유", 0.2, 30000))
                .isInstanceOf(InvalidRequestException.class);
    }
}

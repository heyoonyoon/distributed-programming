package com.distribution.insurance.application.domain;

import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.common.service.IllegalStateTransitionException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationTransitionTest {

    private InsuranceApplication pendingHealthApp() {
        Policyholder ph = new Policyholder("홍길동", "h@test.com", "010", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌");
        var product = new HealthInsuranceProduct("건강플러스", "암 보장", 30000, 120);
        return new InsuranceApplication(ph, product, null, new MedicalHistory("없음", "없음", "없음"));
    }

    @Test
    void PENDING에서_승인되면_APPROVED() {
        InsuranceApplication app = pendingHealthApp();
        app.markApproved();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    }

    @Test
    void PENDING에서_반려되면_REJECTED() {
        InsuranceApplication app = pendingHealthApp();
        app.markRejected();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void 이미_심사된_건을_다시_전이하면_예외() {
        InsuranceApplication app = pendingHealthApp();
        app.markApproved();
        assertThatThrownBy(app::markRejected)
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThatThrownBy(app::markApproved)
                .isInstanceOf(IllegalStateTransitionException.class);
    }
}

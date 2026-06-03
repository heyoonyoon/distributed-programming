package com.distribution.insurance.domain.contract;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class PaymentTest {

    private InsuranceContract contract() {
        return new InsuranceContract(TestFixtures.policyholder(), TestFixtures.healthProduct(),
                30000, LocalDate.of(2026, 1, 1));
    }

    @Test
    void 성공_납부는_SUCCESS이고_금액과_수단을_보존한다() {
        Payment p = Payment.success(contract(), 30000, PaymentMethod.CARD);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(p.getAmount()).isEqualTo(30000);
        assertThat(p.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(p.getPaidAt()).isNotNull();
    }

    @Test
    void 실패_납부는_FAILED이고_영수증을_만들_수_없다() {
        Payment p = Payment.failed(contract(), 30000, PaymentMethod.CARD);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThatThrownBy(p::getReceipt)
                .isInstanceOf(com.distribution.insurance.service.InvalidRequestException.class);
    }

    @Test
    void 성공_납부의_영수증은_금액을_담는다() {
        Payment p = Payment.success(contract(), 30000, PaymentMethod.TRANSFER);
        assertThat(p.getReceipt().amount()).isEqualTo(30000);
    }
}

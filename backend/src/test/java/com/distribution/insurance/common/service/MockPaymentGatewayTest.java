package com.distribution.insurance.common.service;

import com.distribution.insurance.contract.domain.PaymentMethod;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MockPaymentGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway();

    @Test
    void 일반_결제정보는_성공한다() {
        PaymentGateway.Result r = gateway.charge(PaymentMethod.CARD, 30000, "1234-5678-9012-3456");
        assertThat(r.success()).isTrue();
    }

    @Test
    void 영영영영으로_끝나는_결제정보는_실패한다() {
        PaymentGateway.Result r = gateway.charge(PaymentMethod.CARD, 30000, "1234-5678-9012-0000");
        assertThat(r.success()).isFalse();
        assertThat(r.reason()).isNotBlank();
    }
}

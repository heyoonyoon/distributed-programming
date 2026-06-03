package com.distribution.insurance.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MockBenefitTransferGatewayTest {

    private final BenefitTransferGateway gateway = new MockBenefitTransferGateway();

    @Test
    void 정상_계좌면_송금_성공() {
        BenefitTransferGateway.Result r = gateway.transfer("110-123-456789", 500000);
        assertThat(r.success()).isTrue();
    }

    @Test
    void 계좌가_0000으로_끝나면_송금_실패() {
        BenefitTransferGateway.Result r = gateway.transfer("110-123-450000", 500000);
        assertThat(r.success()).isFalse();
        assertThat(r.reason()).isNotBlank();
    }

    @Test
    void 계좌가_비면_송금_실패() {
        assertThat(gateway.transfer("", 500000).success()).isFalse();
        assertThat(gateway.transfer(null, 500000).success()).isFalse();
    }
}

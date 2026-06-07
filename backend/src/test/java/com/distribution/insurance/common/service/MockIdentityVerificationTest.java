package com.distribution.insurance.common.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockIdentityVerificationTest {

    IdentityVerificationService service = new MockIdentityVerification();

    @Test
    void 본인인증_목은_항상_성공한다() {
        assertThat(service.verify(1L)).isTrue();
    }
}

package com.distribution.insurance.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    JwtTokenProvider provider = new JwtTokenProvider(
            "test-secret-key-please-change-this-to-a-long-enough-value-32byte!!",
            3600000L);

    @Test
    void 토큰을_발급하고_파싱하면_userId와_userType이_복원된다() {
        String token = provider.createToken(42L, "POLICYHOLDER");

        assertThat(provider.validate(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo(42L);
        assertThat(provider.getUserType(token)).isEqualTo("POLICYHOLDER");
    }

    @Test
    void 위조된_토큰은_검증에_실패한다() {
        String token = provider.createToken(42L, "POLICYHOLDER");
        assertThat(provider.validate(token + "tampered")).isFalse();
    }
}

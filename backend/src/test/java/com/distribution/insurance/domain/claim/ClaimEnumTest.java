package com.distribution.insurance.domain.claim;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ClaimEnumTest {

    @Test
    void ClaimStatus는_6개_값을_가진다() {
        assertThat(ClaimStatus.values())
                .containsExactly(ClaimStatus.PENDING, ClaimStatus.IN_REVIEW,
                        ClaimStatus.APPROVED, ClaimStatus.REJECTED,
                        ClaimStatus.COMPLETED, ClaimStatus.FAILED);
    }

    @Test
    void ClaimComplexity는_SIMPLE과_COMPLEX를_가진다() {
        assertThat(ClaimComplexity.values())
                .containsExactly(ClaimComplexity.SIMPLE, ClaimComplexity.COMPLEX);
    }
}

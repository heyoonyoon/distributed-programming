package com.distribution.insurance.review.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReviewResultTest {

    @Test
    void 세_가지_결과값을_가진다() {
        assertThat(ReviewResult.values())
                .containsExactlyInAnyOrder(
                        ReviewResult.APPROVED,
                        ReviewResult.CONDITIONAL,
                        ReviewResult.REJECTED);
    }
}

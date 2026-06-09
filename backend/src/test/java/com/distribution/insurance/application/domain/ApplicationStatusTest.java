package com.distribution.insurance.application.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStatusTest {

    @Test
    void 네_가지_상태값을_가진다() {
        assertThat(ApplicationStatus.values())
                .containsExactlyInAnyOrder(
                        ApplicationStatus.PENDING,
                        ApplicationStatus.APPROVED,
                        ApplicationStatus.REJECTED,
                        ApplicationStatus.CANCELLED);
    }
}

package com.distribution.insurance.review.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class AccidentHistoryTest {

    @Test
    void 사고이력_필드를_보존한다() {
        LocalDateTime now = LocalDateTime.now();
        AccidentHistory h = new AccidentHistory(2, 5_000_000, "VALID", now);
        assertThat(h.getAccidentCount()).isEqualTo(2);
        assertThat(h.getTotalPaidAmount()).isEqualTo(5_000_000);
        assertThat(h.getLicenseStatus()).isEqualTo("VALID");
        assertThat(h.getFetchedAt()).isEqualTo(now);
    }
}

package com.distribution.insurance.service;

import com.distribution.insurance.domain.review.AccidentHistory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockAccidentHistoryClientTest {

    private final AccidentHistoryClient client = new MockAccidentHistoryClient();

    @Test
    void 같은_주민번호는_항상_같은_결과를_준다() {
        AccidentHistory a = client.fetch("900101-1234567");
        AccidentHistory b = client.fetch("900101-1234567");
        assertThat(a.getAccidentCount()).isEqualTo(b.getAccidentCount());
        assertThat(a.getLicenseStatus()).isEqualTo(b.getLicenseStatus());
    }

    @Test
    void 사고건수는_0이상_3이하이고_지급총액과_정합() {
        AccidentHistory h = client.fetch("910101-2345678");
        assertThat(h.getAccidentCount()).isBetween(0, 3);
        assertThat(h.getTotalPaidAmount()).isEqualTo(h.getAccidentCount() * 1_000_000);
        assertThat(h.getFetchedAt()).isNotNull();
    }
}

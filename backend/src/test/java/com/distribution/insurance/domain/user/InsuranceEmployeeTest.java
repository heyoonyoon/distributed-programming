package com.distribution.insurance.domain.user;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InsuranceEmployeeTest {

    @Test
    void 업무를_배정하면_currentLoad가_1_증가한다() {
        InsuranceEmployee e = new InsuranceEmployee("김직원", "k@t.com", "010", "pw", "심사1팀", 2);
        e.assignWork();
        assertThat(e.getCurrentLoad()).isEqualTo(3);
    }
}

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

    @Test
    void releaseWork은_currentLoad를_1_감소시킨다() {
        InsuranceEmployee e = new InsuranceEmployee("김직원", "k@t.com", "010", "pw", "심사1팀", 3);
        e.releaseWork();
        assertThat(e.getCurrentLoad()).isEqualTo(2);
    }

    @Test
    void releaseWork은_0_아래로_내려가지_않는다() {
        InsuranceEmployee e = new InsuranceEmployee("김직원", "k@t.com", "010", "pw", "심사1팀", 0);
        e.releaseWork();
        assertThat(e.getCurrentLoad()).isEqualTo(0);
    }
}

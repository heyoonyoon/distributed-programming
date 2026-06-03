package com.distribution.insurance.service;

import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;

import java.time.LocalDate;

/** 서비스 테스트용 도메인 픽스처. 실제 생성자 시그니처(이슈 A/B 도메인)에 맞춰 작성. */
final class TestEntities {

    private TestEntities() {}

    static Policyholder policyholder() {
        return new Policyholder("홍길동", "hong@test.com", "010-1111-2222", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌");
    }

    static HealthInsuranceProduct healthProduct() {
        return new HealthInsuranceProduct("실손의료", "설명", 30000, 120);
    }

    static InsuranceEmployee employee() {
        return new InsuranceEmployee("김직원", "emp@test.com", "010-3333-4444", "pw", "심사팀", 0);
    }
}

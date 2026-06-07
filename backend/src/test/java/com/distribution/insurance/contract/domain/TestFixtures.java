package com.distribution.insurance.contract.domain;

import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.user.domain.Policyholder;

import java.time.LocalDate;

/** 테스트용 도메인 픽스처. 실제 생성자 시그니처(이슈 A 도메인)에 맞춰 작성. */
final class TestFixtures {

    private TestFixtures() {}

    static Policyholder policyholder() {
        return new Policyholder("홍길동", "hong@test.com", "010-1111-2222", "pw",
                "900101-1234567", LocalDate.of(1990, 1, 1), "주소", "계좌");
    }

    static HealthInsuranceProduct healthProduct() {
        return new HealthInsuranceProduct("실손의료", "설명", 30000, 120);
    }
}

package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;
import org.hibernate.Hibernate;

/** 상품 종류 코드 매핑(공유). 지연 로딩 프록시도 안전하게 처리한다. */
public final class ProductTypeMapper {

    private ProductTypeMapper() {}

    public static String typeOf(InsuranceProduct product) {
        Object unproxied = Hibernate.unproxy(product);
        if (unproxied instanceof HealthInsuranceProduct) return "HEALTH";
        if (unproxied instanceof CarInsuranceProduct) return "CAR";
        // 내부 매핑 누락(새 상품 종류 추가 시)은 서버 오류(500). IllegalStateException은
        // 전역 핸들러에서 403으로 매핑되므로 일부러 쓰지 않는다.
        throw new RuntimeException(
                "알 수 없는 상품 종류: " + unproxied.getClass().getSimpleName());
    }
}

package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;

import java.time.LocalDate;

/** UC08 2단계: 계약번호, 상품명, 보험종류, 계약기간, 월보험료. */
public record ContractSummaryResponse(
        Long contractId, String productName, String productType,
        LocalDate startDate, LocalDate endDate, int monthlyPremium, String status) {

    public static ContractSummaryResponse from(InsuranceContract c) {
        return new ContractSummaryResponse(
                c.getId(), c.getProduct().getProductName(), typeOf(c.getProduct()),
                c.getStartDate(), c.getEndDate(), c.getMonthlyPremium(), c.getStatus().name());
    }

    static String typeOf(InsuranceProduct product) {
        Object unproxied = org.hibernate.Hibernate.unproxy(product);
        if (unproxied instanceof HealthInsuranceProduct) return "HEALTH";
        if (unproxied instanceof CarInsuranceProduct) return "CAR";
        throw new RuntimeException("알 수 없는 상품 종류: " + unproxied.getClass().getSimpleName());
    }
}

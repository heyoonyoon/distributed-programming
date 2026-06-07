package com.distribution.insurance.contract.dto;

import com.distribution.insurance.contract.domain.InsuranceContract;

import java.time.LocalDate;
import com.distribution.insurance.product.dto.ProductTypeMapper;

/** UC08 2단계: 계약번호, 상품명, 보험종류, 계약기간, 월보험료. */
public record ContractSummaryResponse(
        Long contractId, String productName, String productType,
        LocalDate startDate, LocalDate endDate, int monthlyPremium, String status) {

    public static ContractSummaryResponse from(InsuranceContract c) {
        return new ContractSummaryResponse(
                c.getId(), c.getProduct().getProductName(), ProductTypeMapper.typeOf(c.getProduct()),
                c.getStartDate(), c.getEndDate(), c.getMonthlyPremium(), c.getStatus().name());
    }
}

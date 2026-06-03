package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.CoverageItem;

import java.time.LocalDate;
import java.util.List;

/**
 * UC08 4단계 상세. 도메인 실재분만 노출(spec/grill): 보장항목·보장금액·월보험료·기간·상태.
 * 결제수단은 이슈 A에서 "미등록" 상수. 수익자·특약은 제외.
 */
public record ContractDetailResponse(
        Long contractId, String productName, String productType,
        LocalDate startDate, LocalDate endDate, int monthlyPremium, String status,
        String paymentMethod, List<CoverageResponse> coverageItems) {

    public record CoverageResponse(String itemName, int coverageLimit, int deductible) {
        static CoverageResponse from(CoverageItem ci) {
            return new CoverageResponse(ci.getItemName(), ci.getCoverageLimit(), ci.getDeductible());
        }
    }

    public static ContractDetailResponse from(InsuranceContract c) {
        List<CoverageResponse> items = c.getProduct().getCoverageItems().stream()
                .map(CoverageResponse::from)
                .toList();
        return new ContractDetailResponse(
                c.getId(), c.getProduct().getProductName(),
                ProductTypeMapper.typeOf(c.getProduct()),
                c.getStartDate(), c.getEndDate(), c.getMonthlyPremium(), c.getStatus().name(),
                "미등록", items);  // 이슈 B에서 실제 결제수단으로 대체
    }
}

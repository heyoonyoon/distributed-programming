package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.product.CoverageItem;
import com.distribution.insurance.domain.product.HealthInsuranceProduct;
import com.distribution.insurance.domain.product.InsuranceProduct;

import java.util.stream.Collectors;

public record ProductSummaryResponse(
        Long id, String productName, String coverageSummary,
        int monthlyPremium, String productType) {

    public static ProductSummaryResponse from(InsuranceProduct product) {
        String summary = product.getCoverageItems().stream()
                .map(CoverageItem::getItemName)
                .limit(3)
                .collect(Collectors.joining(", "));
        return new ProductSummaryResponse(
                product.getId(), product.getProductName(), summary,
                product.getBasePremium(), typeOf(product));
    }

    static String typeOf(InsuranceProduct product) {
        if (product instanceof HealthInsuranceProduct) return "HEALTH";
        if (product instanceof CarInsuranceProduct) return "CAR";
        throw new IllegalStateException(
                "알 수 없는 상품 종류: " + product.getClass().getSimpleName());
    }
}

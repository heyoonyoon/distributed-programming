package com.distribution.insurance.product.dto;

import com.distribution.insurance.product.domain.CoverageItem;
import com.distribution.insurance.product.domain.InsuranceProduct;

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
                product.getBasePremium(), ProductTypeMapper.typeOf(product));
    }
}

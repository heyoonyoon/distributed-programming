package com.distribution.insurance.product.dto;

import com.distribution.insurance.product.domain.InsuranceProduct;

import java.util.List;

public record ProductDetailResponse(
        Long id, String productName, String productType, String description,
        int monthlyPremium, List<CoverageItemResponse> coverageItems) {

    public static ProductDetailResponse from(InsuranceProduct product) {
        List<CoverageItemResponse> items = product.getCoverageItems().stream()
                .map(CoverageItemResponse::from)
                .toList();
        return new ProductDetailResponse(
                product.getId(), product.getProductName(),
                ProductTypeMapper.typeOf(product), product.getDescription(),
                product.getBasePremium(), items);
    }
}

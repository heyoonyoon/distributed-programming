package com.distribution.insurance.product.dto;

import com.distribution.insurance.product.domain.CoverageItem;

public record CoverageItemResponse(String itemName, int coverageLimit, int deductible) {

    public static CoverageItemResponse from(CoverageItem item) {
        return new CoverageItemResponse(
                item.getItemName(), item.getCoverageLimit(), item.getDeductible());
    }
}

package com.distribution.insurance.web.dto;

import com.distribution.insurance.domain.product.CoverageItem;

public record CoverageItemResponse(String itemName, int coverageLimit, int deductible) {

    public static CoverageItemResponse from(CoverageItem item) {
        return new CoverageItemResponse(
                item.getItemName(), item.getCoverageLimit(), item.getDeductible());
    }
}

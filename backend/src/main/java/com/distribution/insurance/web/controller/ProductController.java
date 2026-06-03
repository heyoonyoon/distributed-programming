package com.distribution.insurance.web.controller;

import com.distribution.insurance.domain.product.ProductType;
import com.distribution.insurance.service.ProductService;
import com.distribution.insurance.web.dto.ProductDetailResponse;
import com.distribution.insurance.web.dto.ProductSummaryResponse;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@Validated
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public List<ProductSummaryResponse> list(
            @RequestParam ProductType type,
            @RequestParam(required = false) @Min(0) Integer minPremium,
            @RequestParam(required = false) @Min(0) Integer maxPremium,
            @RequestParam(required = false) String keyword) {
        if (minPremium != null && maxPremium != null && minPremium > maxPremium) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "minPremium은 maxPremium보다 클 수 없습니다.");
        }
        return productService.search(type, minPremium, maxPremium, keyword);
    }

    @GetMapping("/products/{id}")
    public ProductDetailResponse detail(@PathVariable Long id) {
        return productService.detail(id);
    }
}

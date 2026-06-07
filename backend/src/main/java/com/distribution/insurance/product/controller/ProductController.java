package com.distribution.insurance.product.controller;

import com.distribution.insurance.product.domain.ProductType;
import com.distribution.insurance.product.service.ProductService;
import com.distribution.insurance.product.dto.ProductDetailResponse;
import com.distribution.insurance.product.dto.ProductSummaryResponse;
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

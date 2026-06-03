package com.distribution.insurance.domain.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coverage_item")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CoverageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String itemName;
    private int coverageLimit;
    private int deductible;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private InsuranceProduct product;

    public CoverageItem(String itemName, int coverageLimit, int deductible) {
        this.itemName = itemName;
        this.coverageLimit = coverageLimit;
        this.deductible = deductible;
    }

    /** InsuranceProduct.addCoverageItem에서만 호출(연관관계 주인 설정). */
    void assignProduct(InsuranceProduct product) {
        this.product = product;
    }
}

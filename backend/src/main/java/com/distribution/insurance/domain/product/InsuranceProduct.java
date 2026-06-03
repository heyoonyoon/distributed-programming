package com.distribution.insurance.domain.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "insurance_product")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "product_type")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract class InsuranceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    @Column(length = 1000)
    private String description;

    private int basePremium;

    @Getter(lombok.AccessLevel.NONE)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CoverageItem> coverageItems = new ArrayList<>();

    protected InsuranceProduct(String productName, String description, int basePremium) {
        this.productName = productName;
        this.description = description;
        this.basePremium = basePremium;
    }

    /** 외부에서 add/clear로 불변식을 우회하지 못하도록 읽기전용 뷰로 노출. */
    public List<CoverageItem> getCoverageItems() {
        return Collections.unmodifiableList(coverageItems);
    }

    /** 보장항목 추가 — 양방향 연관관계를 한 곳에서 일관되게 설정. */
    public void addCoverageItem(CoverageItem item) {
        coverageItems.add(item);
        item.assignProduct(this);
    }
}

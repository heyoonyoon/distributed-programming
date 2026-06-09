package com.distribution.insurance.product.domain;

/** 요청 파라미터(type)와 엔티티 클래스를 잇는 보험 종류. */
public enum ProductType {
    HEALTH(HealthInsuranceProduct.class),
    CAR(CarInsuranceProduct.class);

    private final Class<? extends InsuranceProduct> entityClass;

    ProductType(Class<? extends InsuranceProduct> entityClass) {
        this.entityClass = entityClass;
    }

    public Class<? extends InsuranceProduct> entityClass() {
        return entityClass;
    }
}

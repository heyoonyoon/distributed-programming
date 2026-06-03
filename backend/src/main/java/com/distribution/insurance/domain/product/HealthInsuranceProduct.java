package com.distribution.insurance.domain.product;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("HEALTH")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class HealthInsuranceProduct extends InsuranceProduct {

    private int maxHospitalizationDays;

    public HealthInsuranceProduct(String productName, String description, int basePremium,
                                  int maxHospitalizationDays) {
        super(productName, description, basePremium);
        this.maxHospitalizationDays = maxHospitalizationDays;
    }
}

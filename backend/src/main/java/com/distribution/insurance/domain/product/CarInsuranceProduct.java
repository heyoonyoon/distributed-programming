package com.distribution.insurance.domain.product;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("CAR")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CarInsuranceProduct extends InsuranceProduct {

    private String vehicleType;
    private String driverScopeType;

    public CarInsuranceProduct(String productName, String description, int basePremium,
                               String vehicleType, String driverScopeType) {
        super(productName, description, basePremium);
        this.vehicleType = vehicleType;
        this.driverScopeType = driverScopeType;
    }
}

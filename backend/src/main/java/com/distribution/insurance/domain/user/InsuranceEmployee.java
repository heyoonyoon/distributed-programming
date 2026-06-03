package com.distribution.insurance.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("EMPLOYEE")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class InsuranceEmployee extends User {

    private String department;
    private int currentLoad;

    public InsuranceEmployee(String name, String email, String phone, String password,
                             String department, int currentLoad) {
        super(name, email, phone, password);
        this.department = department;
        this.currentLoad = currentLoad;
    }
}

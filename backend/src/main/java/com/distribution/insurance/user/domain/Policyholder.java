package com.distribution.insurance.user.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@DiscriminatorValue("POLICYHOLDER")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Policyholder extends User {

    private String ssn;
    private LocalDate birthDate;
    private String address;
    private String bankAccount;

    public Policyholder(String name, String email, String phone, String password,
                        String ssn, LocalDate birthDate, String address, String bankAccount) {
        super(name, email, phone, password);
        this.ssn = ssn;
        this.birthDate = birthDate;
        this.address = address;
        this.bankAccount = bankAccount;
    }

    /** 가입자 고유정보(주소·계좌) 수정 — 신설. */
    public void updateProfile(String address, String bankAccount) {
        this.address = address;
        this.bankAccount = bankAccount;
    }
}

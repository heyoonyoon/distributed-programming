package com.distribution.insurance.user.dto;

import com.distribution.insurance.user.domain.Policyholder;

public record ProfileResponse(
        String name, String email, String phone, String address, String bankAccount) {

    public static ProfileResponse from(Policyholder p) {
        return new ProfileResponse(p.getName(), p.getEmail(), p.getPhone(),
                p.getAddress(), p.getBankAccount());
    }
}

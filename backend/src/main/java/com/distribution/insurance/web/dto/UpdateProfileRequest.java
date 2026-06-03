package com.distribution.insurance.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank String email,
        @NotBlank String phone,
        @NotBlank String address,
        @NotBlank String bankAccount) {
}

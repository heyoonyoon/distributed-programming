package com.distribution.insurance.contract.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** UC10 A1: 출금계좌 + 출금일. */
public record AutoDebitRequest(
        @NotBlank String account,
        @Min(1) @Max(31) int withdrawalDay) {}

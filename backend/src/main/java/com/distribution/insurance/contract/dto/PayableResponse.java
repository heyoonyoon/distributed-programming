package com.distribution.insurance.contract.dto;

import com.distribution.insurance.contract.service.BillingService.ContractBilling;

import java.time.LocalDate;

/** UC10 2단계: 계약명, 납부기한, 납부금액. */
public record PayableResponse(
        Long contractId, String productName, LocalDate dueDate, int amount) {

    public static PayableResponse from(ContractBilling cb) {
        return new PayableResponse(
                cb.contract().getId(),
                cb.contract().getProduct().getProductName(),
                cb.status().oldestUnpaidDueDate(),
                cb.status().unpaidPrincipal());
    }
}

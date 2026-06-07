package com.distribution.insurance.contract.dto;

import com.distribution.insurance.contract.service.BillingService.ContractBilling;

import java.time.LocalDate;

/** UC07 2단계: 계약명, 납부기한, 미납금액, 연체일수, 연체이자. */
public record UnpaidResponse(
        Long contractId, String productName, LocalDate dueDate,
        int unpaidPrincipal, long overdueDays, long overdueInterest) {

    public static UnpaidResponse from(ContractBilling cb) {
        return new UnpaidResponse(
                cb.contract().getId(),
                cb.contract().getProduct().getProductName(),
                cb.status().oldestUnpaidDueDate(),
                cb.status().unpaidPrincipal(),
                cb.status().overdueDays(),
                cb.status().overdueInterest());
    }
}

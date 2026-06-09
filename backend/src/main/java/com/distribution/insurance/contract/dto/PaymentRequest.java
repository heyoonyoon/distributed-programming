package com.distribution.insurance.contract.dto;

import com.distribution.insurance.contract.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;

/** UC10 3~4단계: 결제수단 + 결제정보(카드/계좌 번호). */
public record PaymentRequest(@NotNull PaymentMethod method, String paymentInfo) {}

package com.distribution.insurance.contract.domain;

import com.distribution.insurance.common.service.InvalidRequestException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 보험료 납부 1건(UC10). 어느 회차인지는 저장하지 않는다(ADR 0004, FIFO 충당). */
@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int amount;
    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private InsuranceContract contract;

    private Payment(InsuranceContract contract, int amount, PaymentMethod method, PaymentStatus status) {
        this.contract = contract;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.paidAt = LocalDateTime.now();
    }

    public static Payment success(InsuranceContract contract, int amount, PaymentMethod method) {
        return new Payment(contract, amount, method, PaymentStatus.SUCCESS);
    }

    public static Payment failed(InsuranceContract contract, int amount, PaymentMethod method) {
        return new Payment(contract, amount, method, PaymentStatus.FAILED);
    }

    /** 납부 완료 영수증(UC10 6단계). 실패 납부는 영수증이 없다. */
    public Receipt getReceipt() {
        if (status != PaymentStatus.SUCCESS) {
            throw new InvalidRequestException("실패한 납부는 영수증을 발급할 수 없습니다.");
        }
        return new Receipt(id, amount, paidAt, method.name());
    }

    public record Receipt(Long paymentId, int amount, LocalDateTime paidAt, String method) {}
}

package com.distribution.insurance.contract.domain;

import com.distribution.insurance.product.domain.InsuranceProduct;
import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.common.service.IllegalStateTransitionException;
import com.distribution.insurance.common.service.InvalidRequestException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** 보험 계약(UC08). 심사 승인 시 생성된다(ADR 0005). */
@Entity
@Table(name = "insurance_contract")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class InsuranceContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ContractStatus status;

    private int monthlyPremium;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policyholder_id")
    private Policyholder policyholder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private InsuranceProduct product;

    @Embedded
    private AutoDebit autoDebit;   // 자동이체 등록 전 null

    /** 계약 기간은 시작일 + 1년(ADR 0005). monthlyPremium은 adjustedPremium(ADR 0003). */
    public InsuranceContract(Policyholder policyholder, InsuranceProduct product,
                             int monthlyPremium, LocalDate startDate) {
        this.policyholder = policyholder;
        this.product = product;
        this.monthlyPremium = monthlyPremium;
        this.startDate = startDate;
        this.endDate = startDate.plusYears(1);
        this.status = ContractStatus.ACTIVE;
    }

    public void suspend() {
        if (this.status != ContractStatus.ACTIVE) {
            throw new IllegalStateTransitionException("정상 계약만 정지할 수 있습니다.");
        }
        this.status = ContractStatus.SUSPENDED;
    }

    public void terminate() {
        if (this.status == ContractStatus.TERMINATED) {
            throw new IllegalStateTransitionException("이미 해지된 계약입니다.");
        }
        this.status = ContractStatus.TERMINATED;
    }

    /** 자동이체 등록(UC10 A1). 출금일은 1~31. */
    public void registerAutoDebit(String account, int withdrawalDay) {
        if (withdrawalDay < 1 || withdrawalDay > 31) {
            throw new InvalidRequestException("출금일은 1일에서 31일 사이여야 합니다.");
        }
        this.autoDebit = new AutoDebit(account, withdrawalDay);
    }

    /** 계약 상세에 노출할 결제수단 표기. 자동이체 등록 시 "AUTO_DEBIT", 아니면 "미등록". */
    public String registeredPaymentMethod() {
        return autoDebit != null ? PaymentMethod.AUTO_DEBIT.name() : "미등록";
    }

    /** 텍스트 기반 계약서. UC08 6단계 '계약서 다운로드'. */
    public byte[] generatePdf() {
        String body = "보험 계약서\n"
                + "계약번호: " + id + "\n"
                + "상품명: " + product.getProductName() + "\n"
                + "계약기간: " + startDate + " ~ " + endDate + "\n"
                + "월 보험료: " + monthlyPremium + "원\n"
                + "상태: " + status + "\n";
        return body.getBytes(StandardCharsets.UTF_8);
    }
}

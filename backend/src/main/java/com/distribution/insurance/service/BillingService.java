package com.distribution.insurance.service;

import com.distribution.insurance.domain.contract.*;
import com.distribution.insurance.repository.ContractRepository;
import com.distribution.insurance.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BillingService {

    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationSender notificationSender;

    public BillingService(ContractRepository contractRepository,
                          PaymentRepository paymentRepository,
                          PaymentGateway paymentGateway,
                          NotificationSender notificationSender) {
        this.contractRepository = contractRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.notificationSender = notificationSender;
    }

    /** 계약 + 그 미납 계산결과 묶음. */
    public record ContractBilling(InsuranceContract contract, BillingStatus status) {}

    /** 납부 결과 + 실패 사유(성공 시 null). */
    public record PayOutcome(Payment payment, String failureReason) {}

    /** 미납(연체) 목록(UC07): 연체일수 > 0 인 계약만. */
    @Transactional(readOnly = true)
    public List<ContractBilling> myOverdue(Long policyholderId) {
        List<ContractBilling> result = new ArrayList<>();
        for (InsuranceContract c : contractRepository.findByPolicyholderId(policyholderId)) {
            BillingStatus s = statusOf(c);
            if (s.isOverdue()) result.add(new ContractBilling(c, s));
        }
        return result;
    }

    /** 납부 예정 목록(UC10 2단계): 미납 회차가 1개 이상인 계약. */
    @Transactional(readOnly = true)
    public List<ContractBilling> myPayable(Long policyholderId) {
        List<ContractBilling> result = new ArrayList<>();
        for (InsuranceContract c : contractRepository.findByPolicyholderId(policyholderId)) {
            BillingStatus s = statusOf(c);
            if (s.hasUnpaid()) result.add(new ContractBilling(c, s));
        }
        return result;
    }

    /** 단건 미납 상세(UC07 3단계). 본인 계약만. */
    @Transactional(readOnly = true)
    public ContractBilling unpaidDetail(Long policyholderId, Long contractId) {
        InsuranceContract c = requireOwned(policyholderId, contractId);
        return new ContractBilling(c, statusOf(c));
    }

    /**
     * 한 회차(monthlyPremium) 납부(UC10). 성공/실패 모두 Payment로 기록한다.
     * 게이트웨이 실패는 클라이언트 오류가 아니므로 예외 없이 FAILED 결과를 반환한다(E1).
     */
    @Transactional
    public PayOutcome pay(Long policyholderId, Long contractId, PaymentMethod method, String paymentInfo) {
        InsuranceContract c = requireOwned(policyholderId, contractId);
        if (!statusOf(c).hasUnpaid()) {
            throw new InvalidRequestException("납부할 미납 보험료가 없습니다.");
        }
        int amount = c.getMonthlyPremium();

        PaymentGateway.Result result = paymentGateway.charge(method, amount, paymentInfo);
        Payment payment = result.success()
                ? Payment.success(c, amount, method)
                : Payment.failed(c, amount, method);
        paymentRepository.save(payment);

        if (result.success()) {
            notificationSender.send(c.getPolicyholder().getEmail(), c.getPolicyholder().getPhone(),
                    "보험료 " + amount + "원 납부가 완료되었습니다. (영수증번호 " + payment.getId() + ")");
        }
        return new PayOutcome(payment, result.success() ? null : result.reason());
    }

    /** 자동이체 등록(UC10 A1). 본인 계약만. */
    @Transactional
    public void registerAutoDebit(Long policyholderId, Long contractId, String account, int withdrawalDay) {
        InsuranceContract c = requireOwned(policyholderId, contractId);
        c.registerAutoDebit(account, withdrawalDay);
        notificationSender.send(c.getPolicyholder().getEmail(), c.getPolicyholder().getPhone(),
                "자동이체가 등록되었습니다. 출금계좌 " + account + ", 매월 " + withdrawalDay + "일.");
    }

    private BillingStatus statusOf(InsuranceContract c) {
        long success = paymentRepository.countByContractIdAndStatus(c.getId(), PaymentStatus.SUCCESS);
        return BillingCalculator.compute(c, success, LocalDate.now());
    }

    /** 없으면 404, 타인 계약이면 403(이슈 A ContractService와 동일 규약). */
    private InsuranceContract requireOwned(Long policyholderId, Long contractId) {
        InsuranceContract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));
        if (!c.getPolicyholder().getId().equals(policyholderId)) {
            throw new IllegalStateException("본인의 계약만 조회할 수 있습니다.");
        }
        return c;
    }
}

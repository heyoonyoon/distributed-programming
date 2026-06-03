package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.BenefitPayment;
import com.distribution.insurance.domain.claim.Claim;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.BenefitPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 보험금 지급(UC17). 송금 실패는 예외가 아니라 FAILED 상태로 기록(E1). */
@Service
public class BenefitPayoutService {

    private final BenefitTransferGateway transferGateway;
    private final BenefitPaymentRepository benefitPaymentRepository;
    private final NotificationSender notificationSender;

    public BenefitPayoutService(BenefitTransferGateway transferGateway,
                                BenefitPaymentRepository benefitPaymentRepository,
                                NotificationSender notificationSender) {
        this.transferGateway = transferGateway;
        this.benefitPaymentRepository = benefitPaymentRepository;
        this.notificationSender = notificationSender;
    }

    @Transactional
    public BenefitPayment pay(Claim claim) {
        Policyholder ph = claim.getContract().getPolicyholder();
        String account = ph.getBankAccount();
        int amount = claim.getRequestAmount();

        BenefitTransferGateway.Result result = transferGateway.transfer(account, amount);

        if (result.success()) {
            claim.markCompleted();
            BenefitPayment payment = benefitPaymentRepository.save(
                    BenefitPayment.success(claim, amount, account));
            notificationSender.send(ph.getEmail(), ph.getPhone(),
                    "보험금 " + amount + "원이 지급되었습니다.");
            return payment;
        } else {
            claim.markFailed();
            BenefitPayment payment = benefitPaymentRepository.save(
                    BenefitPayment.failed(claim, amount, account));
            // 가입자가 아닌 직원에게 실패 알림(UC17 E1-2). 직원 배정은 이슈 B이므로 로그성 알림만 남긴다.
            notificationSender.send(null, null,
                    "[직원알림] 청구 " + claim.getId() + " 보험금 송금 실패: " + result.reason());
            return payment;
        }
    }
}

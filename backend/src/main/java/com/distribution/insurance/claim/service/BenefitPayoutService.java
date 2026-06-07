package com.distribution.insurance.claim.service;

import com.distribution.insurance.claim.domain.BenefitPayment;
import com.distribution.insurance.claim.domain.Claim;
import com.distribution.insurance.claim.domain.ClaimStatus;
import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.claim.repository.BenefitPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.distribution.insurance.common.service.BenefitTransferGateway;
import com.distribution.insurance.common.service.NotificationSender;
import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.contract.domain.BillingStatus;
import com.distribution.insurance.common.service.PaymentGateway;
import com.distribution.insurance.contract.domain.PaymentMethod;
import com.distribution.insurance.contract.domain.Payment;
import com.distribution.insurance.contract.domain.Notice;
import com.distribution.insurance.common.service.IdentityVerificationService;
import com.distribution.insurance.application.domain.ApplicationStatus;
import com.distribution.insurance.application.domain.VehicleInfo;
import com.distribution.insurance.application.domain.MedicalHistory;
import com.distribution.insurance.common.service.AccidentHistoryClient;
import com.distribution.insurance.common.service.StaffAssignmentService;
import com.distribution.insurance.common.service.IllegalStateTransitionException;

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
        ClaimStatus status = claim.getStatus();
        if (status != ClaimStatus.PENDING && status != ClaimStatus.APPROVED && status != ClaimStatus.FAILED) {
            throw new IllegalStateTransitionException(
                    "지급 가능한 상태가 아닙니다. 현재 상태: " + status);
        }
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

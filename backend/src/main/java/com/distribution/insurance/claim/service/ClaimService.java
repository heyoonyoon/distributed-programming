package com.distribution.insurance.claim.service;

import com.distribution.insurance.claim.domain.ClaimAttachment;
import com.distribution.insurance.claim.domain.ClaimComplexity;
import com.distribution.insurance.claim.domain.HealthInsuranceClaim;
import com.distribution.insurance.contract.domain.ContractStatus;
import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.product.domain.HealthInsuranceProduct;
import com.distribution.insurance.review.domain.BenefitPaymentReview;
import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.review.repository.BenefitPaymentReviewRepository;
import com.distribution.insurance.claim.repository.ClaimRepository;
import com.distribution.insurance.contract.repository.ContractRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import com.distribution.insurance.common.service.NotificationSender;
import com.distribution.insurance.common.service.StaffAssignmentService;
import com.distribution.insurance.common.service.InvalidRequestException;

/** 의료보험 청구(UC05). 복잡도 판별 후 SIMPLE은 즉시지급, COMPLEX는 심사 대기. */
@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ContractRepository contractRepository;
    private final BenefitPayoutService payoutService;
    private final NotificationSender notificationSender;
    private final int complexThreshold;
    private final BenefitPaymentReviewRepository reviewRepository;
    private final StaffAssignmentService staffAssignmentService;

    public ClaimService(ClaimRepository claimRepository,
                        ContractRepository contractRepository,
                        BenefitPayoutService payoutService,
                        NotificationSender notificationSender,
                        @Value("${insurance.claim.complex-threshold:1000000}") int complexThreshold,
                        BenefitPaymentReviewRepository reviewRepository,
                        StaffAssignmentService staffAssignmentService) {
        this.claimRepository = claimRepository;
        this.contractRepository = contractRepository;
        this.payoutService = payoutService;
        this.notificationSender = notificationSender;
        this.complexThreshold = complexThreshold;
        this.reviewRepository = reviewRepository;
        this.staffAssignmentService = staffAssignmentService;
    }

    @Transactional
    public HealthInsuranceClaim fileHealthClaim(Long policyholderId, Long contractId,
                                                String hospitalName, String diagnosisCode,
                                                LocalDate treatmentDate, int requestAmount,
                                                int receiptAmount, List<ClaimAttachment> attachments) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));
        if (!contract.getPolicyholder().getId().equals(policyholderId)) {
            throw new IllegalStateException("본인 계약에만 청구할 수 있습니다.");
        }
        if (!(Hibernate.unproxy(contract.getProduct()) instanceof HealthInsuranceProduct)) {
            throw new InvalidRequestException("의료보험 계약이 아닙니다.");
        }
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new InvalidRequestException("유효한 계약이 아닙니다.");
        }
        if (requestAmount <= 0 || receiptAmount <= 0) {
            throw new InvalidRequestException("청구 금액은 0보다 커야 합니다.");
        }

        ClaimComplexity complexity = requestAmount >= complexThreshold
                ? ClaimComplexity.COMPLEX : ClaimComplexity.SIMPLE;

        HealthInsuranceClaim claim = new HealthInsuranceClaim(
                contract, requestAmount, hospitalName, diagnosisCode, treatmentDate, receiptAmount, complexity);
        attachments.forEach(claim::addAttachment);
        claimRepository.save(claim);

        Policyholder ph = contract.getPolicyholder();
        if (complexity == ClaimComplexity.SIMPLE) {
            payoutService.pay(claim);
        } else {
            BenefitPaymentReview review = reviewRepository.save(new BenefitPaymentReview(claim));
            staffAssignmentService.assignAutomatically(review);   // 직원 없으면 IllegalStateException → 롤백
            claim.markInReview();
            notificationSender.send(ph.getEmail(), ph.getPhone(),
                    "청구가 접수되었습니다. 담당자 배정 후 심사를 진행합니다.");
        }
        return claim;
    }
}

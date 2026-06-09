package com.distribution.insurance.claim.service;

import com.distribution.insurance.claim.domain.CarAccidentReport;
import com.distribution.insurance.claim.domain.Claim;
import com.distribution.insurance.claim.domain.ClaimStatus;
import com.distribution.insurance.claim.domain.HealthInsuranceClaim;
import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.contract.domain.PaymentStatus;
import com.distribution.insurance.claim.repository.BenefitPaymentRepository;
import com.distribution.insurance.claim.repository.ClaimRepository;
import com.distribution.insurance.contract.repository.ContractRepository;
import com.distribution.insurance.contract.repository.PaymentRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import com.distribution.insurance.common.service.InvalidRequestException;

/** 보상 현황(UC03)·이력(UC04)·실익분석(UC11) 조회. 읽기 전용. */
@Service
public class ClaimQueryService {

    private static final Set<ClaimStatus> IN_PROGRESS =
            EnumSet.of(ClaimStatus.PENDING, ClaimStatus.IN_REVIEW, ClaimStatus.APPROVED, ClaimStatus.FAILED);
    /** 이력(UC04)은 처리 결과가 확정된 종결 건만 노출한다. */
    private static final Set<ClaimStatus> TERMINAL =
            EnumSet.of(ClaimStatus.COMPLETED, ClaimStatus.REJECTED);

    private final ClaimRepository claimRepository;
    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final BenefitPaymentRepository benefitPaymentRepository;

    public ClaimQueryService(ClaimRepository claimRepository, ContractRepository contractRepository,
                             PaymentRepository paymentRepository, BenefitPaymentRepository benefitPaymentRepository) {
        this.claimRepository = claimRepository;
        this.contractRepository = contractRepository;
        this.paymentRepository = paymentRepository;
        this.benefitPaymentRepository = benefitPaymentRepository;
    }

    public record ClaimSummary(Long claimId, String claimType, LocalDate claimDate, int requestAmount,
                               long paidAmount, String status) {}
    public record BenefitAnalysis(long totalPaidPremium, long totalReceivedBenefit,
                                  long profit, double profitRate) {}

    @Transactional(readOnly = true)
    public List<ClaimSummary> inProgressClaims(Long policyholderId) {
        return claimRepository.findByContractPolicyholderId(policyholderId).stream()
                .filter(c -> IN_PROGRESS.contains(c.getStatus()))
                .map(c -> toSummary(c, 0L))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClaimSummary> history(Long policyholderId, LocalDate from, LocalDate to) {
        LocalDate end = (to == null) ? LocalDate.now() : to;
        LocalDate start = (from == null) ? end.minusYears(1) : from;
        if (start.isAfter(end)) {
            throw new InvalidRequestException("조회 시작일이 종료일보다 늦을 수 없습니다.");
        }
        return claimRepository.findByContractPolicyholderId(policyholderId).stream()
                .filter(c -> TERMINAL.contains(c.getStatus()))
                .filter(c -> !c.getClaimDate().isBefore(start) && !c.getClaimDate().isAfter(end))
                .map(c -> toSummary(c, benefitPaymentRepository.sumPaidByClaimIdAndStatus(c.getId(), PaymentStatus.SUCCESS)))
                .toList();
    }

    @Transactional(readOnly = true)
    public BenefitAnalysis benefitAnalysis(Long policyholderId, Long contractId) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));
        if (!contract.getPolicyholder().getId().equals(policyholderId)) {
            throw new IllegalStateException("본인 계약만 분석할 수 있습니다.");
        }
        if (contract.getStartDate().isAfter(LocalDate.now().minusMonths(6))) {
            throw new InvalidRequestException("분석에 필요한 데이터가 충분하지 않습니다. 6개월 이후 이용 가능합니다.");
        }
        long paid = paymentRepository.sumAmountByContractIdAndStatus(contractId, PaymentStatus.SUCCESS);
        long received = benefitPaymentRepository.sumPaidByContractIdAndStatus(contractId, PaymentStatus.SUCCESS);
        double rate = paid > 0 ? (double) received / paid : 0.0;
        return new BenefitAnalysis(paid, received, received - paid, rate);
    }

    private ClaimSummary toSummary(Claim claim, long paidAmount) {
        String type = Hibernate.unproxy(claim) instanceof CarAccidentReport ? "CAR" : "HEALTH";
        return new ClaimSummary(claim.getId(), type, claim.getClaimDate(),
                claim.getRequestAmount(), paidAmount, claim.getStatus().name());
    }
}

package com.distribution.insurance.claim.service;

import com.distribution.insurance.claim.domain.CarAccidentReport;
import com.distribution.insurance.claim.domain.ClaimAttachment;
import com.distribution.insurance.contract.domain.ContractStatus;
import com.distribution.insurance.contract.domain.InsuranceContract;
import com.distribution.insurance.product.domain.CarInsuranceProduct;
import com.distribution.insurance.review.domain.BenefitPaymentReview;
import com.distribution.insurance.user.domain.InsuranceEmployee;
import com.distribution.insurance.user.domain.Policyholder;
import com.distribution.insurance.review.repository.BenefitPaymentReviewRepository;
import com.distribution.insurance.claim.repository.CarAccidentReportRepository;
import com.distribution.insurance.contract.repository.ContractRepository;
import com.distribution.insurance.user.repository.UserRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import com.distribution.insurance.common.service.NotificationSender;
import com.distribution.insurance.common.service.InvalidRequestException;
import com.distribution.insurance.contract.domain.BillingStatus;
import com.distribution.insurance.common.service.IllegalStateTransitionException;

/** 자동차사고 접수(UC09). 접수번호 발급 + 직원·가입자 알림 + 미배정 보상심사 review 생성(ADR 0009). 담당자는 수동 배정(UC14 A1). */
@Service
public class CarAccidentService {

    private final CarAccidentReportRepository reportRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final NotificationSender notificationSender;
    private final BenefitPaymentReviewRepository reviewRepository;

    public CarAccidentService(CarAccidentReportRepository reportRepository,
                              ContractRepository contractRepository,
                              UserRepository userRepository,
                              NotificationSender notificationSender,
                              BenefitPaymentReviewRepository reviewRepository) {
        this.reportRepository = reportRepository;
        this.contractRepository = contractRepository;
        this.userRepository = userRepository;
        this.notificationSender = notificationSender;
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public CarAccidentReport report(Long policyholderId, Long contractId, LocalDate accidentDate,
                                    String accidentLocation, String accidentType, String vehicleNumber,
                                    boolean hasInjury, int injuredCount, List<ClaimAttachment> attachments) {
        InsuranceContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));
        if (!contract.getPolicyholder().getId().equals(policyholderId)) {
            throw new IllegalStateException("본인 계약에만 사고를 접수할 수 있습니다.");
        }
        if (!(Hibernate.unproxy(contract.getProduct()) instanceof CarInsuranceProduct)) {
            throw new InvalidRequestException("자동차보험 계약이 아닙니다.");
        }
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new InvalidRequestException("유효한 계약이 아닙니다.");
        }
        validateAccidentPayload(accidentDate, accidentLocation, accidentType, vehicleNumber, hasInjury, injuredCount);

        CarAccidentReport report = new CarAccidentReport(
                contract, accidentDate, accidentLocation, accidentType, vehicleNumber, hasInjury, injuredCount);
        attachments.forEach(report::addAttachment);
        reportRepository.save(report);

        // 자동차사고는 보상심사 review만 미배정·접수(PENDING) 상태로 생성(ADR 0009).
        // 담당자 수동 배정(UC14 A1) 시점에 심사중(IN_REVIEW)으로 전이된다.
        reviewRepository.save(new BenefitPaymentReview(report));

        // 직원 전원에게 접수 알림(UC09 step6)
        for (InsuranceEmployee staff : userRepository.findAllEmployees()) {
            notificationSender.send(staff.getEmail(), staff.getPhone(),
                    "자동차사고 접수 알림. 접수번호 " + report.getId());
        }
        // 가입자 접수완료 안내(UC09 step7)
        Policyholder ph = contract.getPolicyholder();
        notificationSender.send(ph.getEmail(), ph.getPhone(),
                "사고 접수가 완료되었습니다. 접수번호 " + report.getId() + ". 담당자가 곧 연락드립니다.");
        return report;
    }

    /** 사고 정보 유효성(UC09 3단계 입력 검증). 위반 시 400. */
    private void validateAccidentPayload(LocalDate accidentDate, String accidentLocation, String accidentType,
                                         String vehicleNumber, boolean hasInjury, int injuredCount) {
        if (accidentDate == null || accidentDate.isAfter(LocalDate.now())) {
            throw new InvalidRequestException("사고 일자는 미래일 수 없습니다.");
        }
        if (isBlank(accidentLocation) || isBlank(accidentType) || isBlank(vehicleNumber)) {
            throw new InvalidRequestException("사고 장소·유형·차량번호는 필수입니다.");
        }
        if (injuredCount < 0) {
            throw new InvalidRequestException("부상자 수는 음수일 수 없습니다.");
        }
        // 대인사고 일관성(UC09 A1): 부상 있으면 1명 이상, 없으면 0명.
        if (hasInjury && injuredCount < 1) {
            throw new InvalidRequestException("대인사고는 부상자 수가 1명 이상이어야 합니다.");
        }
        if (!hasInjury && injuredCount != 0) {
            throw new InvalidRequestException("부상이 없으면 부상자 수는 0이어야 합니다.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

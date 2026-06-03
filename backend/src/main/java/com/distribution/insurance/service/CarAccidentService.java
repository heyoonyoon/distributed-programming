package com.distribution.insurance.service;

import com.distribution.insurance.domain.claim.CarAccidentReport;
import com.distribution.insurance.domain.claim.ClaimAttachment;
import com.distribution.insurance.domain.contract.ContractStatus;
import com.distribution.insurance.domain.contract.InsuranceContract;
import com.distribution.insurance.domain.product.CarInsuranceProduct;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.domain.user.Policyholder;
import com.distribution.insurance.repository.CarAccidentReportRepository;
import com.distribution.insurance.repository.ContractRepository;
import com.distribution.insurance.repository.UserRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** 자동차사고 접수(UC09). 접수번호 발급 + 직원·가입자 알림. 심사/지급 없음. */
@Service
public class CarAccidentService {

    private final CarAccidentReportRepository reportRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final NotificationSender notificationSender;

    public CarAccidentService(CarAccidentReportRepository reportRepository,
                              ContractRepository contractRepository,
                              UserRepository userRepository,
                              NotificationSender notificationSender) {
        this.reportRepository = reportRepository;
        this.contractRepository = contractRepository;
        this.userRepository = userRepository;
        this.notificationSender = notificationSender;
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

        CarAccidentReport report = new CarAccidentReport(
                contract, accidentDate, accidentLocation, accidentType, vehicleNumber, hasInjury, injuredCount);
        attachments.forEach(report::addAttachment);
        reportRepository.save(report);

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
}

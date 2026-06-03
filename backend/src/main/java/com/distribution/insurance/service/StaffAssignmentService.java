package com.distribution.insurance.service;

import com.distribution.insurance.domain.review.BenefitPaymentReview;
import com.distribution.insurance.domain.user.InsuranceEmployee;
import com.distribution.insurance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

/** 담당자 지정(UC14). 최소 부하 자동 배정 + 수동 배정. */
@Service
public class StaffAssignmentService {

    private final UserRepository userRepository;
    private final NotificationSender notificationSender;

    public StaffAssignmentService(UserRepository userRepository, NotificationSender notificationSender) {
        this.userRepository = userRepository;
        this.notificationSender = notificationSender;
    }

    /** 현재 업무량 최소(동점 시 id 오름차순) 직원에게 자동 배정. */
    @Transactional
    public void assignAutomatically(BenefitPaymentReview review) {
        InsuranceEmployee staff = userRepository.findAllEmployees().stream()
                .min(Comparator.comparingInt(InsuranceEmployee::getCurrentLoad)
                        .thenComparing(InsuranceEmployee::getId))
                .orElseThrow(() -> new IllegalStateException("배정 가능한 담당자가 없습니다."));
        bind(review, staff);
    }

    /** 관리자가 지정한 직원으로 수동 배정(UC14 A1).
     *  이미 배정된 경우 이전 담당자 부하를 회수 후 신규 담당자에게 배정.
     *  확정된 심사는 재배정 불가. */
    @Transactional
    public void assignManually(BenefitPaymentReview review, Long employeeId) {
        if (review.getResult() != null) {
            throw new IllegalStateTransitionException("이미 확정된 심사는 재배정할 수 없습니다.");
        }
        InsuranceEmployee staff = userRepository.findById(employeeId)
                .filter(u -> u instanceof InsuranceEmployee)
                .map(u -> (InsuranceEmployee) u)
                .orElseThrow(() -> new IllegalArgumentException("심사 직원을 찾을 수 없습니다."));

        // 이미 배정된 경우: 이전 담당자 부하 회수
        if (review.getAssignedStaffId() != null) {
            userRepository.findById(review.getAssignedStaffId())
                    .filter(u -> u instanceof InsuranceEmployee)
                    .map(u -> (InsuranceEmployee) u)
                    .ifPresent(InsuranceEmployee::releaseWork);
        }

        bind(review, staff);

        // 최초 배정 시 접수(PENDING) → 심사중(IN_REVIEW)으로 전이. 재배정(이미 IN_REVIEW)은 그대로.
        var claim = review.getClaim();
        if (claim.getStatus() == com.distribution.insurance.domain.claim.ClaimStatus.PENDING) {
            claim.markInReview();
        }
    }

    private void bind(BenefitPaymentReview review, InsuranceEmployee staff) {
        staff.assignWork();
        review.assignTo(staff.getId());
        notificationSender.send(staff.getEmail(), staff.getPhone(),
                "새 심사 건이 배정되었습니다. 청구 " + review.getClaim().getId());
    }
}
